package no.nav.aap.behandlingsflyt.sakogbehandling.sak

import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.db.PersonRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.db.SakRepositoryImpl
import no.nav.aap.verdityper.Periode
import no.nav.aap.verdityper.sakogbehandling.Ident

class PersonOgSakService(
    private val connection: DBConnection,
    private val pdlGateway: PdlGateway
) {

    suspend fun finnEllerOpprett(ident: Ident, periode: Periode) : Sak {
        val identliste = pdlGateway.hentAlleIdenterForPerson(ident)

        val personRepository = PersonRepository(connection)
        val person = personRepository.finnEllerOpprett(identliste)

        val sakRepository = SakRepositoryImpl(connection)
        return sakRepository.finnEllerOpprett(person, periode)
    }
}