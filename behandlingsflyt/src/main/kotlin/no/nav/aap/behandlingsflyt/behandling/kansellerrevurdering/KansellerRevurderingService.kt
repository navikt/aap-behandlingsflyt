package no.nav.aap.behandlingsflyt.behandling.kansellerrevurdering

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.repository.RepositoryProvider

class KansellerRevurderingService (
    private val kansellerRevurderingRepository: KansellerRevurderingRepository
) {
    constructor(repositoryProvider: RepositoryProvider) : this(repositoryProvider.provide())

    fun revurderingErKansellert(behandlingId: BehandlingId): Boolean {
        return kansellerRevurderingRepository.hentHvisEksisterer(behandlingId)?.vurdering != null;
    }
}