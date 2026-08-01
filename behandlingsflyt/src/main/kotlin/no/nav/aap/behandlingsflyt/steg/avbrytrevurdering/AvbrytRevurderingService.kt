package no.nav.aap.behandlingsflyt.steg.avbrytrevurdering

import no.nav.aap.behandling.BehandlingId
import no.nav.aap.komponenter.repository.RepositoryProvider

class AvbrytRevurderingService (
    private val avbrytRevurderingRepository: AvbrytRevurderingRepository
) {
    constructor(repositoryProvider: RepositoryProvider) : this(repositoryProvider.provide())

    fun revurderingErAvbrutt(behandlingId: BehandlingId): Boolean {
        return avbrytRevurderingRepository.hentHvisEksisterer(behandlingId)?.vurdering != null
    }
}