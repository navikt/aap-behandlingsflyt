package no.nav.aap.behandlingsflyt.hendelse.avløp

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.komponenter.gateway.Gateway
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider

interface BehandlingHendelseService : Gateway {
    fun stoppet(behandling: Behandling, avklaringsbehovene: Avklaringsbehovene)

    companion object {
        /**
         * Resolves from GatewayProvider if registered (e.g. DummyBehandlingHendelseService in tests),
         * otherwise creates BehandlingHendelseServiceImpl.
         */
        fun resolve(gatewayProvider: GatewayProvider, repositoryProvider: RepositoryProvider): BehandlingHendelseService =
            try {
                gatewayProvider.provide()
            } catch (_: Exception) {
                BehandlingHendelseServiceImpl(repositoryProvider, gatewayProvider)
            }
    }
}