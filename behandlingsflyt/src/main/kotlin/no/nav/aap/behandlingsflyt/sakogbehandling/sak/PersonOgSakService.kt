package no.nav.aap.behandlingsflyt.sakogbehandling.sak

import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.db.PersonRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.db.SakRepositoryImpl
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.type.Periode

class PersonOgSakService(
    private val connection: DBConnection,
    private val pdlGateway: IdentGateway
) {

    fun finnEllerOpprett(ident: Ident, periode: Periode): Sak {
        val identliste = pdlGateway.hentAlleIdenterForPerson(ident)
        if (identliste.isEmpty()) {
            throw IllegalStateException("Fikk ingen treff på ident i PDL")
        }

        val personRepository = PersonRepositoryImpl(connection)
        val person = personRepository.finnEllerOpprett(identliste)

        val sakRepository = SakRepositoryImpl(connection)
        return sakRepository.finnEllerOpprett(person, periode)
    }
}