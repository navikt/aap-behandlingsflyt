package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.brev.Forvaltningsmelding
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
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider

class SendForvaltningsmeldingSteg(
    private val brevbestillingService: BrevbestillingService,
    private val behandlingRepository: BehandlingRepository,
) : BehandlingSteg {

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        when (kontekst.behandlingType) {
            TypeBehandling.Førstegangsbehandling, TypeBehandling.Revurdering -> {
                val behandlingId = kontekst.behandlingId
                val behandling = behandlingRepository.hent(behandlingId)
                if (erBehandlingForMottattSøknad(kontekst.vurderingsbehovRelevanteForSteg) &&
                    !harAlleredeBestiltForvaltningsmeldingForBehandling(behandling)
                ) {
                    val brevBehov = Forvaltningsmelding
                    brevbestillingService.bestillV2(
                        behandlingId = behandlingId,
                        brevBehov = brevBehov,
                        unikReferanse = "${behandling.referanse}-${brevBehov.typeBrev}",
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

    private fun erBehandlingForMottattSøknad(årsakerTilBehandling: Set<Vurderingsbehov>): Boolean {
        return Vurderingsbehov.MOTTATT_SØKNAD in årsakerTilBehandling
    }

    companion object : FlytSteg {
        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): BehandlingSteg {
            return SendForvaltningsmeldingSteg(
                brevbestillingService = BrevbestillingService(repositoryProvider, gatewayProvider),
                behandlingRepository = repositoryProvider.provide(),
            )
        }

        override fun type(): StegType {
            return StegType.SEND_FORVALTNINGSMELDING
        }
    }
}
