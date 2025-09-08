package no.nav.aap.behandlingsflyt.behandling.søknad

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.lookup.repository.RepositoryProvider

class TrukketSøknadService(
    private val trukketSøknadRepository: TrukketSøknadRepository
) {
    constructor(repositoryProvider: RepositoryProvider) : this(repositoryProvider.provide())

    fun søknadErTrukket(behandlingId: BehandlingId): Boolean {
        return trukketSøknadRepository.hentTrukketSøknadVurderinger(behandlingId).isNotEmpty()
    }
}