package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.BrevbestillingService
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.TypeBrev
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.ÅrsakTilBehandling
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider

class SendForvaltningsmeldingSteg(
    private val brevbestillingService: BrevbestillingService,
    private val behandlingRepository: BehandlingRepository,
    private val unleashGateway: UnleashGateway,
) : BehandlingSteg {

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        if (unleashGateway.isDisabled(BehandlingsflytFeature.SendForvaltningsmelding)) {
            return Fullført
        }
        when (kontekst.behandlingType) {
            TypeBehandling.Førstegangsbehandling, TypeBehandling.Revurdering -> {
                val behandlingId = kontekst.behandlingId
                val behandling = behandlingRepository.hent(behandlingId)
                if (erBehandlingForMottattSøknad(kontekst.årsakerTilBehandling) &&
                    !harAlleredeBestiltForvaltningsmeldingForBehandling(behandling)
                ) {
                    val typeBrev = TypeBrev.FORVALTNINGSMELDING
                    brevbestillingService.bestillV2(
                        behandlingId,
                        typeBrev,
                        "typeBrev-${behandlingId.id}",
                        ferdigstillAutomatisk = true
                    )
                }
            }

            else -> {}
        }

        return Fullført
    }

    private fun harAlleredeBestiltForvaltningsmeldingForBehandling(behandling: Behandling): Boolean {
        return brevbestillingService.hentBrevbestillinger(behandling.referanse)
            .any { it.typeBrev == TypeBrev.FORVALTNINGSMELDING }
    }

    private fun erBehandlingForMottattSøknad(årsakerTilBehandling: Set<ÅrsakTilBehandling>): Boolean {
        return årsakerTilBehandling.any { it == ÅrsakTilBehandling.MOTTATT_SØKNAD }
    }

    companion object : FlytSteg {
        override fun konstruer(repositoryProvider: RepositoryProvider): BehandlingSteg {
            return SendForvaltningsmeldingSteg(
                brevbestillingService = BrevbestillingService(repositoryProvider),
                behandlingRepository = repositoryProvider.provide(),
                unleashGateway = GatewayProvider.provide(),
            )
        }

        override fun type(): StegType {
            return StegType.SEND_FORVALTNINGSMELDING
        }
    }
}
