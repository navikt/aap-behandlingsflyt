package no.nav.aap.behandlingsflyt.behandling.trekkklage

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.lookup.repository.RepositoryProvider

class TrekkKlageService (
    private val trekkKlageRepository: TrekkKlageRepository,
) {
    constructor(repositoryProvider: RepositoryProvider) : this(
        trekkKlageRepository = repositoryProvider.provide(),
    )

    fun klageErTrukket(behandlingId: BehandlingId): Boolean {
        val grunnlag = trekkKlageRepository.hentTrekkKlageGrunnlag(behandlingId)
        return grunnlag?.vurdering?.skalTrekkes == true
    }
}