package no.nav.aap.behandlingsflyt.prosessering

import no.nav.aap.behandlingsflyt.behandling.utbetaling.UtbetalingGateway
import no.nav.aap.behandlingsflyt.behandling.utbetaling.UtbetalingService
import no.nav.aap.behandlingsflyt.faktagrunnlag.SakOgBehandlingService
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.repository.RepositoryProvider
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.motor.ProvidersJobbSpesifikasjon
import org.slf4j.LoggerFactory
import java.time.Duration

class IverksettUtbetalingJobbUtfører(
    private val utbetalingGateway: UtbetalingGateway,
    private val utbetalingService: UtbetalingService,
    private val sakOgBehandlingService: SakOgBehandlingService
) : JobbUtfører {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun utfør(input: JobbInput) {
        val behandlingId = input.payload<BehandlingId>()
        val sakId = SakId(input.sakId())
        val sisteFattedeVedtakBehandling = sakOgBehandlingService.finnBehandlingMedSisteFattedeVedtak(sakId)
            ?: error("Finner ingen fattede vedtaksbehandlinger for sak med id $sakId")

        if (sisteFattedeVedtakBehandling.id != behandlingId) {
            log.warn("Iverksetter tilkjent ytelse for annen behandling [${sisteFattedeVedtakBehandling.id}] enn den som trigget iverksettUtbetaling-jobben [$behandlingId] for sak $sakId. " +
                    "Det som trolig har skjedd er at jobber er opprettet i ulik rekkefølge og Kelvin sikrer dette med å kun utbetale nyeste fattede vedtak.")
        }

        val tilkjentYtelseDto = utbetalingService.lagTilkjentYtelseForUtbetaling(sakId, sisteFattedeVedtakBehandling.id)
            ?: throw IllegalStateException("Ingen tilkjent ytelse på for siste utbetalingsjobb")

        log.info("Iverksetter tilkjent ytelse for behandling ${sisteFattedeVedtakBehandling.id} for sak med id $sakId")
        utbetalingGateway.utbetal(tilkjentYtelseDto)
    }

    companion object : ProvidersJobbSpesifikasjon {
        override fun konstruer(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider): JobbUtfører {
            val utbetalingGateway = gatewayProvider.provide<UtbetalingGateway>()
            val utbetalingService = UtbetalingService(repositoryProvider, gatewayProvider)
            val sakOgBehandlingService = SakOgBehandlingService(repositoryProvider, gatewayProvider)
            return IverksettUtbetalingJobbUtfører(
                utbetalingGateway = utbetalingGateway,
                utbetalingService = utbetalingService,
                sakOgBehandlingService = sakOgBehandlingService
            )
        }

        override val beskrivelse = "Overfører vedtak fra behandlingsflyt til utbetaling"
        override val navn = "Iverksett utbetaling"
        override val type = "flyt.iverksettUtbetaling"
        override val retries = 15
        override val retryBackoffTid = Duration.ofMinutes(1)

    }
}