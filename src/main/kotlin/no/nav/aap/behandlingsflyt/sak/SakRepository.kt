package no.nav.aap.behandlingsflyt.sak

import no.nav.aap.behandlingsflyt.Periode
import no.nav.aap.behandlingsflyt.dbstuff.DBConnection
import no.nav.aap.behandlingsflyt.dbstuff.Row
import java.util.*

class SakRepository(private val connection: DBConnection) {

    private val personRepository = PersonRepository(connection)

    fun hent(sakId: Long): Sak {
        return connection.prepareFirstQueryStatement("SELECT id, saksnummer, person_id, rettighetsperiode, status FROM SAK WHERE id = ?") {
            setParams {
                setLong(1, sakId)
            }
            setRowMapper { row ->
                mapSak(row)
            }
        }
    }

    fun hent(saksnummer: Saksnummer): Sak {
        return connection.prepareFirstQueryStatement("SELECT id, saksnummer, person_id, rettighetsperiode, status FROM SAK WHERE saksnummer = ?") {
            setParams {
                setString(1, saksnummer.toString())
            }
            setRowMapper { row ->
                mapSak(row)
            }
        }
    }

    private fun genererSaksnummer(id: Long): Saksnummer {
        return Saksnummer(
            (id * 1000).toString(36)
                .uppercase(Locale.getDefault())
                .replace("O", "o")
                .replace("I", "i")
        )
    }

    private fun opprett(person: Person, periode: Periode): Sak {
        val sakId = connection.prepareFirstQueryStatement("SELECT nextval('SEQ_SAKSNUMMER') as nextval") {
            setRowMapper { row ->
                row.getLong("nextval")
            }
        }
        val saksnummer = genererSaksnummer(sakId)
        val keys = connection.prepareExecuteStatementReturnAutoGenKeys(
            "INSERT INTO " +
                    "SAK (saksnummer, person_id, rettighetsperiode, status) " +
                    "VALUES (?, ?, ?::daterange, ?)"
        ) {
            setParams {
                setString(1, saksnummer.toString())
                setLong(2, person.id)
                setDateRange(3, periode)
                setString(4, Status.OPPRETTET.name)
            }
        }
        return Sak(keys.first(), saksnummer, person, periode)
    }

    fun finnEllerOpprett(person: Person, periode: Periode): Sak {
        val relevantesaker = connection.prepareListQueryStatement(
            "SELECT id, saksnummer, person_id, rettighetsperiode, status " +
                    "FROM SAK " +
                    "WHERE person_id = ? AND rettighetsperiode && ?::daterange"
        ) {
            setParams {
                setLong(1, person.id)
                setDateRange(2, periode)
            }
            setRowMapper { row ->
                mapSak(row)
            }
        }
        if (relevantesaker.isEmpty()) {
            return opprett(person, periode)
        }

        if (relevantesaker.size != 1) {
            throw IllegalStateException("Fant flere saker som er relevant: $relevantesaker")
        }
        return relevantesaker.first()
    }

    fun finnSakerFor(person: Person): List<Sak> {
        return connection.prepareListQueryStatement(
            "SELECT id, saksnummer, person_id, rettighetsperiode, status " +
                    "FROM SAK " +
                    "WHERE person_id = ?"
        ) {
            setParams {
                setLong(1, person.id)
            }
            setRowMapper { row ->
                mapSak(row)
            }
        }
    }

    private fun mapSak(row: Row) = Sak(
        id = row.getLong("id"),
        person = personRepository.hent(row.getLong("person_id")),
        rettighetsperiode = row.getDateRange("rettighetsperiode"),
        saksnummer = Saksnummer(row.getString("saksnummer")),
        status = Status.valueOf(row.getString("status"))
    )

    fun finnAlle(): List<Sak> {
        return connection.prepareListQueryStatement(
            "SELECT id, saksnummer, person_id, rettighetsperiode, status " +
                    "FROM SAK"
        ) {
            setRowMapper { row ->
                mapSak(row)
            }
        }
    }
}
