package no.nav.aap.behandlingsflyt.behandling.etannetsted

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.barnetillegg.BarnetilleggRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.barnetillegg.tilTidslinje
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.InstitusjonsoppholdRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.Institusjonstype
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon.HelseinstitusjonVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon.Soningsvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon.flate.OppholdVurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.komponenter.miljo.Miljø
import no.nav.aap.komponenter.miljo.MiljøKode
import no.nav.aap.komponenter.tidslinje.JoinStyle
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.StandardSammenslåere
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Tid
import no.nav.aap.lookup.repository.RepositoryProvider
import java.time.LocalDate
import java.util.stream.IntStream
import kotlin.math.max

class EtAnnetStedUtlederService(
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
        basertPåVurderingerFørDenneBehandlingen: Boolean = false
    ): BehovForAvklaringer {
        val input = konstruerInput(behandlingId, basertPåVurderingerFørDenneBehandlingen)

        return utledBehov(input)
    }

    internal fun utledBehov(input: EtAnnetStedInput): BehovForAvklaringer {
        val opphold = input.institusjonsOpphold
        val soningsOppgold = opphold.filter { segment -> segment.verdi.type == Institusjonstype.FO }
        val helseopphold = opphold.filter { segment -> segment.verdi.type == Institusjonstype.HS }
        val barnetillegg = input.barnetillegg
        val soningsvurderingTidslinje = byggSoningsvurderingTidslinje(input.soningsvurderinger)
        val helsevurderingerTidslinje = byggHelsevurderingTidslinje(input.helsevurderinger)

        var perioderSomTrengerVurdering =
            Tidslinje(soningsOppgold)
                .begrensetTil(input.rettighetsperiode)
                .mapValue { InstitusjonsOpphold(soning = SoningOpphold(vurdering = OppholdVurdering.UAVKLART)) }
                .kombiner(soningsvurderingTidslinje, JoinStyle.OUTER_JOIN { periode, venstreSegment, høyreSegment ->
                    val venstreVerdi = venstreSegment?.verdi
                    val høyreVerdi = høyreSegment?.verdi

                    val soning = utledSoning(venstreVerdi?.soning, høyreVerdi)
                    val helse = venstreVerdi?.helse

                    val verdi = InstitusjonsOpphold(helse = helse, soning = soning)
                    Segment(periode, verdi)
                })

        val helseOpphold = opprettTidslinje(helseopphold).begrensetTil(input.rettighetsperiode)

        val barnetilleggTidslinje = barnetillegg.tilTidslinje()

        //fjern perioder hvor bruker har barnetillegg gjennom hele helseinstitusjonsoppholdet
        val oppholdUtenBarnetillegg =
            helseOpphold.disjoint(barnetilleggTidslinje) { p, v -> Segment(p, v.verdi) }

        // Oppholdet må være lengre enn 3 måneder for å være aktuelt for avklaring og må ha vart i minimum 2 måneder for å være klar for avklaring
        val oppholdSomKanGiReduksjon = harOppholdSomKreverAvklaring(oppholdUtenBarnetillegg)

        perioderSomTrengerVurdering = perioderSomTrengerVurdering.kombiner(oppholdSomKanGiReduksjon.mapValue {
            InstitusjonsOpphold(helse = HelseOpphold(vurdering = OppholdVurdering.UAVKLART))
        }, sammenslåer()).kombiner(helsevurderingerTidslinje, helsevurderingSammenslåer()).komprimer()

        // Hvis det er mindre en 3 måneder siden sist opphold og bruker er nå innlagt
        val helseoppholdUtenBarnetillegg = helseOpphold.disjoint(
            barnetilleggTidslinje
        ) { p, v ->
            Segment(
                p,
                v.verdi
            )
        }.komprimer()

        val oppholdSomLiggerMindreEnnTreMånederFraForrigeSomGaReduksjon =
            regnUtTidslinjeOverOppholdSomErMindreEnnTreMånederFraForrigeSomGaReduksjon(
                perioderSomTrengerVurdering,
                helseoppholdUtenBarnetillegg, helsevurderingerTidslinje
            )

        perioderSomTrengerVurdering = perioderSomTrengerVurdering.kombiner(
            oppholdSomLiggerMindreEnnTreMånederFraForrigeSomGaReduksjon,
            sammenslåer()
        ).komprimer()

        return BehovForAvklaringer(perioderSomTrengerVurdering)
    }

    private fun helsevurderingSammenslåer(): JoinStyle.LEFT_JOIN<InstitusjonsOpphold, HelseOpphold, InstitusjonsOpphold> =
        JoinStyle.LEFT_JOIN { periode, venstreSegment, høyreSegment ->
            val venstreVerdi = venstreSegment.verdi
            val høyreVerdi = høyreSegment?.verdi

            val soning = venstreVerdi.soning
            val helse = utledHelse(venstreVerdi.helse, høyreVerdi)

            val verdi = InstitusjonsOpphold(helse = helse, soning = soning)
            Segment(periode, verdi)
        }

    private fun regnUtTidslinjeOverOppholdSomErMindreEnnTreMånederFraForrigeSomGaReduksjon(
        perioderSomTrengerVurdering: Tidslinje<InstitusjonsOpphold>,
        helseoppholdUtenBarnetillegg: Tidslinje<Boolean>,
        helsevurderingerTidslinje: Tidslinje<HelseOpphold>
    ): Tidslinje<InstitusjonsOpphold> {
        var result = Tidslinje<InstitusjonsOpphold>()
        // Kjører gjennom noen ganger for å ta med per vi får med et og et nytt opphold basert på den dumme regelen her
        IntStream.range(0, max(helseoppholdUtenBarnetillegg.segmenter().count() - 1, 0)).forEach { i ->
            val oppholdSomKanGiReduksjon = Tidslinje(
                oppholdSomLiggerMindreEnnTreMånederFraForrigeSomGaReduksjon(
                    helseoppholdUtenBarnetillegg, perioderSomTrengerVurdering
                ).segmenter().mapNotNull {
                    val fom = it.fom().withDayOfMonth(1).plusMonths(1)

                    if (fom.isAfter(it.tom())) {
                        null
                    } else {
                        Segment(
                            it.periode, InstitusjonsOpphold(
                                helse = HelseOpphold(
                                    vurdering = OppholdVurdering.UAVKLART,
                                    umiddelbarReduksjon = true
                                )
                            )
                        )
                    }
                }
            ).kombiner(helsevurderingerTidslinje, helsevurderingSammenslåer())

            result = result.kombiner(oppholdSomKanGiReduksjon, sammenslåer())
        }

        return result
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
    ): Tidslinje<HelseOpphold> {
        return Tidslinje(helsevurderinger.sortedBy { it.periode }.map {
            Segment(
                it.periode, HelseOpphold(
                    if (it.faarFriKostOgLosji && it.harFasteUtgifter == false && it.forsoergerEktefelle == false) {
                        OppholdVurdering.AVSLÅTT
                    } else {
                        OppholdVurdering.GODKJENT
                    }
                )
            )
        }).komprimer()
    }

    private fun sammenslåer(): JoinStyle.OUTER_JOIN<InstitusjonsOpphold, InstitusjonsOpphold, InstitusjonsOpphold> {
        return JoinStyle.OUTER_JOIN { periode, venstreSegment, høyreSegment ->
            val venstreVerdi = venstreSegment?.verdi
            val høyreVerdi = høyreSegment?.verdi

            val soning = utledSoning(venstreVerdi?.soning, høyreVerdi?.soning)
            val helse = utledHelse(venstreVerdi?.helse, høyreVerdi?.helse)

            val verdi = InstitusjonsOpphold(helse = helse, soning = soning)
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
        oppholdUtenBarnetillegg: Tidslinje<InstitusjonsOpphold>
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
                .filter { segment ->
                    harOppholdSomVarerMerEnnFireMånederOgErMinstToMånederInnIOppholdet(segment)
                })
    }

    private fun harOppholdSomVarerMerEnnFireMånederOgErMinstToMånederInnIOppholdet(
        segment: Segment<Boolean>
    ): Boolean {
        val fom = segment.fom().withDayOfMonth(1).plusMonths(1)

        if (fom.isAfter(segment.tom())) {
            return false
        }
        val førsteDagMedMuligReduksjon = fom.plusMonths(3)
        val justertPeriode = Periode(fom, segment.tom())
        return justertPeriode.inneholder(førsteDagMedMuligReduksjon) && (fom.plusMonths(2) < LocalDate.now() || Miljø.er() == MiljøKode.DEV)
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
    ): EtAnnetStedInput {
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

        return EtAnnetStedInput(rettighetsperiode, opphold, soningsvurderinger, barnetillegg, helsevurderinger)
    }
}