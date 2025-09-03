package no.nav.aap.behandlingsflyt.behandling.brev

import no.nav.aap.behandlingsflyt.behandling.Resultat
import no.nav.aap.behandlingsflyt.behandling.ResultatUtleder
import no.nav.aap.behandlingsflyt.behandling.brev.Innvilgelse.GrunnlagBeregning.InntektPerÅr
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.TilkjentYtelseRepository
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.tilTidslinje
import no.nav.aap.behandlingsflyt.behandling.vedtak.VedtakRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.Beregningsgrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.BeregningsgrunnlagRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.Grunnlag11_19
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.GrunnlagInntekt
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.GrunnlagUføre
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.GrunnlagYrkesskade
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.UføreInntekt
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.resultat.Avslått
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.resultat.DelvisOmgjøres
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.resultat.KlageresultatUtleder
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.resultat.Opprettholdes
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.Grunnbeløp
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningVurderingRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningstidspunktVurdering
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov.FASTSATT_PERIODE_PASSERT
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
    private val unleashGateway: UnleashGateway,
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
        unleashGateway = gatewayProvider.provide(),
    )

    fun utledBehovForMeldingOmVedtak(behandlingId: BehandlingId): BrevBehov? {
        val behandling = behandlingRepository.hent(behandlingId)

        when (behandling.typeBehandling()) {
            TypeBehandling.Førstegangsbehandling -> {
                val resultat = resultatUtleder.utledResultat(behandlingId)

                return when (resultat) {
                    Resultat.INNVILGELSE -> brevBehovInnvilgelse(behandling)
                    Resultat.AVSLAG -> Avslag
                    Resultat.TRUKKET -> null
                }
            }

            TypeBehandling.Revurdering -> {
                val vurderingsbehov = behandling.vurderingsbehov().map { it.type }.toSet()
                if (setOf(MOTTATT_MELDEKORT, FASTSATT_PERIODE_PASSERT).containsAll(vurderingsbehov)) {
                    return null
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

            TypeBehandling.Tilbakekreving, TypeBehandling.SvarFraAndreinstans, TypeBehandling.OppfølgingsBehandling, TypeBehandling.Aktivitetsplikt ->
                return null // TODO
        }
    }

    private fun brevBehovInnvilgelse(behandling: Behandling): Innvilgelse {
        val vedtak = checkNotNull(vedtakRepository.hent(behandling.id)) {
            "Fant ikke vedtak for behandling med innvilgelse"
        }
        checkNotNull(vedtak.virkningstidspunkt) {
            "Vedtak for behandling med innvilgelse mangler virkningstidspunkt"
        }
        val grunnlagBeregning = if (unleashGateway.isEnabled(BehandlingsflytFeature.BrevBeregningsgrunnlag)) {
            hentGrunnlagBeregning(behandling.id, vedtak.virkningstidspunkt)
        } else {
            null
        }

        val tilkjentYtelse = if (unleashGateway.isEnabled(BehandlingsflytFeature.BrevBeregningsgrunnlag)) {
            utledDagsats(behandling.id, vedtak.virkningstidspunkt)
        } else {
            null
        }

        return Innvilgelse(
            virkningstidspunkt = vedtak.virkningstidspunkt,
            grunnlagBeregning = grunnlagBeregning,
            tilkjentYtelse = tilkjentYtelse,
        )
    }

    private fun hentGrunnlagBeregning(
        behandlingId: BehandlingId,
        virkningstidspunkt: LocalDate
    ): Innvilgelse.GrunnlagBeregning {
        val grunnlag = beregningsgrunnlagRepository.hentHvisEksisterer(behandlingId)
        val beregningsgrunnlag = beregnBeregningsgrunnlagBeløp(grunnlag, virkningstidspunkt)
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

            null -> Innvilgelse.GrunnlagBeregning(null, emptyList(), beregningsgrunnlag)
        }
    }

    private fun utledGrunnlagBeregning11_9(
        grunnlag: Grunnlag11_19,
        beregningstidspunktVurdering: BeregningstidspunktVurdering?,
        beregningsgrunnlag: Beløp?,
    ): Innvilgelse.GrunnlagBeregning {
        val beregningstidspunkt = beregningstidspunktVurdering?.nedsattArbeidsevneDato
        val inntekter = grunnlag.inntekter().grunnlagInntektTilInntektPerÅr()
        return Innvilgelse.GrunnlagBeregning(
            beregningstidspunkt = beregningstidspunkt,
            inntekterPerÅr = inntekter,
            beregningsgrunnlag = beregningsgrunnlag,
        )
    }

    private fun utledGrunnlagBeregningUføre(
        grunnlag: GrunnlagUføre,
        beregningstidspunktVurdering: BeregningstidspunktVurdering?,
        beregningsgrunnlag: Beløp?,
    ): Innvilgelse.GrunnlagBeregning {
        val beregningstidspunkt = utledBeregningstidspunktUføre(grunnlag, beregningstidspunktVurdering)
        val inntekter = utledInntekterPerÅrUføre(grunnlag)
        return Innvilgelse.GrunnlagBeregning(
            beregningstidspunkt = beregningstidspunkt,
            inntekterPerÅr = inntekter,
            beregningsgrunnlag = beregningsgrunnlag,
        )
    }

    private fun utledDagsats(behandlingId: BehandlingId, virkningstidspunkt: LocalDate): Innvilgelse.TilkjentYtelse? {
        /**
         * Henter data basert på virkningstidspunkt.
         */

        val tilkjentYtelseTidslinje =
            tilkjentYtelseRepository.hentHvisEksisterer(behandlingId)?.tilTidslinje() ?: return null
        val underveisTidslinje =
            Tidslinje(underveisRepository.hent(behandlingId).perioder.map { Segment(it.periode, it) })

        return tilkjentYtelseTidslinje.innerJoin(underveisTidslinje) { _, tilkjent, underveisperiode ->

            /**
             * Gradering tar høyde for fastsatt arbeidsevne, men ikke timer arbeidet (derfor benyttes ikke
             * [no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.Tilkjent.gradering.arbeidGradering]).
             * Inkluderer ikke barnetillegg slik som
             * [no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.Tilkjent.redusertDagsats] siden barnetillegg skilles ut
             * i brevet.
             */
            val gradering = Prosent.`100_PROSENT`
                .minus(tilkjent.gradering.samordningGradering ?: Prosent.`0_PROSENT`)
                .minus(tilkjent.gradering.institusjonGradering ?: Prosent.`0_PROSENT`)
                .minus(tilkjent.gradering.samordningUføregradering ?: Prosent.`0_PROSENT`)
                .minus(tilkjent.gradering.samordningArbeidsgiverGradering ?: Prosent.`0_PROSENT`)
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

            Innvilgelse.TilkjentYtelse(
                dagsats = tilkjent.dagsats,
                gradertDagsats = gradertDagsats,
                barnetillegg = tilkjent.barnetillegg,
                gradertBarnetillegg = gradertBarnetillegg,
                gradertDagsatsInkludertBarnetillegg = gradertDagsatsInkludertBarnetillegg,
                antallBarn = tilkjent.antallBarn,
                barnetilleggsats = tilkjent.barnetilleggsats
            )
        }.segment(virkningstidspunkt)?.verdi
    }

    private fun utledBeregningstidspunktUføre(
        grunnlag: GrunnlagUføre,
        beregningstidspunktVurdering: BeregningstidspunktVurdering?
    ): LocalDate? {
        return when (grunnlag.type()) {
            GrunnlagUføre.Type.STANDARD -> beregningstidspunktVurdering?.nedsattArbeidsevneDato
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

    private fun beregnBeregningsgrunnlagBeløp(grunnlag: Beregningsgrunnlag?, virkningstidspunkt: LocalDate): Beløp? {
        return grunnlag?.grunnlaget()?.multiplisert(Grunnbeløp.finnGrunnbeløp(virkningstidspunkt))
    }
}
