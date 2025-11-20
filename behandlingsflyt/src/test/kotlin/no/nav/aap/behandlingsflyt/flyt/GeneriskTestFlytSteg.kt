package no.nav.aap.behandlingsflyt.flyt

import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider

class GeneriskTestFlytSteg(
    private val stegType: StegType,
    private val avklaringsbehovRekkefølge: List<Definisjon> = emptyList()
) : FlytSteg {
    override val rekkefølge: List<Definisjon> get() = avklaringsbehovRekkefølge

    override fun konstruer(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider): BehandlingSteg {
        return GeneriskTestSteg()
    }

    override fun type(): StegType {
        return stegType
    }
}
