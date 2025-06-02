package no.nav.aap.behandlingsflyt.sakogbehandling.sak

import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.db.PersonRepository
import no.nav.aap.komponenter.type.Periode

class PersonOgSakService(
    private val pdlGateway: IdentGateway,
    private val personRepository: PersonRepository,
    private val sakRepository: SakRepository,
) {

    fun finnEllerOpprett(ident: Ident, periode: Periode): Sak {
        val identliste = pdlGateway.hentAlleIdenterForPerson(ident)
        if (identliste.isEmpty()) {
            throw IllegalStateException("Fikk ingen treff på ident i PDL")
        }

        val person = personRepository.finnEllerOpprett(identliste)

        return sakRepository.finnEllerOpprett(person, periode)
    }
}