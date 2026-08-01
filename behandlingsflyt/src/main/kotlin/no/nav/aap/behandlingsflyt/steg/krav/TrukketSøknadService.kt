package no.nav.aap.behandlingsflyt.steg.krav

import no.nav.aap.behandling.BehandlingId
import no.nav.aap.lookup.repository.RepositoryProvider

class TrukketSøknadService(
    private val trukketSøknadRepository: TrukketSøknadRepository
) {
    constructor(repositoryProvider: RepositoryProvider) : this(repositoryProvider.provide())

    fun søknadErTrukket(behandlingId: BehandlingId): Boolean {
        return trukketSøknadRepository.hentTrukketSøknadVurderinger(behandlingId).any { it.skalTrekkes }
    }
}