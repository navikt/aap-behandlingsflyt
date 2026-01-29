package no.nav.aap.behandlingsflyt.behandling.institusjonsopphold

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

    internal fun utledBehov(
        input: InstitusjonsoppholdInput,
        begrensetTilRettighetsperiode: Boolean? = true
    ): BehovForAvklaringer {
        val opphold = input.institusjonsOpphold
        val soningsOpphold = opphold.filter { segment -> segment.verdi.type == Institusjonstype.FO }
        val helseopphold = opphold.filter { segment -> segment.verdi.type == Institusjonstype.HS }
        val barnetillegg = input.barnetillegg
        val soningsvurderingTidslinje = byggSoningsvurderingTidslinje(input.soningsvurderinger)

        // Fyll inn gaps med GODKJENT slik at de ikke krever vurdering
        val helseoppholdPerioder = helseopphold.map { it.periode }
        val helsevurderingerTidslinje = byggHelsevurderingTidslinje(
            input.helsevurderinger,
            helseoppholdPerioder
        )

        var perioderSomTrengerVurdering =
            Tidslinje(soningsOpphold)
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

        if (helseopphold.isNotEmpty()) {
            val helseOppholdTidslinje = opprettTidslinje(helseopphold)
            val barnetilleggTidslinje = barnetillegg.tilTidslinje()

            val oppholdUtenBarnetillegg =
                helseOppholdTidslinje.disjoint(barnetilleggTidslinje) { p, v -> Segment(p, v.verdi) }

            val harEksisterendeVurderinger = input.helsevurderinger.isNotEmpty()

            if (!harEksisterendeVurderinger) {
                // Ingen vurderinger - sjekk om opphold krever avklaring (3 mnd regel)
                // Oppholdet må være lengre enn 3 måneder for å være aktuelt for avklaring og må ha vart i minimum 2 måneder for å være klar for avklaring
                val oppholdSomKanGiReduksjon = harOppholdSomKreverAvklaring(oppholdUtenBarnetillegg)

                perioderSomTrengerVurdering = perioderSomTrengerVurdering.kombiner(oppholdSomKanGiReduksjon.mapValue {
                    InstitusjonsoppholdVurdering(helse = HelseOpphold(vurdering = OppholdVurdering.UAVKLART))
                }, sammenslåer()).komprimer()
            }

            // Kombiner med helsevurderinger (gaps + faktiske vurderinger)
            perioderSomTrengerVurdering = perioderSomTrengerVurdering.kombiner(
                helsevurderingerTidslinje.mapValue { InstitusjonsoppholdVurdering(helse = it) },
                sammenslåer()
            ).komprimer()
        }

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

    // TODO Thao: Ta en avsjekk med Thea om denne skal fjernes siden saksbehandler kan velge dato selv nå og som overstyrer denne regelen
    private fun regnUtTidslinjeOverOppholdSomErMindreEnnTreMånederFraForrigeSomGaReduksjon(
        perioderSomTrengerVurdering: Tidslinje<InstitusjonsoppholdVurdering>,
        helseoppholdUtenBarnetillegg: Tidslinje<Boolean>,
        helsevurderingerTidslinje: Tidslinje<HelseOpphold>
    ): Tidslinje<InstitusjonsoppholdVurdering> {
        var result = Tidslinje<InstitusjonsoppholdVurdering>()
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
                            it.periode, InstitusjonsoppholdVurdering(
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
        oppholdPerioder: List<Periode>
    ): Tidslinje<HelseOpphold> {
        // Første lag tidslinje fra saksbehandlers vurderinger
        val vurderingTidslinje = Tidslinje(helsevurderinger.sortedBy { it.periode }.map {
            Segment(
                it.periode, HelseOpphold(
                    if (it.faarFriKostOgLosji && it.harFasteUtgifter == false && it.forsoergerEktefelle == false) {
                        OppholdVurdering.AVSLÅTT
                    } else {
                        OppholdVurdering.GODKJENT
                    },
                    umiddelbarReduksjon = true
                )
            )
        })

        // Fyll inn gaps med GODKJENT for perioder som ikke er vurdert
        val vurderingMedGaps = fyllInnGapsMedGodkjentForHelseopphold(vurderingTidslinje, oppholdPerioder)

        return vurderingMedGaps.komprimer()
    }

    /**
     * Fyller inn gaps før første vurdering med GODKJENT (ingen reduksjon)
     * Dette gjør at gaps ikke krever vurdering og steget kan bekreftes
     */
    private fun fyllInnGapsMedGodkjentForHelseopphold(
        helsevurderingerTidslinje: Tidslinje<HelseOpphold>,
        oppholdPerioder: List<Periode>
    ): Tidslinje<HelseOpphold> {
        if (helsevurderingerTidslinje.isEmpty() || oppholdPerioder.isEmpty()) {
            return helsevurderingerTidslinje
        }

        val gapSegmenter = mutableListOf<Segment<HelseOpphold>>()

        oppholdPerioder.forEach { oppholdPeriode ->
            // Finn alle vurderinger for dette oppholdet
            val vurderingerForOpphold = helsevurderingerTidslinje.segmenter()
                .filter { it.periode.overlapper(oppholdPeriode) }
                .sortedBy { it.fom() }

            if (vurderingerForOpphold.isEmpty()) {
                // Ingen vurderinger for dette oppholdet - gjør ingenting (blir UAVKLART)
                return@forEach
            }

            val førsteVurdering = vurderingerForOpphold.first()
            val oppholdStart = oppholdPeriode.fom
            val vurderingStart = førsteVurdering.fom()

            // Sjekk om det er gap mellom opphold start og første vurdering
            if (vurderingStart.isAfter(oppholdStart)) {
                val gapPeriode = Periode(
                    fom = oppholdStart,
                    tom = vurderingStart.minusDays(1)
                )

                gapSegmenter.add(
                    Segment(
                        gapPeriode,
                        HelseOpphold(
                            vurdering = OppholdVurdering.GODKJENT, // Automatisk godkjent
                            umiddelbarReduksjon = false
                        )
                    )
                )
            }
        }

        return if (gapSegmenter.isEmpty()) {
            helsevurderingerTidslinje
        } else {
            helsevurderingerTidslinje.kombiner(
                Tidslinje(gapSegmenter),
                JoinStyle.OUTER_JOIN { periode, vurdering, gap ->
                    // Prioriter eksisterende vurderinger over gaps
                    val verdi = vurdering?.verdi ?: gap?.verdi ?: HelseOpphold(OppholdVurdering.UAVKLART)
                    Segment(periode, verdi)
                }
            )
        }
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
        return justertPeriode.inneholder(førsteDagMedMuligReduksjon) && (fom.plusMonths(2) <= LocalDate.now() || Miljø.er() == MiljøKode.DEV)
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