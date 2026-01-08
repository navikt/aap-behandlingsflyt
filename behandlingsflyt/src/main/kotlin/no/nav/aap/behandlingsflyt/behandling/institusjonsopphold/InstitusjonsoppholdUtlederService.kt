package no.nav.aap.behandlingsflyt.behandling.institusjonsopphold

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.barnetillegg.BarnetilleggPeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.barnetillegg.BarnetilleggRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.InstitusjonsoppholdRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.Institusjonstype
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon.HelseinstitusjonVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon.Soningsvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon.flate.OppholdVurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.komponenter.tidslinje.JoinStyle
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.StandardSammenslåere
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Tid
import no.nav.aap.lookup.repository.RepositoryProvider
import java.time.LocalDate

class InstitusjonsoppholdUtlederService(
    private val barnetilleggRepository: BarnetilleggRepository,
    private val institusjonsoppholdRepository: InstitusjonsoppholdRepository,
    private val sakRepository: SakRepository,
    private val behandlingRepository: BehandlingRepository
) {
    constructor(repositoryProvider: RepositoryProvider) : this(
        barnetilleggRepository = repositoryProvider.provide(),
        institusjonsoppholdRepository = repositoryProvider.provide(),
        sakRepository = repositoryProvider.provide(),
        behandlingRepository = repositoryProvider.provide(),
    )

    fun utled(
        behandlingId: BehandlingId,
        basertPåVurderingerFørDenneBehandlingen: Boolean = false,
        begrensetTilRettighetsperiode: Boolean? = true
    ): BehovForAvklaringer {
        val input = konstruerInput(behandlingId, basertPåVurderingerFørDenneBehandlingen)

        return utledBehov(input, begrensetTilRettighetsperiode)
    }

    internal fun utledBehov(input: InstitusjonsoppholdInput, begrensetTilRettighetsperiode: Boolean? = true): BehovForAvklaringer {
        val opphold = input.institusjonsOpphold
        val soningsOppgold = opphold.filter { segment -> segment.verdi.type == Institusjonstype.FO }
        val helseopphold = opphold.filter { segment -> segment.verdi.type == Institusjonstype.HS }
        val soningsvurderingTidslinje = byggSoningsvurderingTidslinje(input.soningsvurderinger)
        val barnetillegg = input.barnetillegg
        val helsevurderingerTidslinje = byggHelsevurderingTidslinje(input.helsevurderinger, barnetillegg)
        var perioderSomTrengerVurdering =
            Tidslinje(soningsOppgold)
                .begrensetTil(input.rettighetsperiode)
                .mapValue { InstitusjonsoppholdVurdering(soning = SoningOpphold(vurdering = OppholdVurdering.UAVKLART)) }
                .kombiner(soningsvurderingTidslinje, JoinStyle.OUTER_JOIN { periode, venstreSegment, høyreSegment ->
                    val venstreVerdi = venstreSegment?.verdi
                    val høyreVerdi = høyreSegment?.verdi

                    val soning = utledSoning(venstreVerdi?.soning, høyreVerdi)
                    val helse = venstreVerdi?.helse

                    val verdi = InstitusjonsoppholdVurdering(helse = helse, soning = soning)
                    Segment(periode, verdi)
                })


        val helseOppholdTidslinje = opprettTidslinje(helseopphold)

       val oppholdSomKanGiReduksjon = harOppholdSomKreverAvklaring(helseOppholdTidslinje)

        perioderSomTrengerVurdering = perioderSomTrengerVurdering.kombiner(oppholdSomKanGiReduksjon.mapValue {
            InstitusjonsoppholdVurdering(helse = HelseOpphold(vurdering = OppholdVurdering.UAVKLART))
        }, sammenslåer()).kombiner(helsevurderingerTidslinje, helsevurderingSammenslåer()).komprimer()


        if (begrensetTilRettighetsperiode == true) {
            perioderSomTrengerVurdering = perioderSomTrengerVurdering.begrensetTil(input.rettighetsperiode)
        }
        return BehovForAvklaringer(perioderSomTrengerVurdering)
    }

    private fun helsevurderingSammenslåer(): JoinStyle.LEFT_JOIN<InstitusjonsoppholdVurdering, HelseOpphold, InstitusjonsoppholdVurdering> =
        JoinStyle.LEFT_JOIN { periode, venstreSegment, høyreSegment ->
            val venstreVerdi = venstreSegment.verdi
            val høyreVerdi = høyreSegment?.verdi

            val soning = venstreVerdi.soning
            val helse = utledHelse(venstreVerdi.helse, høyreVerdi)

            val verdi = InstitusjonsoppholdVurdering(helse = helse, soning = soning)
            Segment(periode, verdi)
        }

    private fun byggSoningsvurderingTidslinje(
        soningsvurderinger: List<Soningsvurdering>
    ): Tidslinje<SoningOpphold> {
        return soningsvurderinger.sortedBy { it.fraDato }.map {
            Tidslinje(
                Periode(
                    it.fraDato,
                    Tid.MAKS
                ), SoningOpphold(
                    if (it.skalOpphøre) {
                        OppholdVurdering.AVSLÅTT
                    } else {
                        OppholdVurdering.GODKJENT
                    }
                )
            )
        }.fold(Tidslinje<SoningOpphold>()) { acc, tidslinje ->
            acc.kombiner(tidslinje, StandardSammenslåere.prioriterHøyreSideCrossJoin())
        }.komprimer()
    }

    private fun byggHelsevurderingTidslinje(
        helsevurderinger: List<HelseinstitusjonVurdering>,
        barnetillegg: List<BarnetilleggPeriode>
    ): Tidslinje<HelseOpphold> {

        val barnTilleggPeriodeDekkerAlt = barnetilleggDekkerAlleInstitusjonsperioder(
            helsevurderinger,
            barnetillegg
        )

        return Tidslinje(
            helsevurderinger
                .sortedBy { it.periode }
                .map {
                    Segment(
                        it.periode,
                        HelseOpphold(
                            if (barnTilleggPeriodeDekkerAlt) {
                                OppholdVurdering.GODKJENT
                            } else if (
                                it.faarFriKostOgLosji &&
                                it.harFasteUtgifter == false &&
                                it.forsoergerEktefelle == false
                            ) {
                                OppholdVurdering.AVSLÅTT
                            } else {
                                OppholdVurdering.GODKJENT
                            }
                        )
                    )
                }
        ).komprimer()
    }



    private fun barnetilleggDekkerAlleInstitusjonsperioder(
        helsevurderinger: List<HelseinstitusjonVurdering>,
        barnetillegg: List<BarnetilleggPeriode>
    ): Boolean {
        val barnetilleggPerioder = barnetillegg.map { it.periode }

        return helsevurderinger.all { helse ->
            erFulltDekket(helse.periode, barnetilleggPerioder)
        }
    }

    private fun erFulltDekket(
        target: Periode,
        dekning: List<Periode>
    ): Boolean {
        val relevante = dekning
            .filter { it.overlapper(target) }
            .sortedBy { it.fom }

        if (relevante.isEmpty()) return false

        var currentEnd = relevante.first().tom
        if (relevante.first().fom > target.fom) return false

        for (periode in relevante.drop(1)) {

            if (periode.fom > currentEnd.plusDays(1)) return false
            currentEnd = maxOf(currentEnd, periode.tom)
            if (currentEnd >= target.tom) return true
        }

        return currentEnd >= target.tom
    }

    private fun sammenslåer(): JoinStyle.OUTER_JOIN<InstitusjonsoppholdVurdering, InstitusjonsoppholdVurdering, InstitusjonsoppholdVurdering> {
        return JoinStyle.OUTER_JOIN { periode, venstreSegment, høyreSegment ->
            val venstreVerdi = venstreSegment?.verdi
            val høyreVerdi = høyreSegment?.verdi

            val soning = utledSoning(venstreVerdi?.soning, høyreVerdi?.soning)
            val helse = utledHelse(venstreVerdi?.helse, høyreVerdi?.helse)

            val verdi = InstitusjonsoppholdVurdering(helse = helse, soning = soning)
            Segment(periode, verdi)
        }
    }

    private fun utledSoning(
        venstreopphold: SoningOpphold?,
        høyreopphold: SoningOpphold?
    ): SoningOpphold? {
        if (venstreopphold == null && høyreopphold == null) {
            return null
        }
        if (venstreopphold == null) {
            return høyreopphold
        }
        if (høyreopphold == null) {
            return venstreopphold
        }

        return SoningOpphold(høyreopphold.vurdering.prioritertVerdi(venstreopphold.vurdering))
    }

    private fun utledHelse(
        venstreopphold: HelseOpphold?,
        høyreopphold: HelseOpphold?
    ): HelseOpphold? {
        if (venstreopphold == null && høyreopphold == null) {
            return null
        }
        if (venstreopphold == null) {
            return høyreopphold
        }
        if (høyreopphold == null) {
            return venstreopphold
        }

        return HelseOpphold(
            vurdering = høyreopphold.vurdering.prioritertVerdi(venstreopphold.vurdering),
            umiddelbarReduksjon = høyreopphold.umiddelbarReduksjon || venstreopphold.umiddelbarReduksjon
        )
    }

    private fun oppholdSomLiggerMindreEnnTreMånederFraForrigeSomGaReduksjon(
        helseOpphold: Tidslinje<Boolean>,
        oppholdUtenBarnetillegg: Tidslinje<InstitusjonsoppholdVurdering>
    ): Tidslinje<Boolean> {
        val tidslinje = Tidslinje(
            helseOpphold.segmenter()
                .filter { segment -> segment.verdi }
                .filter { segment ->
                    segment.periode.tom < LocalDate.now() && oppholdUtenBarnetillegg.segmenter()
                        .filter { it.verdi.helse?.vurdering == OppholdVurdering.AVSLÅTT }
                        .any {
                            Periode(
                                it.periode.tom.plusDays(1),
                                it.periode.tom.plusMonths(3).minusDays(1)
                            ).inneholder(segment.periode.fom)
                        }
                })
        return tidslinje
    }

    private fun harOppholdSomKreverAvklaring(
        oppholdUtenBarnetillegg: Tidslinje<Boolean>
    ): Tidslinje<Boolean> {
        return Tidslinje(
            oppholdUtenBarnetillegg.segmenter()
                .filter { segment -> segment.verdi }
                )
    }

    private fun <T> opprettTidslinje(segmenter: List<Segment<T>>): Tidslinje<Boolean> {
        return segmenter.sortedBy { it.fom() }.map { segment ->
            Tidslinje(
                segment.periode,
                true
            )
        }.fold(Tidslinje()) { acc, tidslinje ->
            acc.kombiner(tidslinje, StandardSammenslåere.prioriterHøyreSideCrossJoin())
        }
    }

    private fun konstruerInput(
        behandlingId: BehandlingId,
        basertPåVurderingerFørDenneBehandlingen: Boolean
    ): InstitusjonsoppholdInput {
        val behandling = behandlingRepository.hent(behandlingId)
        val rettighetsperiode = sakRepository.hent(behandling.sakId).rettighetsperiode
        val grunnlag = institusjonsoppholdRepository.hentHvisEksisterer(behandlingId)
        val barnetillegg = barnetilleggRepository.hentHvisEksisterer(behandlingId)?.perioder.orEmpty()

        val opphold = grunnlag?.oppholdene?.opphold.orEmpty()
        val soningsvurderinger: List<Soningsvurdering>
        val helsevurderinger: List<HelseinstitusjonVurdering>
        if (basertPåVurderingerFørDenneBehandlingen) {
            val forrigeGrunnlag =
                behandling.forrigeBehandlingId?.let { institusjonsoppholdRepository.hentHvisEksisterer(behandling.forrigeBehandlingId) }
            soningsvurderinger = forrigeGrunnlag?.soningsVurderinger?.vurderinger.orEmpty()
            helsevurderinger = forrigeGrunnlag?.helseoppholdvurderinger?.vurderinger.orEmpty()
        } else {
            soningsvurderinger = grunnlag?.soningsVurderinger?.vurderinger.orEmpty()
            helsevurderinger = grunnlag?.helseoppholdvurderinger?.vurderinger.orEmpty()
        }

        return InstitusjonsoppholdInput(rettighetsperiode, opphold, soningsvurderinger, barnetillegg, helsevurderinger)
    }
}