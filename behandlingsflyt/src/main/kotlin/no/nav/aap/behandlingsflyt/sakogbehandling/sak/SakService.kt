package no.nav.aap.behandlingsflyt.sakogbehandling.sak

import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.lookup.repository.RepositoryProvider

class SakService(private val sakRepository: SakRepository) {
    constructor(repositoryProvider: RepositoryProvider): this(
        sakRepository = repositoryProvider.provide()
    )

    fun hent(sakId: SakId): Sak {
        return sakRepository.hent(sakId)
    }

    fun hent(saksnummer: Saksnummer): Sak {
        return sakRepository.hent(saksnummer)
    }
}
