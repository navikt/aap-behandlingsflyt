package no.nav.aap.behandlingsflyt.sakogbehandling.sak

import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.db.PersonRepository
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.lookup.repository.RepositoryProvider

class PersonOgSakService(
    private val pdlGateway: IdentGateway,
    private val personRepository: PersonRepository,
    private val sakRepository: SakRepository,
) {


    constructor(repositoryProvider: RepositoryProvider): this (
        pdlGateway =  GatewayProvider.provide(IdentGateway::class),
        personRepository = repositoryProvider.provide(),
        sakRepository = repositoryProvider.provide(),
    )

    fun finnEllerOpprett(ident: Ident, periode: Periode): Sak {
        val identliste = pdlGateway.hentAlleIdenterForPerson(ident)
        if (identliste.isEmpty()) {
            throw IllegalStateException("Fikk ingen treff på ident i PDL")
        }

        val person = personRepository.finnEllerOpprett(identliste)

        return sakRepository.finnEllerOpprett(person, periode)
    }
}