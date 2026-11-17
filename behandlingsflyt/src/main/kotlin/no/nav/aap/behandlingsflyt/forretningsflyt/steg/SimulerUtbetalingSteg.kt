package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.utbetaling.UtbetalingGateway
import no.nav.aap.behandlingsflyt.behandling.utbetaling.UtbetalingService
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.log.SECURE_LOGGER
import no.nav.aap.lookup.repository.RepositoryProvider
import org.slf4j.LoggerFactory

private val VURDERINGSTYPER_FOR_SIMULERING = listOf(
    VurderingType.FØRSTEGANGSBEHANDLING, VurderingType.REVURDERING, VurderingType.OVERGANG_UFORE_STANS, VurderingType.EFFEKTUER_AKTIVITETSPLIKT
)

class SimulerUtbetalingSteg private constructor(
    private val utbetalingService: UtbetalingService,
    private val utbetalingGateway: UtbetalingGateway,
    private val unleashGateway: UnleashGateway,
) : BehandlingSteg {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        if (unleashGateway.isDisabled(BehandlingsflytFeature.SimuleringStegForNyttUtbetalingsgrensesnitt)) {
            return Fullført
        }
        if (kontekst.vurderingType in VURDERINGSTYPER_FOR_SIMULERING) {
            val behandlingId = kontekst.behandlingId
            val sakId = kontekst.sakId

            val tilkjentYtelseDto = utbetalingService.lagTilkjentYtelseForUtbetaling(sakId, behandlingId, simulering = true)
                ?: throw IllegalStateException("Ingen tilkjent ytelse på for siste utbetalingsjobb")

            val migrertTilNyUtbetalingsløsning = utbetalingGateway.erMigrert(tilkjentYtelseDto.personIdent, tilkjentYtelseDto.saksnummer)
            if (migrertTilNyUtbetalingsløsning) {
                log.info("Henter simulering for sakId $sakId og behandlingId $behandlingId")
                val simulering = utbetalingGateway.simuleringV2(tilkjentYtelseDto)

                // Forløpig logger vi bare innholdet av simulering
                SECURE_LOGGER.info("Simulering for sakId $sakId og behandlingId $behandlingId: $simulering")
            }
        }
        return Fullført
    }

    companion object : FlytSteg {
        override fun konstruer(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider): BehandlingSteg {
            return SimulerUtbetalingSteg(
                utbetalingService = UtbetalingService(repositoryProvider, gatewayProvider),
                utbetalingGateway = gatewayProvider.provide(),
                unleashGateway = gatewayProvider.provide(),
            )
        }

        override fun type(): StegType {
            return StegType.SIMULERING
        }
    }
}
