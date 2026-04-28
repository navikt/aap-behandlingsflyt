package no.nav.aap.behandlingsflyt.test

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.behandlingsflyt.hendelse.avløp.BehandlingHendelseService
import no.nav.aap.behandlingsflyt.hendelse.avløp.BehandlingHendelseServiceProvider
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.komponenter.gateway.Factory
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider

object DummyBehandlingHendelseService : BehandlingHendelseService {
    override fun stoppet(
        behandling: Behandling,
        avklaringsbehovene: Avklaringsbehovene
    ) {
        // noop
    }
}

class DummyBehandlingHendelseServiceFactory : BehandlingHendelseServiceProvider {
    override fun create(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider): BehandlingHendelseService {
        return DummyBehandlingHendelseService
    }

    companion object : Factory<BehandlingHendelseServiceProvider> {
        override fun konstruer(): BehandlingHendelseServiceProvider = DummyBehandlingHendelseServiceFactory()
    }
}
