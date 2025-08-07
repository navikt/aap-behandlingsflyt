package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.søknad.TrukketSøknadService
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.TilkjentYtelseRepository
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.VirkningstidspunktUtleder
import no.nav.aap.behandlingsflyt.behandling.utbetaling.UtbetalingGateway
import no.nav.aap.behandlingsflyt.behandling.utbetaling.UtbetalingService
import no.nav.aap.behandlingsflyt.behandling.vedtak.VedtakRepository
import no.nav.aap.behandlingsflyt.behandling.vedtak.VedtakService
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.andrestatligeytelservurdering.SamordningAndreStatligeYtelserRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.arbeidsgiver.SamordningArbeidsgiverRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.refusjonkrav.RefusjonkravRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.samordning.refusjonskrav.TjenestepensjonRefusjonsKravVurderingRepository
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.prosessering.DatadelingBehandlingJobbUtfører
import no.nav.aap.behandlingsflyt.prosessering.VarsleVedtakJobbUtfører
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.StegStatus
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import org.slf4j.LoggerFactory

class IverksettVedtakSteg private constructor(
    private val behandlingRepository: BehandlingRepository,
    private val utbetalingService: UtbetalingService,
    private val vedtakService: VedtakService,
    private val virkningstidspunktUtleder: VirkningstidspunktUtleder,
    private val utbetalingGateway: UtbetalingGateway,
    private val trukketSøknadService: TrukketSøknadService,
    private val flytJobbRepository: FlytJobbRepository
) : BehandlingSteg {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        if (kontekst.vurderingType == VurderingType.FØRSTEGANGSBEHANDLING && trukketSøknadService.søknadErTrukket(
                kontekst.behandlingId
            )
        ) {
            return Fullført
        }

        val stegHistorikk = behandlingRepository.hentStegHistorikk(kontekst.behandlingId)
        val vedtakstidspunkt =
            stegHistorikk
                .find { it.steg() == StegType.FATTE_VEDTAK && it.status() == StegStatus.AVSLUTTER }
                ?.tidspunkt() ?: error("Forventet å finne et avsluttet fatte vedtak steg")

        val virkningstidspunkt = virkningstidspunktUtleder.utledVirkningsTidspunkt(kontekst.behandlingId)
        vedtakService.lagreVedtak(kontekst.behandlingId, vedtakstidspunkt, virkningstidspunkt)

        val tilkjentYtelseDto = utbetalingService.lagTilkjentYtelseForUtbetaling(kontekst.sakId, kontekst.behandlingId)
        if (tilkjentYtelseDto != null) {
            utbetalingGateway.utbetal(tilkjentYtelseDto)
        } else {
            log.info("Fant ikke tilkjent ytelse for behandingsref ${kontekst.behandlingId}. Virkningstidspunkt: $virkningstidspunkt.")
        }
        if (GatewayProvider.provide<UnleashGateway>().isEnabled(BehandlingsflytFeature.Samvarsling)) {
            flytJobbRepository.leggTil(
                jobbInput = JobbInput(jobb = VarsleVedtakJobbUtfører).medPayload(kontekst.behandlingId)
            )
        }

        flytJobbRepository.leggTil(
            JobbInput(jobb = DatadelingBehandlingJobbUtfører).medPayload(Pair(kontekst.behandlingId, vedtakstidspunkt))
                .forBehandling(kontekst.sakId.id, kontekst.behandlingId.id)
        )

        return Fullført
    }

    companion object : FlytSteg {
        override fun konstruer(repositoryProvider: RepositoryProvider): BehandlingSteg {
            val sakRepository = repositoryProvider.provide<SakRepository>()
            val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
            val refusjonskravRepository = repositoryProvider.provide<RefusjonkravRepository>()
            val tilkjentYtelseRepository = repositoryProvider.provide<TilkjentYtelseRepository>()
            val avklaringsbehovRepository = repositoryProvider.provide<AvklaringsbehovRepository>()
            val vedtakRepository = repositoryProvider.provide<VedtakRepository>()
            val samordningAndreStatligeYtelserRepository =
                repositoryProvider.provide<SamordningAndreStatligeYtelserRepository>()
            val samordningArbeidsgiverRepository =
                repositoryProvider.provide<SamordningArbeidsgiverRepository>()
            val utbetalingGateway = GatewayProvider.provide<UtbetalingGateway>()
            val flytJobbRepository = repositoryProvider.provide<FlytJobbRepository>()
            val virkningstidspunktUtlederService = VirkningstidspunktUtleder(
                vilkårsresultatRepository = repositoryProvider.provide(),
            )
            val tjenestepensjonRefusjonsKravVurderingRepository =
                repositoryProvider.provide<TjenestepensjonRefusjonsKravVurderingRepository>()
            val underveisRepository = repositoryProvider.provide<UnderveisRepository>()
            return IverksettVedtakSteg(
                behandlingRepository = behandlingRepository,
                utbetalingService = UtbetalingService(
                    sakRepository = sakRepository,
                    behandlingRepository = behandlingRepository,
                    tilkjentYtelseRepository = tilkjentYtelseRepository,
                    avklaringsbehovRepository = avklaringsbehovRepository,
                    vedtakRepository = vedtakRepository,
                    refusjonskravRepository = refusjonskravRepository,
                    tjenestepensjonRefusjonsKravVurderingRepository = tjenestepensjonRefusjonsKravVurderingRepository,
                    samordningAndreStatligeYtelserRepository = samordningAndreStatligeYtelserRepository,
                    samordningArbeidsgiverRepository = samordningArbeidsgiverRepository,
                    underveisRepository = underveisRepository,
                ),
                vedtakService = VedtakService(vedtakRepository, behandlingRepository),
                utbetalingGateway = utbetalingGateway,
                virkningstidspunktUtleder = virkningstidspunktUtlederService,
                trukketSøknadService = TrukketSøknadService(repositoryProvider),
                flytJobbRepository = flytJobbRepository,
            )
        }

        override fun type(): StegType {
            return StegType.IVERKSETT_VEDTAK
        }
    }
}
