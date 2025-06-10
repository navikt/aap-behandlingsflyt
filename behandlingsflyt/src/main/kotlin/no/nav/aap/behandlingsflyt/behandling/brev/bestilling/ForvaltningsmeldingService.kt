package no.nav.aap.behandlingsflyt.behandling.brev.bestilling

import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.ÅrsakTilBehandling
import no.nav.aap.komponenter.miljo.Miljø
import no.nav.aap.lookup.repository.RepositoryProvider

class ForvaltningsmeldingService(
    private val brevbestillingService: BrevbestillingService,
    private val behandlingRepository: BehandlingRepository,
) {

    constructor(repositoryProvider: RepositoryProvider) : this(
        BrevbestillingService(repositoryProvider),
        repositoryProvider.provide()
    )

    fun sendForvaltningsmeldingForNySøknad(behandlingId: BehandlingId) {
        val behandling = behandlingRepository.hent(behandlingId)

        if (Miljø.erDev() && // tester i dev først
            erBehandlingForMottattSøknad(behandling) &&
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

    private fun harAlleredeBestiltForvaltningsmeldingForBehandling(behandling: Behandling): Boolean {
        return brevbestillingService.hentBrevbestillinger(behandling.referanse)
            .any { it.typeBrev == TypeBrev.FORVALTNINGSMELDING }
    }

    private fun erBehandlingForMottattSøknad(behandling: Behandling): Boolean {
        when (behandling.typeBehandling()) {
            TypeBehandling.Førstegangsbehandling, TypeBehandling.Revurdering -> {
                return behandling.årsaker().any { it.type == ÅrsakTilBehandling.MOTTATT_SØKNAD }
            }

            else -> {
                return false
            }
        }
    }
}