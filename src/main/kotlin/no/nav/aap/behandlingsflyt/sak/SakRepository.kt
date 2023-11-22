package no.nav.aap.behandlingsflyt.sak

import no.nav.aap.behandlingsflyt.Periode
import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.dbconnect.Row

class SakRepository(private val connection: DBConnection) {

    private val personRepository = PersonRepository(connection)

    fun hent(sakId: SakId): Sak {
        return connection.queryFirst("SELECT id, saksnummer, person_id, rettighetsperiode, status FROM SAK WHERE id = ?") {
            setParams {
                setLong(1, sakId.toLong())
            }
            setRowMapper { row ->
                mapSak(row)
            }
        }
    }

    fun hent(saksnummer: Saksnummer): Sak {
        return connection.queryFirst("SELECT id, saksnummer, person_id, rettighetsperiode, status FROM SAK WHERE saksnummer = ?") {
            setParams {
                setString(1, saksnummer.toString())
            }
            setRowMapper { row ->
                mapSak(row)
            }
        }
    }

    private fun opprett(person: Person, periode: Periode): Sak {
        val sakId = connection.queryFirst("SELECT nextval('SEQ_SAKSNUMMER') as nextval") {
            setRowMapper { row ->
                row.getLong("nextval")
            }
        }
        val saksnummer = Saksnummer.valueOf(sakId)
        val keys = connection.executeReturnKey(
            "INSERT INTO " +
                    "SAK (saksnummer, person_id, rettighetsperiode, status) " +
                    "VALUES (?, ?, ?::daterange, ?)"
        ) {
            setParams {
                setString(1, saksnummer.toString())
                setLong(2, person.id)
                setPeriode(3, periode)
                setEnumName(4, Status.OPPRETTET)
            }
        }
        return Sak(SakId(keys), saksnummer, person, periode)
    }

    fun finnEllerOpprett(person: Person, periode: Periode): Sak {
        val relevantesaker = connection.queryList(
            "SELECT id, saksnummer, person_id, rettighetsperiode, status " +
                    "FROM SAK " +
                    "WHERE person_id = ? AND rettighetsperiode && ?::daterange"
        ) {
            setParams {
                setLong(1, person.id)
                setPeriode(2, periode)
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


    fun oppdaterSakStatus(sakId: SakId, status: Status) {
        val query = """UPDATE sak SET status = ? WHERE ID = ?"""

        return connection.execute(query) {
            setParams {
                setEnumName(1, status)
                setLong(2, sakId.toLong())
            }
            setResultValidator {
                require(it == 1)
            }
        }
    }

    fun finnSakerFor(person: Person): List<Sak> {
        return connection.queryList(
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

    fun finnAlle(): List<Sak> {
        return connection.queryList(
            "SELECT id, saksnummer, person_id, rettighetsperiode, status " +
                    "FROM SAK"
        ) {
            setRowMapper { row ->
                mapSak(row)
            }
        }
    }

    private fun mapSak(row: Row) = Sak(
        id = SakId(row.getLong("id")),
        person = personRepository.hent(row.getLong("person_id")),
        rettighetsperiode = row.getPeriode("rettighetsperiode"),
        saksnummer = Saksnummer(row.getString("saksnummer")),
        status = Status.valueOf(row.getString("status"))
    )
}
