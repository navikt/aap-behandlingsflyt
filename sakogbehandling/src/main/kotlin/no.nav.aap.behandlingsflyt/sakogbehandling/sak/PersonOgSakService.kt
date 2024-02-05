package no.nav.aap.behandlingsflyt.sakogbehandling.sak

import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters.PersonService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.db.SakRepositoryImpl
import no.nav.aap.verdityper.Periode
import no.nav.aap.verdityper.sakogbehandling.Ident

class PersonOgSakService(private val connection: DBConnection) {

    suspend fun finnEllerOpprett(ident: Ident, periode: Periode) : Saksnummer {
        val person = PersonService.hentPerson(ident, connection)
        val sakRepository = SakRepositoryImpl(connection)

        return sakRepository.finnEllerOpprett(person, periode).saksnummer
    }
}