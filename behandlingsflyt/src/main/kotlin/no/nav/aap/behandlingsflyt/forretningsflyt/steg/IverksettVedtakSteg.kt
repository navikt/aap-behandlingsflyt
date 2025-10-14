package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.avbrytrevurdering.AvbrytRevurderingService
import no.nav.aap.behandlingsflyt.behandling.mellomlagring.MellomlagretVurderingRepository
import no.nav.aap.behandlingsflyt.behandling.søknad.TrukketSøknadService
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.VirkningstidspunktUtleder
import no.nav.aap.behandlingsflyt.behandling.utbetaling.UtbetalingGateway
import no.nav.aap.behandlingsflyt.behandling.utbetaling.UtbetalingService
import no.nav.aap.behandlingsflyt.behandling.vedtak.VedtakRepository
import no.nav.aap.behandlingsflyt.behandling.vedtak.VedtakService
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.prosessering.DatadelingBehandlingJobbUtfører
import no.nav.aap.behandlingsflyt.prosessering.IverksettUtbetalingJobbUtfører
import no.nav.aap.behandlingsflyt.prosessering.VarsleVedtakJobbUtfører
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.StegStatus
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import no.nav.aap.utbetal.tilkjentytelse.TilkjentYtelseDto
import org.slf4j.LoggerFactory

class IverksettVedtakSteg private constructor(
    private val behandlingRepository: BehandlingRepository,
    private val utbetalingService: UtbetalingService,
    private val vedtakService: VedtakService,
    private val virkningstidspunktUtleder: VirkningstidspunktUtleder,
    private val utbetalingGateway: UtbetalingGateway,
    private val trukketSøknadService: TrukketSøknadService,
    private val avbrytRevurderingService: AvbrytRevurderingService,
    private val flytJobbRepository: FlytJobbRepository,
    private val unleashGateway: UnleashGateway,
    private val mellomlagretVurderingRepository: MellomlagretVurderingRepository
) : BehandlingSteg {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        if (kontekst.vurderingType == VurderingType.FØRSTEGANGSBEHANDLING && trukketSøknadService.søknadErTrukket(
                kontekst.behandlingId
            )
            || kontekst.vurderingType == VurderingType.REVURDERING && avbrytRevurderingService.revurderingErAvbrutt(
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
            utbetal(kontekst, tilkjentYtelseDto)
        } else {
            log.info("Fant ikke tilkjent ytelse for behandingsref ${kontekst.behandlingId}. Virkningstidspunkt: $virkningstidspunkt.")
        }
        flytJobbRepository.leggTil(
            jobbInput = JobbInput(jobb = VarsleVedtakJobbUtfører).medPayload(kontekst.behandlingId).forBehandling(kontekst.sakId.id,kontekst.behandlingId.id)
        )


        flytJobbRepository.leggTil(
            JobbInput(jobb = DatadelingBehandlingJobbUtfører).medPayload(Pair(kontekst.behandlingId, vedtakstidspunkt))
                .forBehandling(kontekst.sakId.id, kontekst.behandlingId.id)
        )

        mellomlagretVurderingRepository.slett(kontekst.behandlingId)

        return Fullført
    }

    private fun utbetal(
        kontekst: FlytKontekstMedPerioder,
        tilkjentYtelseDto: TilkjentYtelseDto
    ) {
        if (unleashGateway.isEnabled(BehandlingsflytFeature.IverksettUtbetalingSomSelvstendigJobb)) {
            /**
             * Må opprette jobb med sakId, men uten behandlingId for at disse skal bli kjørt sekvensielt i riktig rekkefølge.
             * Viktig at eldste jobb kjøres først slik at utbetaling blir konsistent med Kelvin
             */
            flytJobbRepository.leggTil(
                jobbInput = JobbInput(jobb = IverksettUtbetalingJobbUtfører)
                    .medPayload(kontekst.behandlingId)
                    .forSak(sakId = kontekst.sakId.toLong())
            )
        } else {
            utbetalingGateway.utbetal(tilkjentYtelseDto)
        }
    }

    companion object : FlytSteg {
        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): BehandlingSteg {
            val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
            val vedtakRepository = repositoryProvider.provide<VedtakRepository>()
            val utbetalingGateway = gatewayProvider.provide<UtbetalingGateway>()
            val flytJobbRepository = repositoryProvider.provide<FlytJobbRepository>()
            val virkningstidspunktUtlederService = VirkningstidspunktUtleder(
                vilkårsresultatRepository = repositoryProvider.provide(),
            )
            val mellomlagretVurderingRepository = repositoryProvider.provide<MellomlagretVurderingRepository>()
            return IverksettVedtakSteg(
                behandlingRepository = behandlingRepository,
                utbetalingService = UtbetalingService(
                    repositoryProvider = repositoryProvider,
                    gatewayProvider = gatewayProvider
                ),
                vedtakService = VedtakService(vedtakRepository, behandlingRepository),
                utbetalingGateway = utbetalingGateway,
                virkningstidspunktUtleder = virkningstidspunktUtlederService,
                trukketSøknadService = TrukketSøknadService(repositoryProvider),
                avbrytRevurderingService = AvbrytRevurderingService(repositoryProvider),
                flytJobbRepository = flytJobbRepository,
                unleashGateway = gatewayProvider.provide(),
                mellomlagretVurderingRepository = mellomlagretVurderingRepository
            )
        }

        override fun type(): StegType {
            return StegType.IVERKSETT_VEDTAK
        }
    }
}
