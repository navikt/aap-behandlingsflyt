package no.nav.aap.behandlingsflyt.prosessering

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingService
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovOgÅrsak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.motor.ProvidersJobbSpesifikasjon
import org.slf4j.LoggerFactory

class OpprettBehandlingGReguleringJobbUtfører(
    private val prosesserBehandlingService: ProsesserBehandlingService,
    private val behandlingService: BehandlingService,
    private val behandlingRepository: BehandlingRepository,
    private val unleashGateway: UnleashGateway,
) : JobbUtfører {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun utfør(input: JobbInput) {
        if (unleashGateway.isDisabled(BehandlingsflytFeature.GReguleringsJobb)) {
            log.info("Feature toggle GReguleringsJobb er avskrudd, hopper over G-regulering")
            return
        }

        val sakId = SakId(input.sakId())

        val sisteYtelsesbehandling = behandlingService.finnSisteYtelsesbehandlingFor(sakId)
        if (sisteYtelsesbehandling != null && sisteYtelsesbehandling.status().erÅpen()) {
            log.info("Sak med id $sakId har allerede en åpen behandling (${sisteYtelsesbehandling.id}), oppretter ikke G-regulering")
            return
        }

        // TODO: Midlertidig sjekk — erstatt med pr-år-logikk når permanent løsning er på plass
        if (harFullførtGRegulering(sakId)) {
            log.info("Sak med id $sakId har allerede en fullført G-regulering, oppretter ikke ny")
            return
        }

        val sisteGjeldendeBehandling = behandlingService.finnBehandlingMedSisteFattedeVedtak(sakId)
        if (sisteGjeldendeBehandling != null) {
            log.info("Gjeldende behandling for sak $sakId er ${sisteGjeldendeBehandling.id}")

            val gReguleringBehandling = opprettNyBehandling(sakId)
            prosesserBehandlingService.triggProsesserBehandling(gReguleringBehandling)

        } else {
            log.info("Sak med id $sakId har ingen gjeldende behandlinger, ingen behandling opprettes")
        }
    }

    private fun harFullførtGRegulering(sakId: SakId): Boolean {
        return behandlingRepository.hentAlleFor(sakId).any { behandling ->
            behandling.status().erVedtatt() &&
                behandling.vurderingsbehov().any { it.type == Vurderingsbehov.G_REGULERING }
        }
    }

    private fun opprettNyBehandling(sakId: SakId): BehandlingService.OpprettetBehandling =
        behandlingService.finnEllerOpprettBehandling(
            sakId = sakId,
            vurderingsbehovOgÅrsak = VurderingsbehovOgÅrsak(
                årsak = ÅrsakTilOpprettelse.G_REGULERING,
                vurderingsbehov = listOf(VurderingsbehovMedPeriode(Vurderingsbehov.G_REGULERING))
            ),
        )

    companion object : ProvidersJobbSpesifikasjon {
        override fun konstruer(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider): JobbUtfører {
            return OpprettBehandlingGReguleringJobbUtfører(
                prosesserBehandlingService = ProsesserBehandlingService(repositoryProvider, gatewayProvider),
                behandlingService = BehandlingService(repositoryProvider, gatewayProvider),
                behandlingRepository = repositoryProvider.provide(),
                unleashGateway = gatewayProvider.provide(),
            )
        }

        override val type = "batch.OpprettBehandlingGRegulering"
        override val navn = "Opprett behandling for G-regulering"
        override val beskrivelse = "Oppretter behandling med vurderingsbehov G_REGULERING for saker som skal G-reguleres"
    }
}