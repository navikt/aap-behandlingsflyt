package no.nav.aap.behandlingsflyt.test

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.behandlingsflyt.hendelse.avløp.BehandlingHendelseService
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.komponenter.gateway.Factory

object DummyBehandlingHendelseService : BehandlingHendelseService {
    override fun stoppet(
        behandling: Behandling,
        avklaringsbehovene: Avklaringsbehovene
    ) {
        // noop — avoids scheduling oppgave/statistikk/datadeling motor jobs in tests
    }
}

/**
 * Factory wrapper so DummyBehandlingHendelseService can be registered in GatewayProvider.
 * Usage: `register<DummyBehandlingHendelseServiceFactory>()`
 */
class DummyBehandlingHendelseServiceFactory : BehandlingHendelseService by DummyBehandlingHendelseService {
    companion object : Factory<BehandlingHendelseService> {
        override fun konstruer(): BehandlingHendelseService = DummyBehandlingHendelseService
    }
}


