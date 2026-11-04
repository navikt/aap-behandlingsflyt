package no.nav.aap.behandlingsflyt.behandling.egenskaper

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.KravRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider

data class BehandlingEgenskaper(
    val erMigrertFraArena: Boolean,
)

class BehandlingEgenskaperService(
    private val kravRepository: KravRepository,
) {
    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        kravRepository = repositoryProvider.provide(),
    )

    fun hentEgenskaper(behandlingId: BehandlingId): BehandlingEgenskaper {
        return BehandlingEgenskaper(
            erMigrertFraArena = erMigrertFraArena(behandlingId),
        )
    }

    private fun erMigrertFraArena(behandlingId: BehandlingId): Boolean {
        return kravRepository.hentHvisEksisterer(behandlingId)?.gjeldendeMigrertKrav() != null
    }
}
