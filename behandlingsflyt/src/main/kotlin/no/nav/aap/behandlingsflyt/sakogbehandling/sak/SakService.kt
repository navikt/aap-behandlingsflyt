package no.nav.aap.behandlingsflyt.sakogbehandling.sak

import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.lookup.repository.RepositoryProvider

class SakService(private val sakRepository: SakRepository, private val behandlingRepository: BehandlingRepository) {
    constructor(repositoryProvider: RepositoryProvider) : this(
        sakRepository = repositoryProvider.provide(),
        behandlingRepository = repositoryProvider.provide()
    )

    fun hent(sakId: SakId): Sak {
        return sakRepository.hent(sakId)
    }

    fun hent(saksnummer: Saksnummer): Sak {
        return sakRepository.hent(saksnummer)
    }

    fun hentSakFor(behandlingId: BehandlingId): Sak {
        val behandling = behandlingRepository.hent(behandlingId)
        return sakRepository.hent(behandling.sakId)
    }
}
