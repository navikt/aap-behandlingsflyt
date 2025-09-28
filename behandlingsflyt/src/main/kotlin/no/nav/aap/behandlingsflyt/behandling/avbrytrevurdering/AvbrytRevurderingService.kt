package no.nav.aap.behandlingsflyt.behandling.avbrytrevurdering

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.komponenter.repository.RepositoryProvider

class AvbrytRevurderingService(
    private val avbrytRevurderingRepository: AvbrytRevurderingRepository,
    private val behandlingRepository: BehandlingRepository
) {
    constructor(repositoryProvider: RepositoryProvider) : this(
        avbrytRevurderingRepository = repositoryProvider.provide(),
        behandlingRepository = repositoryProvider.provide()
    )

    fun revurderingErAvbrutt(behandlingId: BehandlingId): Boolean {
        return avbrytRevurderingRepository.hentHvisEksisterer(behandlingId)?.vurdering != null;
    }

    fun hentBehandlingerMedAvbruttRevurderingForSak(sakId: SakId): List<Behandling> {
        val alleBehandlinger = behandlingRepository.hentAlleFor(sakId)
        val revurderingAvbruttRevurderingBehandlinger = alleBehandlinger.filter { revurderingErAvbrutt(it.id) }

        return revurderingAvbruttRevurderingBehandlinger
    }
}
