package no.nav.aap.behandlingsflyt.sak

import no.nav.aap.behandlingsflyt.verdityper.Periode
import no.nav.aap.behandlingsflyt.dbconnect.DBConnection

interface SakRepository {

    fun finnEllerOpprett(person: Person, periode: Periode): Sak

    fun finnSakerFor(person: Person): List<Sak>

    fun finnAlle(): List<Sak>

    fun hent(sakId: SakId): Sak

    fun hent(saksnummer: Saksnummer): Sak

}

fun sakRepository(connection: DBConnection): SakRepository {
    return SakRepositoryImpl(connection)
}