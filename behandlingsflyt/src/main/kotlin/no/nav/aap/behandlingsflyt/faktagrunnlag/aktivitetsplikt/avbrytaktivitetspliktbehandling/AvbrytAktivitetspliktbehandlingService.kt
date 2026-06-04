package no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.avbrytaktivitetspliktbehandling

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.repository.RepositoryProvider

class AvbrytAktivitetspliktbehandlingService(
    private val avbrytAktivitetspliktbehandlingRepository: AvbrytAktivitetspliktbehandlingRepository
) {
    constructor(repositoryProvider: RepositoryProvider) : this(repositoryProvider.provide())

    fun behandlingErAvbrutt(behandlingId: BehandlingId): Boolean {
        return avbrytAktivitetspliktbehandlingRepository.hentHvisEksisterer(behandlingId)?.vurdering != null
    }
}