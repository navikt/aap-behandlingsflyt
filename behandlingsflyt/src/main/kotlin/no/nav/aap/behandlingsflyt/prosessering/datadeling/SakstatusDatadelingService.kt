package no.nav.aap.behandlingsflyt.prosessering.datadeling

import no.nav.aap.behandlingsflyt.datadeling.SakStatus
import no.nav.aap.behandlingsflyt.datadeling.SakStatus.DatadelingBehandlingStatus
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingService
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider

class SakstatusDatadelingService(
    private val behandlingRepository: BehandlingRepository,
    private val sakRepository: SakRepository,
    private val behandlingService: BehandlingService,
) {
    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        repositoryProvider.provide(),
        repositoryProvider.provide(),
        BehandlingService(repositoryProvider, gatewayProvider)
    )

    fun utledSakstatus(referanse: BehandlingReferanse): SakStatus {
        val behandling = behandlingRepository.hent(referanse)
        val sak = sakRepository.hent(behandling.sakId)

        val sisteYtelseBehandling =
            requireNotNull(behandlingService.finnSisteYtelsesbehandlingFor(sak.id)) { "Fant ingen ytelsesbehandling for sak ${sak.id}" }

        val datadelingBehandlingStatus = when {
            sisteYtelseBehandling.status()
                .erÅpen() && sisteYtelseBehandling.vurderingsbehov()
                .find { it.type == Vurderingsbehov.MOTTATT_SØKNAD } != null -> DatadelingBehandlingStatus.SOKNAD_UNDER_BEHANDLING

            sisteYtelseBehandling.status().erÅpen() -> DatadelingBehandlingStatus.REVURDERING_UNDER_BEHANDLING

            else -> DatadelingBehandlingStatus.FERDIGBEHANDLET
        }


        return SakStatus.fromKelvin(
            sak.saksnummer.toString(),
            sak.status(),
            datadelingBehandlingStatus,
            sak.rettighetsperiode
        )
    }
}