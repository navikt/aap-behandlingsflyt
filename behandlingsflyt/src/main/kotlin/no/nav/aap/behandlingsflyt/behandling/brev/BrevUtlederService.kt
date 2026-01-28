package no.nav.aap.behandlingsflyt.behandling.brev

import no.nav.aap.behandlingsflyt.behandling.Resultat
import no.nav.aap.behandlingsflyt.behandling.ResultatUtleder
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.BeregnTilkjentYtelseService.Companion.ANTALL_ÅRLIGE_ARBEIDSDAGER
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.MINSTE_ÅRLIG_YTELSE_TIDSLINJE
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.TilkjentYtelseRepository
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.tilTidslinje
import no.nav.aap.behandlingsflyt.behandling.vedtak.VedtakRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.Aktivitetsplikt11_7Repository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.Beregningsgrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.BeregningsgrunnlagRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.Grunnlag11_19
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.GrunnlagInntekt
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.GrunnlagUføre
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.GrunnlagYrkesskade
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.UføreInntekt
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.resultat.Avslått
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.resultat.DelvisOmgjøres
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.resultat.KlageresultatUtleder
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.resultat.Opprettholdes
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.Grunnbeløp
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsopptrapping.ArbeidsopptrappingRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsopptrapping.perioderMedArbeidsopptrapping
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningVurderingRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningstidspunktVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdomsvurderingbrev.SykdomsvurderingForBrevRepository
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov.BARNETILLEGG_SATS_REGULERING
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov.EFFEKTUER_AKTIVITETSPLIKT
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov.EFFEKTUER_AKTIVITETSPLIKT_11_9
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov.FASTSATT_PERIODE_PASSERT
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov.UTVID_VEDTAKSLENGDE
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov.MIGRER_RETTIGHETSPERIODE
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov.FRITAK_MELDEPLIKT
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov.MOTTATT_MELDEKORT
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.lookup.repository.RepositoryProvider
import java.math.RoundingMode
import java.time.LocalDate

