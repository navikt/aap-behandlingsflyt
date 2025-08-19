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
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.resultat.Avslått
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.resultat.DelvisOmgjøres
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.resultat.KlageresultatUtleder
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.resultat.Opprettholdes
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.Grunnbeløp
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningVurderingRepository
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov.FASTSATT_PERIODE_PASSERT
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov.MOTTATT_MELDEKORT
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.lookup.repository.RepositoryProvider
import java.time.LocalDate

class BrevUtlederService(
    private val resultatUtleder: ResultatUtleder,
    private val klageresultatUtleder: KlageresultatUtleder,
    private val behandlingRepository: BehandlingRepository,
    private val vedtakRepository: VedtakRepository,
    private val beregningsgrunnlagRepository: BeregningsgrunnlagRepository,
    private val beregningVurderingRepository: BeregningVurderingRepository,
    private val tilkjentYtelseRepository: TilkjentYtelseRepository,
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

            TypeBehandling.Tilbakekreving, TypeBehandling.SvarFraAndreinstans, TypeBehandling.OppfølgingsBehandling ->
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
        return Innvilgelse(vedtak.virkningstidspunkt, grunnlagBeregning)
    }

    private fun hentGrunnlagBeregning(
        behandlingId: BehandlingId,
        virkningstidspunkt: LocalDate
    ): Innvilgelse.GrunnlagBeregning {
        val grunnlag = beregningsgrunnlagRepository.hentHvisEksisterer(behandlingId)

        return Innvilgelse.GrunnlagBeregning(
            dagsats = utledDagsats(behandlingId, virkningstidspunkt)?.verdi,
            beregningstidspunkt = hentBeregningstidspunkt(behandlingId),
            beregningsgrunnlagBeløp = beregnBeregningsgrunnlagBeløp(grunnlag, virkningstidspunkt)?.verdi,
            inntekterPerÅr = utledInntektererPerÅr(grunnlag)
        )
    }

    private fun utledDagsats(behandlingId: BehandlingId, virkningstidspunkt: LocalDate): Beløp? {
        // Henter dagsats fra første periode. Kan variere basert på minste årlig ytelse, alder og grunnbeløp
        return tilkjentYtelseRepository.hentHvisEksisterer(behandlingId)?.tilTidslinje()
            ?.segment(virkningstidspunkt)?.verdi?.dagsats
    }

    private fun hentBeregningstidspunkt(behandlingId: BehandlingId): LocalDate? {
        return beregningVurderingRepository.hentHvisEksisterer(behandlingId)?.tidspunktVurdering?.let {
            it.ytterligereNedsattArbeidsevneDato ?: it.nedsattArbeidsevneDato
        }
    }

    private fun beregnBeregningsgrunnlagBeløp(grunnlag: Beregningsgrunnlag?, virkningstidspunkt: LocalDate): Beløp? {
        return grunnlag?.grunnlaget()?.multiplisert(grunnbeløp(virkningstidspunkt))
    }

    private fun grunnbeløp(dato: LocalDate): Beløp {
        return checkNotNull(Grunnbeløp.tilTidslinje().segment(dato)?.verdi) {
            "Fant ikke grunnbeløp for dato $dato."
        }
    }

    private fun utledInntektererPerÅr(grunnlag: Beregningsgrunnlag?): List<InntektPerÅr> {
        return when (grunnlag) {
            is Grunnlag11_19 ->
                grunnlag.inntekter().grunnlagInntekttilInntektPerÅr()

            is GrunnlagUføre -> grunnlag.uføreInntekterFraForegåendeÅr().uføreInntekttilInntektPerÅr()
            is GrunnlagYrkesskade ->
                when (val underliggende = grunnlag.underliggende()) {
                    is Grunnlag11_19 -> underliggende.inntekter().grunnlagInntekttilInntektPerÅr()
                    is GrunnlagUføre -> underliggende.uføreInntekterFraForegåendeÅr().uføreInntekttilInntektPerÅr()
                    is GrunnlagYrkesskade -> throw IllegalStateException("GrunnlagYrkesskade kan ikke ha grunnlag som også er GrunnlagYrkesskade")
                }

            null -> emptyList()
        }
    }

    private fun List<GrunnlagInntekt>.grunnlagInntekttilInntektPerÅr(): List<InntektPerÅr> {
        return this.map { InntektPerÅr(it.år, it.inntektIKroner.verdi()) }
    }

    private fun List<UføreInntekt>.uføreInntekttilInntektPerÅr(): List<InntektPerÅr> {
        return this.map { InntektPerÅr(it.år, it.inntektIKroner.verdi()) }
    }
}
