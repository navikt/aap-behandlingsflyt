package no.nav.aap.behandlingsflyt.faktagrunnlag

import no.nav.aap.lookup.repository.RepositoryProvider

interface Informasjonskravkonstruktør {
    val navn: InformasjonskravNavn
    fun konstruer(repositoryProvider: RepositoryProvider): Informasjonskrav
}
