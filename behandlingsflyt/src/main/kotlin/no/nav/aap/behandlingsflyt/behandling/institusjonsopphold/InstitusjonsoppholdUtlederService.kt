package no.nav.aap.behandlingsflyt.behandling.institusjonsopphold

import no.nav.aap.behandlingsflyt.behandling.barnetillegg.RettTilBarnetillegg
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.barnetillegg.BarnetilleggRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.barnetillegg.tilTidslinje
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.Helseoppholdvurderinger
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.Institusjon
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.InstitusjonsoppholdRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.Institusjonstype
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.Soningsvurderinger
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon.flate.OppholdVurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.tidslinje.JoinStyle
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.StandardSammenslåere
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.orEmpty
import no.nav.aap.komponenter.tidslinje.somTidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.lookup.repository.RepositoryProvider
import java.time.LocalDate
import java.util.stream.*
import kotlin.math.max

class InstitusjonsoppholdUtlederService(
    private val barnetilleggRepository: BarnetilleggRepository,
    private val institusjonsoppholdRepository: InstitusjonsoppholdRepository,
    private val sakRepository: SakRepository,
    private val behandlingRepository: BehandlingRepository,
    private val unleashGateway: UnleashGateway
) {
    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        barnetilleggRepository = repositoryProvider.provide(),
        institusjonsoppholdRepository = repositoryProvider.provide(),
        sakRepository = repositoryProvider.provide(),
        behandlingRepository = repositoryProvider.provide(),
        unleashGateway = gatewayProvider.provide<UnleashGateway>()
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

        val helseoppholdvurderinger = input.helsevurderinger

        val helsevurderingerTidslinje = byggHelsevurderingTidslinje(
            helseoppholdvurderinger,
            helseopphold
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
            val helseOppholdTidslinje = helseopphold.somTidslinje({ it.periode }, { true })
            val barnetilleggTidslinje = barnetillegg.tilTidslinje()

            //fjern perioder hvor bruker har barnetillegg gjennom hele helseinstitusjonsoppholdet
            val oppholdUtenBarnetillegg =
                helseOppholdTidslinje.disjoint(barnetilleggTidslinje) { p, v -> Segment(p, v.verdi) }

            // Kjedene bygges på de ORIGINALE institusjonsoppholds-segmentene (før barnetillegg splitter dem),
            // slik at en barnetillegg-indusert splitt midt i et fysisk sammenhengende opphold ikke
            // forstyrrer varighets-/kjedeberegningen.
            var oppholdSomKanGiReduksjon = harOppholdSomKreverAvklaring(oppholdUtenBarnetillegg, helseopphold)

            //Håndterer den sære casen ved at barnetillegg opphører
            oppholdSomKanGiReduksjon =
                giNyTidslinjeHvisBarneTilleggTarSluttUnderOppholdet(
                    barnetilleggTidslinje,
                    oppholdSomKanGiReduksjon,
                    helseOppholdTidslinje
                )

            perioderSomTrengerVurdering = perioderSomTrengerVurdering.kombiner(oppholdSomKanGiReduksjon.mapValue {
                InstitusjonsoppholdVurdering(helse = HelseOpphold(vurdering = OppholdVurdering.UAVKLART))
            }, sammenslåer()).kombiner(helsevurderingerTidslinje, helsevurderingSammenslåer()).komprimer()

            // Hvis det er mindre en 3 måneder siden sist opphold og bruker er nå innlagt
            val helseoppholdUtenBarnetillegg = helseOppholdTidslinje.disjoint(
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

    private fun regnUtTidslinjeOverOppholdSomErMindreEnnTreMånederFraForrigeSomGaReduksjon(
        perioderSomTrengerVurdering: Tidslinje<InstitusjonsoppholdVurdering>,
        helseoppholdUtenBarnetillegg: Tidslinje<Boolean>,
        helsevurderingerTidslinje: Tidslinje<HelseOpphold>
    ): Tidslinje<InstitusjonsoppholdVurdering> {
        var result = Tidslinje<InstitusjonsoppholdVurdering>()
        // Kjører gjennom noen ganger for å ta med per vi får med et og et nytt opphold basert på den dumme regelen her
        IntStream.range(0, max(helseoppholdUtenBarnetillegg.segmenter().count() - 1, 0)).forEach { _ ->
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
        soningsvurderinger: Soningsvurderinger?
    ): Tidslinje<SoningOpphold> {
        return soningsvurderinger?.tilTidslinje().orEmpty()
            .map { SoningOpphold(if (it.skalOpphøre) OppholdVurdering.AVSLÅTT else OppholdVurdering.GODKJENT) }
            .komprimer()
    }

    private fun byggHelsevurderingTidslinje(
        helsevurderinger: Helseoppholdvurderinger?,
        helseopphold: List<Segment<Institusjon>>
    ): Tidslinje<HelseOpphold> {
        val alleVurderinger = helsevurderinger?.vurderinger.orEmpty()

        val helseoppholdPerioder =
            if (unleashGateway.isEnabled(BehandlingsflytFeature.SammenhengendeInstitusjonsopphold)) {
                grupperSammenhengendeOppholdSegmenter(helseopphold).map { it.periode }
            } else {
                helseopphold.map { it.periode }
            }

        val vurderingTidslinje = alleVurderinger
            .filterNot { it.erHistoriskUtenReduksjonsberegning }
            .let { Tidslinje(it.map { v -> Segment(v.periode, v) }) }
            .map {
                HelseOpphold(
                    if (it.giReduksjon()) {
                        OppholdVurdering.AVSLÅTT
                    } else {
                        OppholdVurdering.GODKJENT
                    },
                    umiddelbarReduksjon = true
                )
            }

        // Historiske vurderinger matches eksplisitt til oppholdets faktiske periode via oppholdId,
        // siden vurderingens egen periode kan ligge utenfor oppholdets varighet.
        val historiskeSegmenter = alleVurderinger
            .filter { it.erHistoriskUtenReduksjonsberegning }
            .mapNotNull { vurdering ->
                val matchendeOpphold = helseopphold.firstOrNull {
                    lagOppholdId(it.verdi.navn, it.periode.fom) == vurdering.oppholdId
                } ?: return@mapNotNull null

                Segment(
                    matchendeOpphold.periode,
                    HelseOpphold(
                        vurdering = OppholdVurdering.GODKJENT,
                        umiddelbarReduksjon = false
                    )
                )
            }

        val kombinertTidslinje = vurderingTidslinje.kombiner(
            Tidslinje(historiskeSegmenter),
            StandardSammenslåere.prioriterVenstreSideCrossJoin()
        )

        val vurderingMedGaps = fyllInnGapsMedGodkjentForHelseopphold(
            kombinertTidslinje, helseoppholdPerioder
        )

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

    /**
     * En periode "krever avklaring" dersom:
     * 1) Den inngår i en faktisk SAMMENHENGENDE kjede (0 dagers gap) hvis totale varighet
     *    er minst 4 måneder og ikke for kort, ELLER
     * 2) Det finnes en FORRIGE (tidligere) kjede som selv er kvalifisert, og denne kjeden
     *    starter innen 3 måneder etter at den forrige kjeden sluttet - uavhengig av denne
     *    kjedens egen varighet.
     *
     * NB: Kjede-basert logikk (punkt 1 og 2) kjører kun når featuren
     * SammenhengendeInstitusjonsopphold er PÅ. Når featuren er AV, brukes den
     * opprinnelige segment-for-segment-logikken (pre-PR).
     */
    private fun harOppholdSomKreverAvklaring(
        oppholdUtenBarnetillegg: Tidslinje<Boolean>,
        originaleOppholdSegmenter: List<Segment<Institusjon>> = emptyList(),
        ignorerVarighetsBegrensning: Boolean? = false
    ): Tidslinje<Boolean> {
        val segmenter = oppholdUtenBarnetillegg.segmenter().sortedBy { it.periode.fom }

        if (!unleashGateway.isEnabled(BehandlingsflytFeature.SammenhengendeInstitusjonsopphold)) {
            return Tidslinje(
                segmenter.filter { segment ->
                    val forrigePeriodeTom = segmenter
                        .filter { it.periode.tom.isBefore(segment.periode.fom) }
                        .maxOfOrNull { it.periode.tom }

                    val mindreEnnTreMånederFraForrige = forrigePeriodeTom != null &&
                            segment.periode.fom.isBefore(forrigePeriodeTom.plusMonths(3))

                    if (ignorerVarighetsBegrensning == true) {
                        true
                    } else {
                        mindreEnnTreMånederFraForrige ||
                                (harOppholdSomVarerMinstFireMånederOgIkkeErForKort(segment) &&
                                        harOppholdSomVarerMerEnnFireMånederOgErMinstToMånederInnIOppholdet(
                                            segment,
                                            oppholdUtenBarnetillegg.minDato()
                                        ))
                    }
                }
            )
        }

        val kjeder = grupperSammenhengende(
            originaleOppholdSegmenter,
            fom = { it.periode.fom },
            tom = { it.periode.tom },
            erSammenhengende = { sistePeriode, nesteFom -> !nesteFom.isAfter(sistePeriode.tom.plusDays(1)) }
        ).sortedBy { it.periode.fom }

        val oppholdUtenBarnetileggMinDato = oppholdUtenBarnetillegg.takeIf { it.isNotEmpty() }?.minDato()

        fun kjedeOppfyllerVarighetskrav(kjedePeriode: Periode): Boolean {
            val startDato = oppholdUtenBarnetileggMinDato ?: return false
            val kjedeSegment = Segment(kjedePeriode, true)
            return harOppholdSomVarerMinstFireMånederOgIkkeErForKort(kjedeSegment) &&
                    harOppholdSomVarerMerEnnFireMånederOgErMinstToMånederInnIOppholdet(kjedeSegment, startDato)
        }

        fun erInnenTreMånederEtterForrige(forrige: Periode, denne: Periode): Boolean =
            denne.fom.isBefore(forrige.tom.plusMonths(3))

        val startFlagg = kjeder.map { kjedeOppfyllerVarighetskrav(it.periode) }

        // Propager KUN forover: en ikke-flagget kjede flagges hvis FORRIGE kjede er flagget
        // og denne kjeden starter innen 3 måneder etter at forrige sluttet.
        fun propagerEnGang(flagg: List<Boolean>): List<Boolean> =
            flagg.indices.map { i ->
                flagg[i] ||
                        (i > 0 && flagg[i - 1] && erInnenTreMånederEtterForrige(
                            kjeder[i - 1].periode,
                            kjeder[i].periode
                        ))
            }

        val kjedeFlagget = generateSequence(startFlagg, ::propagerEnGang)
            .zipWithNext()
            .firstOrNull { (forrige, neste) -> forrige == neste }
            ?.second
            ?: startFlagg

        fun kjedeIndexFor(periode: Periode): Int =
            kjeder.indexOfFirst { it.periode.overlapper(periode) }

        return Tidslinje(
            segmenter.filter { segment ->
                if (ignorerVarighetsBegrensning == true) {
                    true
                } else {
                    val idx = kjedeIndexFor(segment.periode)
                    idx >= 0 && kjedeFlagget[idx]
                }
            }
        ).komprimer()
    }

    private fun harOppholdSomVarerMinstFireMånederOgIkkeErForKort(segment: Segment<Boolean>): Boolean {
        val fom = segment.fom().withDayOfMonth(1).plusMonths(1)
        if (fom.isAfter(segment.tom())) {
            return false
        }
        val førsteDagMedMuligReduksjon = fom.plusMonths(3)
        return Periode(fom, segment.tom()).inneholder(førsteDagMedMuligReduksjon)
    }

    private fun harOppholdSomVarerMerEnnFireMånederOgErMinstToMånederInnIOppholdet(
        segment: Segment<Boolean>,
        oppholdStartDato: LocalDate
    ): Boolean {
        val fom = segment.fom().withDayOfMonth(1).plusMonths(1)

        if (fom.isAfter(segment.tom())) {
            return false
        }
        val førsteDagMedMuligReduksjon = fom.plusMonths(3)
        val justertPeriode = Periode(fom, segment.tom())
        return justertPeriode.inneholder(førsteDagMedMuligReduksjon) && (oppholdStartDato.plusMonths(2) <= LocalDate.now())
    }

    private fun konstruerInput(
        behandlingId: BehandlingId,
        basertPåVurderingerFørDenneBehandlingen: Boolean
    ): InstitusjonsoppholdInput {
        val behandling = behandlingRepository.hent(behandlingId)
        val rettighetsperiode = sakRepository.hent(behandling.sakId).rettighetsperiode
        val grunnlag = institusjonsoppholdRepository.hentHvisEksisterer(behandlingId)
        val barnetillegg = barnetilleggRepository.hentHvisEksisterer(behandlingId)?.perioder.orEmpty()

        val alleOpphold = grunnlag?.oppholdene?.opphold.orEmpty()
        val opphold = if (unleashGateway.isEnabled(BehandlingsflytFeature.SammenhengendeInstitusjonsopphold)) {
            finnRelevanteOppholdSegmenter(alleOpphold, rettighetsperiode)
        } else {
            alleOpphold.filter { it.periode.overlapper(rettighetsperiode) }
        }

        val soningsvurderinger: Soningsvurderinger?
        val helsevurderinger: Helseoppholdvurderinger?
        if (basertPåVurderingerFørDenneBehandlingen) {
            val forrigeGrunnlag =
                behandling.forrigeBehandlingId?.let { institusjonsoppholdRepository.hentHvisEksisterer(it) }
            soningsvurderinger = forrigeGrunnlag?.soningsVurderinger
            helsevurderinger = forrigeGrunnlag?.helseoppholdvurderinger
        } else {
            soningsvurderinger = grunnlag?.soningsVurderinger
            helsevurderinger = grunnlag?.helseoppholdvurderinger
        }

        return InstitusjonsoppholdInput(
            rettighetsperiode,
            institusjonsOpphold = opphold,
            soningsvurderinger = soningsvurderinger,
            barnetillegg = barnetillegg,
            helsevurderinger = helsevurderinger
        )
    }

    private fun giNyTidslinjeHvisBarneTilleggTarSluttUnderOppholdet(
        barnetilleggTidslinje: Tidslinje<RettTilBarnetillegg>,
        oppholdSomKanGiReduksjon: Tidslinje<Boolean>,
        helseOppholdTidslinje: Tidslinje<Boolean>
    ): Tidslinje<Boolean> {
        val barnetilleggSlutterUnderPågåendeOpphold =
            barnetilleggTidslinje.isNotEmpty() && barnetilleggTidslinje.maxDato() <= helseOppholdTidslinje.maxDato()
        val harGapIBarnetilleggUnderOpphold =
            harGapIBarnetilleggSomOverlapperMedOpphold(barnetilleggTidslinje, helseOppholdTidslinje)

        return if (barnetilleggSlutterUnderPågåendeOpphold || harGapIBarnetilleggUnderOpphold) {
            harOppholdSomKreverVurderingEtterStoppIBarneTillegg(
                barnetilleggTidslinje,
                helseOppholdTidslinje,
            )
        } else oppholdSomKanGiReduksjon
    }

    private fun harGapIBarnetilleggSomOverlapperMedOpphold(
        barnetilleggTidslinje: Tidslinje<RettTilBarnetillegg>,
        helseOppholdTidslinje: Tidslinje<Boolean>
    ): Boolean {
        return barnetilleggTidslinje.segmenter().sortedBy { it.fom() }
            .zipWithNext()
            .any { (current, next) ->
                val gapStart = current.tom().plusDays(1)
                val gapEnd = next.fom().minusDays(1)
                !gapEnd.isBefore(gapStart) &&
                        helseOppholdTidslinje.begrensetTil(Periode(gapStart, gapEnd)).isNotEmpty()
            }
    }

    private fun harOppholdSomKreverVurderingEtterStoppIBarneTillegg(
        barnetilleggTidslinje: Tidslinje<RettTilBarnetillegg>,
        helseOppholdTidslinje: Tidslinje<Boolean>
    ): Tidslinje<Boolean> {
        val oppholdFørBarnetillegg = if (barnetilleggTidslinje.minDato() > helseOppholdTidslinje.minDato()) {
            harOppholdSomKreverAvklaring(
                helseOppholdTidslinje.begrensetTil(
                    Periode(
                        fom = helseOppholdTidslinje.minDato(),
                        tom = barnetilleggTidslinje.minDato().minusDays(1)
                    )
                ),
                ignorerVarighetsBegrensning = true
            )
        } else {
            Tidslinje.empty()
        }

        val oppholdIBarneTilleggGapSomKreverAvklaring = barnetilleggTidslinje.segmenter().sortedBy { it.fom() }
            .zipWithNext()
            .mapNotNull { (current, next) ->
                val gapStart = current.tom().plusDays(1)
                val gapEnd = next.fom().minusDays(1)
                if (gapEnd.isBefore(gapStart)) return@mapNotNull null
                val helseIGap = helseOppholdTidslinje.begrensetTil(Periode(gapStart, gapEnd))
                if (helseIGap.isEmpty()) return@mapNotNull null
                harOppholdSomKreverAvklaring(helseIGap, ignorerVarighetsBegrensning = true)
            }
            .fold(Tidslinje.empty<Boolean>()) { acc, tidslinje ->
                acc.kombiner(tidslinje, StandardSammenslåere.prioriterHøyreSideCrossJoin())
            }

        val oppholdEtterBarnetillegg = if (helseOppholdTidslinje.maxDato() > barnetilleggTidslinje.maxDato()) {
            harOppholdSomKreverAvklaring(
                helseOppholdTidslinje.begrensetTil(
                    Periode(
                        fom = barnetilleggTidslinje.maxDato().plusDays(1),
                        tom = helseOppholdTidslinje.maxDato()
                    )
                ),
                ignorerVarighetsBegrensning = true
            )
        } else {
            Tidslinje.empty()
        }

        return oppholdFørBarnetillegg
            .kombiner(
                oppholdIBarneTilleggGapSomKreverAvklaring,
                joinStyle = StandardSammenslåere.prioriterVenstreSideCrossJoin()
            )
            .kombiner(oppholdEtterBarnetillegg, joinStyle = StandardSammenslåere.prioriterVenstreSideCrossJoin())
    }
}

private val SEGMENT_ER_SAMMENHENGENDE: (Periode, LocalDate) -> Boolean =
    { periode, nesteFom -> !nesteFom.isAfter(periode.tom.plusDays(1)) }

fun finnRelevanteOppholdSegmenter(
    segmenter: List<Segment<Institusjon>>,
    periode: Periode
): List<Segment<Institusjon>> {
    return finnRelevanteInnenforPeriode(
        segmenter, periode, { it.periode.fom }, { it.periode.tom }, SEGMENT_ER_SAMMENHENGENDE
    )
}

fun grupperSammenhengendeOppholdSegmenter(
    segmenter: List<Segment<Institusjon>>
): List<SammenhengendeGruppe<Segment<Institusjon>>> {
    return grupperSammenhengende(segmenter, { it.periode.fom }, { it.periode.tom }, SEGMENT_ER_SAMMENHENGENDE)
}