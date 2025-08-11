package no.nav.aap.behandlingsflyt.flyt.steg.internal

import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.StegKonstruktør
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.repository.RepositoryProvider

class StegKonstruktørImpl(
    private val repositoryProvider: RepositoryProvider,
    private val gatewayProvider: GatewayProvider
) : StegKonstruktør {
    override fun konstruer(steg: FlytSteg): BehandlingSteg {
        return steg.konstruer(repositoryProvider, gatewayProvider)
    }
}