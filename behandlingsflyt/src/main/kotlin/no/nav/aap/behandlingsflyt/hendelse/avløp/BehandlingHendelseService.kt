package no.nav.aap.behandlingsflyt.hendelse.avløp

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.komponenter.gateway.Gateway
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider

interface BehandlingHendelseService {
    fun stoppet(behandling: Behandling, avklaringsbehovene: Avklaringsbehovene)
}

/**
 * Factory for GatewayProvider for å kunne ha ulik implementasjon av BehandlingHendelseService i tester og kjørbar kode.
 */
interface BehandlingHendelseServiceProvider : Gateway {
    fun create(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider): BehandlingHendelseService
}