class BrevUtlederService(
    private val resultatUtleder: ResultatUtleder,
    private val klageresultatUtleder: KlageresultatUtleder,
    private val behandlingRepository: BehandlingRepository,
    private val vedtakRepository: VedtakRepository,
    private val beregningsgrunnlagRepository: BeregningsgrunnlagRepository,
    private val beregningVurderingRepository: BeregningVurderingRepository,
    private val tilkjentYtelseRepository: TilkjentYtelseRepository,
    private val underveisRepository: UnderveisRepository,
    private val aktivitetsplikt11_7Repository: Aktivitetsplikt11_7Repository,
    private val arbeidsopptrappingRepository: ArbeidsopptrappingRepository,
    private val sykdomsvurderingForBrevRepository: SykdomsvurderingForBrevRepository,
    private val unleashGateway: UnleashGateway
) {
    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        behandlingRepository = repositoryProvider.provide(),
        resultatUtleder = ResultatUtleder(repositoryProvider),
        klageresultatUtleder = KlageresultatUtleder(repositoryProvider),
        vedtakRepository = repositoryProvider.provide(),
        beregningsgrunnlagRepository = repositoryProvider.provide(),
        beregningVurderingRepository = repositoryProvider.provide(),
        tilkjentYtelseRepository = repositoryProvider.provide(),
        underveisRepository = repositoryProvider.provide(),
        aktivitetsplikt11_7Repository = repositoryProvider.provide(),
        arbeidsopptrappingRepository = repositoryProvider.provide(),
        sykdomsvurderingForBrevRepository = repositoryProvider.provide(),
        unleashGateway = gatewayProvider.provide()
    )

    fun utledBehovForMeldingOmVedtak(behandlingId: BehandlingId): BrevBehov? {
        val behandling = behandlingRepository.hent(behandlingId)
        val forrigeBehandlingId = behandling.forrigeBehandlingId
        val harBehandlingenArbeidsopptrapping =
            arbeidsopptrappingRepository.hentHvisEksisterer(behandlingId).perioderMedArbeidsopptrapping().isNotEmpty()
        val harForrigeBehandlingArbeidsopptrapping =
            forrigeBehandlingId != null && arbeidsopptrappingRepository.hentHvisEksisterer(forrigeBehandlingId)
                .perioderMedArbeidsopptrapping().isNotEmpty()
        val skalSendeVedtakForArbeidsopptrapping =
            harBehandlingenArbeidsopptrapping && !harForrigeBehandlingArbeidsopptrapping

        when (behandling.typeBehandling()) {
            TypeBehandling.Førstegangsbehandling -> {
                if (skalSendeVedtakForArbeidsopptrapping) {
                    return VedtakArbeidsopptrapping11_23_sjette_ledd
                }

                val resultat = resultatUtleder.utledResultat(behandlingId)

                return when (resultat) {
                    Resultat.INNVILGELSE -> {
                        if (harRettighetsType(behandling.id, RettighetsType.VURDERES_FOR_UFØRETRYGD)
                        ) {
                            brevBehovVurderesForUføretrygd(behandling)
                        } else {
                            brevBehovInnvilgelse(behandling)
                        }
                    }

                    Resultat.AVSLAG -> {
                        brevBehovAvslag(behandling)
                    }
                    Resultat.TRUKKET -> null
                    Resultat.AVBRUTT -> null
                }
            }

            TypeBehandling.Revurdering -> {
                if (skalSendeVedtakForArbeidsopptrapping) {
                    return VedtakArbeidsopptrapping11_23_sjette_ledd
                }

                val resultat = resultatUtleder.utledRevurderingResultat(behandlingId)
                val vurderingsbehov = behandling.vurderingsbehov().map { it.type }.toSet()
                if (setOf(
                        FRITAK_MELDEPLIKT,
                        MOTTATT_MELDEKORT,
                        FASTSATT_PERIODE_PASSERT,
                        MIGRER_RETTIGHETSPERIODE,
                        EFFEKTUER_AKTIVITETSPLIKT,
                        EFFEKTUER_AKTIVITETSPLIKT_11_9,
                    ).containsAll(
                        vurderingsbehov
                    )
                ) {
                    return null
                }

                if (vurderingsbehov == setOf(BARNETILLEGG_SATS_REGULERING)) {
                    return BarnetilleggSatsRegulering
                }

                if (vurderingsbehov == setOf(UTVID_VEDTAKSLENGDE)) {
                    return brevBehovUtvidVedtakslengde(behandling)
                }

                if (resultat == Resultat.AVBRUTT) {
                    return null
                }
                if (harRettighetsType(
                        behandling.id,
                        RettighetsType.VURDERES_FOR_UFØRETRYGD
                    ) && behandling.forrigeBehandlingId != null && !harRettighetsType(
                        behandling.forrigeBehandlingId,
                        RettighetsType.VURDERES_FOR_UFØRETRYGD
                    )
                ) {
                    return brevBehovVurderesForUføretrygd(behandling)
                }
                if (unleashGateway.isEnabled(BehandlingsflytFeature.NyBrevtype11_17) &&
                    harRettighetsType(behandling.id, RettighetsType.ARBEIDSSØKER) &&
                    behandling.forrigeBehandlingId != null &&
                    !harRettighetsType(behandling.forrigeBehandlingId, RettighetsType.ARBEIDSSØKER)
                ) {
                    return brevBehovArbeidssøker(behandling)
                }
                return VedtakEndring
            }

            TypeBehandling.Klage -> {
                val klageresulat = klageresultatUtleder.utledKlagebehandlingResultat(behandlingId)
                return when (klageresulat) {
                    is Avslått -> KlageAvvist
                    is Opprettholdes, is DelvisOmgjøres -> KlageOpprettholdelse
                    else -> null
                }
            }

            TypeBehandling.Aktivitetsplikt -> {
                val grunnlag = aktivitetsplikt11_7Repository.hentHvisEksisterer(behandlingId)
                val vurderingForBehandling =
                    grunnlag?.vurderinger?.firstOrNull { it.vurdertIBehandling == behandlingId }
                        ?: error("Finner ingen vurdering av aktivitetsplikt 11-7 for denne behandlingen - kan ikke utlede brevtype")
                return if (vurderingForBehandling.erOppfylt) {
                    VedtakEndring
                } else {
                    VedtakAktivitetsplikt11_7
                }
            }

            TypeBehandling.Aktivitetsplikt11_9 -> {
                return VedtakAktivitetsplikt11_9
            }

            TypeBehandling.Tilbakekreving, TypeBehandling.SvarFraAndreinstans, TypeBehandling.OppfølgingsBehandling, TypeBehandling.Aktivitetsplikt11_9 ->
                return null // TODO
        }
    }

    private fun brevBehovArbeidssøker(behandling: Behandling): Arbeidssøker {
        val vedtak = checkNotNull(vedtakRepository.hent(behandling.id)) {
            "Fant ikke vedtak for behandling med arbeidssøker"
        }
        checkNotNull(vedtak.virkningstidspunkt) {
            "Vedtak for behandling for arbeidssøker mangler virkningstidspunkt"
        }

        if (unleashGateway.isEnabled(BehandlingsflytFeature.ArbeidssokerBrevMedFaktagrunnlag)) {
            return Arbeidssøker(
                tilkjentYtelse = utledTilkjentYtelse(behandling.id, vedtak.virkningstidspunkt)
            )
        } else {
            return Arbeidssøker(
                tilkjentYtelse = null
            )
        }
    }
    
    private fun brevBehovUtvidVedtakslengde(behandling: Behandling): UtvidVedtakslengde {
        val underveisGrunnlag = underveisRepository.hent(behandling.id)
        return UtvidVedtakslengde(sluttdato = underveisGrunnlag.sisteDagMedYtelse())
    }

    private fun brevBehovInnvilgelse(behandling: Behandling): Innvilgelse {
        val vedtak = checkNotNull(vedtakRepository.hent(behandling.id)) {
            "Fant ikke vedtak for behandling med innvilgelse"
        }
        checkNotNull(vedtak.virkningstidspunkt) {
            "Vedtak for behandling med innvilgelse mangler virkningstidspunkt"
        }
        val grunnlagBeregning = hentGrunnlagBeregning(behandling.id, vedtak.virkningstidspunkt)

        val tilkjentYtelse = utledTilkjentYtelse(behandling.id, vedtak.virkningstidspunkt)

        val sykdomsvurdering = hentSykdomsvurdering(behandling.id)


        return Innvilgelse(
            virkningstidspunkt = vedtak.virkningstidspunkt,
            grunnlagBeregning = grunnlagBeregning,
            tilkjentYtelse = tilkjentYtelse,
            sykdomsvurdering = sykdomsvurdering,
        )
    }

    private fun brevBehovAvslag(behandling: Behandling): Avslag {
        val sykdomsvurdering = hentSykdomsvurdering(behandling.id)
        return Avslag(sykdomsvurdering = sykdomsvurdering)
    }

    private fun brevBehovVurderesForUføretrygd(behandling: Behandling): VurderesForUføretrygd {
        // Sender per nå ikke med dato som betyr at beregningsgrunnlag (beløp) blir null
        val grunnlagBeregning = hentGrunnlagBeregning(behandling.id, null)

        return VurderesForUføretrygd(
            grunnlagBeregning = grunnlagBeregning
        )
    }

    private fun hentGrunnlagBeregning(
        behandlingId: BehandlingId,
        dato: LocalDate?
    ): GrunnlagBeregning {
        val grunnlag = beregningsgrunnlagRepository.hentHvisEksisterer(behandlingId)
        val beregningsgrunnlag = beregnBeregningsgrunnlagBeløp(grunnlag, dato)
        val beregningstidspunktVurdering =
            beregningVurderingRepository.hentHvisEksisterer(behandlingId)?.tidspunktVurdering

        return when (grunnlag) {
            is Grunnlag11_19 -> {
                utledGrunnlagBeregning11_9(grunnlag, beregningstidspunktVurdering, beregningsgrunnlag)
            }

            is GrunnlagUføre -> {
                utledGrunnlagBeregningUføre(grunnlag, beregningstidspunktVurdering, beregningsgrunnlag)
            }

            is GrunnlagYrkesskade -> {
                when (val underliggende = grunnlag.underliggende()) {
                    is Grunnlag11_19 -> {
                        utledGrunnlagBeregning11_9(underliggende, beregningstidspunktVurdering, beregningsgrunnlag)
                    }

                    is GrunnlagUføre -> {
                        utledGrunnlagBeregningUføre(underliggende, beregningstidspunktVurdering, beregningsgrunnlag)
                    }

                    is GrunnlagYrkesskade -> throw IllegalStateException("GrunnlagYrkesskade kan ikke ha grunnlag som også er GrunnlagYrkesskade")
                }
            }

            null -> GrunnlagBeregning(null, emptyList(), beregningsgrunnlag)
        }
    }

    private fun hentSykdomsvurdering(behandlingId: BehandlingId): String? {
        val grunnlag = sykdomsvurderingForBrevRepository.hent(behandlingId)
        return grunnlag?.vurdering
    }

    private fun utledGrunnlagBeregning11_9(
        grunnlag: Grunnlag11_19,
        beregningstidspunktVurdering: BeregningstidspunktVurdering?,
        beregningsgrunnlag: Beløp?,
    ): GrunnlagBeregning {
        val beregningstidspunkt = beregningstidspunktVurdering?.nedsattArbeidsevneEllerStudieevneDato
        val inntekter = grunnlag.inntekter().grunnlagInntektTilInntektPerÅr()
        return GrunnlagBeregning(
            beregningstidspunkt = beregningstidspunkt,
            inntekterPerÅr = inntekter,
            beregningsgrunnlag = beregningsgrunnlag,
        )
    }

    private fun utledGrunnlagBeregningUføre(
        grunnlag: GrunnlagUføre,
        beregningstidspunktVurdering: BeregningstidspunktVurdering?,
        beregningsgrunnlag: Beløp?,
    ): GrunnlagBeregning {
        val beregningstidspunkt = utledBeregningstidspunktUføre(grunnlag, beregningstidspunktVurdering)
        val inntekter = utledInntekterPerÅrUføre(grunnlag)
        return GrunnlagBeregning(
            beregningstidspunkt = beregningstidspunkt,
            inntekterPerÅr = inntekter,
            beregningsgrunnlag = beregningsgrunnlag,
        )
    }

    private fun utledTilkjentYtelse(behandlingId: BehandlingId, virkningstidspunkt: LocalDate): TilkjentYtelse? {
        /**
         * Henter data basert på virkningstidspunkt.
         */

        val tilkjentYtelseTidslinje =
            tilkjentYtelseRepository.hentHvisEksisterer(behandlingId)?.tilTidslinje() ?: return null
        val underveidGrunnlag = underveisRepository.hent(behandlingId)
        val underveisTidslinje = Tidslinje(underveidGrunnlag.perioder.map { Segment(it.periode, it) })

        // Minste årlige ytelse beløp utledes her fra Grunnbeløp's tilTidslinje() og ikke tilTidslinjeGjennomsnitt().
        // Det gir identisk beløp som nav.no/aap/kalkulator og benytter Grunnbeløp.beløp og ikke Grunnbeløp.gjennomsnittBeløp
        val minsteÅrligYtelseBeløpTidslinje =
            MINSTE_ÅRLIG_YTELSE_TIDSLINJE.innerJoin(Grunnbeløp.tilTidslinje()) { _, minsteÅrligeYtelse, grunnbeløp ->
                minsteÅrligeYtelse.multiplisert(grunnbeløp)
            }

        return tilkjentYtelseTidslinje.innerJoin(underveisTidslinje) { _, tilkjent, underveisperiode ->
            Pair(tilkjent, underveisperiode)
        }.innerJoin(minsteÅrligYtelseBeløpTidslinje) { _, (tilkjent, underveisperiode), minsteÅrligYtelse ->

            /**
             * Gradering tar høyde for fastsatt arbeidsevne, men ikke timer arbeidet (derfor benyttes ikke
             * [no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.Tilkjent.graderingGrunnlag.arbeidGradering]).
             * Inkluderer ikke barnetillegg slik som
             * [no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.Tilkjent.redusertDagsats] siden barnetillegg skilles ut
             * i brevet.
             */
            val gradering = Prosent.`100_PROSENT`
                .minus(tilkjent.graderingGrunnlag.samordningGradering)
                .minus(tilkjent.graderingGrunnlag.institusjonGradering)
                .minus(tilkjent.graderingGrunnlag.samordningUføregradering)
                .minus(tilkjent.graderingGrunnlag.samordningArbeidsgiverGradering)
                .minus(underveisperiode.arbeidsgradering.fastsattArbeidsevne)

            /**
             * Dagsats kan variere basert på minste årlig ytelse, alder og grunnbeløp.
             */
            val gradertDagsats =
                Beløp(tilkjent.dagsats.multiplisert(gradering).verdi().setScale(0, RoundingMode.HALF_UP))
            val gradertBarnetillegg =
                Beløp(tilkjent.barnetillegg.multiplisert(gradering).verdi().setScale(0, RoundingMode.HALF_UP))
            val gradertDagsatsInkludertBarnetillegg =
                Beløp(
                    tilkjent.dagsats.pluss(tilkjent.barnetillegg).multiplisert(gradering).verdi()
                        .setScale(0, RoundingMode.HALF_UP)
                )

            TilkjentYtelse(
                dagsats = tilkjent.dagsats,
                gradertDagsats = gradertDagsats,
                barnetillegg = tilkjent.barnetillegg,
                gradertBarnetillegg = gradertBarnetillegg,
                gradertDagsatsInkludertBarnetillegg = gradertDagsatsInkludertBarnetillegg,
                antallBarn = tilkjent.antallBarn,
                barnetilleggsats = tilkjent.barnetilleggsats,
                minsteÅrligYtelse = minsteÅrligYtelse,
                minsteÅrligYtelseUnder25 = Beløp(minsteÅrligYtelse.toTredjedeler()),
                årligYtelse = tilkjent.dagsats.multiplisert(ANTALL_ÅRLIGE_ARBEIDSDAGER),
                sisteDagMedYtelse = underveidGrunnlag.sisteDagMedYtelse()
            )
        }.segment(virkningstidspunkt)?.verdi
    }

    private fun utledBeregningstidspunktUføre(
        grunnlag: GrunnlagUføre,
        beregningstidspunktVurdering: BeregningstidspunktVurdering?
    ): LocalDate? {
        return when (grunnlag.type()) {
            GrunnlagUføre.Type.STANDARD -> beregningstidspunktVurdering?.nedsattArbeidsevneEllerStudieevneDato
            GrunnlagUføre.Type.YTTERLIGERE_NEDSATT -> beregningstidspunktVurdering?.ytterligereNedsattArbeidsevneDato
        }
    }

    private fun utledInntekterPerÅrUføre(grunnlag: GrunnlagUføre): List<InntektPerÅr> {
        return when (grunnlag.type()) {
            GrunnlagUføre.Type.STANDARD -> grunnlag.underliggende().inntekter().grunnlagInntektTilInntektPerÅr()
            GrunnlagUføre.Type.YTTERLIGERE_NEDSATT -> grunnlag.uføreInntekterFraForegåendeÅr()
                .uføreInntektTilInntektPerÅr()
        }
    }

    private fun List<GrunnlagInntekt>.grunnlagInntektTilInntektPerÅr(): List<InntektPerÅr> {
        return this.map { InntektPerÅr(it.år, it.inntektIKroner.verdi()) }
    }

    private fun List<UføreInntekt>.uføreInntektTilInntektPerÅr(): List<InntektPerÅr> {
        return this.map { InntektPerÅr(it.år, it.inntektIKroner.verdi()) }
    }

    private fun beregnBeregningsgrunnlagBeløp(grunnlag: Beregningsgrunnlag?, dato: LocalDate?): Beløp? {
        if (dato == null) {
            return null
        }
        val grunnlaget = grunnlag?.grunnlaget() ?: return null
        val grunnlagetBeløp = grunnlaget.multiplisert(Grunnbeløp.finnGrunnbeløp(dato))
        return Beløp(grunnlagetBeløp.verdi.setScale(0, RoundingMode.HALF_UP))
    }

    private fun harRettighetsType(behandlingId: BehandlingId, rettighetsType: RettighetsType): Boolean {
        return underveisRepository.hentHvisEksisterer(behandlingId)
            ?.perioder
            .orEmpty()
            .any {
                it.utfall == Utfall.OPPFYLT &&
                        it.rettighetsType == rettighetsType
            }
    }
}
