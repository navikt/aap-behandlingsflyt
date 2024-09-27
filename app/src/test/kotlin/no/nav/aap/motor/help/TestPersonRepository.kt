package no.nav.aap.motor.help

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.Row
import no.nav.aap.verdityper.sakogbehandling.Ident
import java.util.*

class TestPersonRepository(private val connection: DBConnection) {

    fun finnEllerOpprett(identer: List<Ident>): Person {
        require(identer.isNotEmpty())

        val relevantePersoner = connection.queryList(
            """SELECT person.id, person.referanse 
                    FROM person 
                    INNER JOIN person_ident ON person_ident.person_id = person.id 
                    WHERE person_ident.ident = ANY(?::text[])"""
        ) {
            setParams {
                setArray(1, identer.map(Ident::identifikator))
            }
            setRowMapper(::mapPerson)
        }
        return if (relevantePersoner.isNotEmpty()) {
            if (relevantePersoner.size > 1) {
                throw IllegalStateException("Har flere personer knyttet til denne identen")
            }
            relevantePersoner.first()
        } else {
            opprettPerson(identer.single { it.aktivIdent })
        }
    }

    private fun mapPerson(row: Row): Person {
        val personId = row.getLong("id")
        return Person(personId, row.getUUID("referanse"), hentIdenter(personId))
    }

    private fun hentIdenter(personId: Long): List<Ident> {
        return connection.queryList("SELECT ident FROM PERSON_IDENT WHERE person_id = ?") {
            setParams {
                setLong(1, personId)
            }
            setRowMapper { row ->
                Ident(row.getString("ident"))
            }
        }
    }

    private fun opprettPerson(ident: Ident): Person {
        val identifikator = UUID.randomUUID()
        val personId = connection.executeReturnKey(
            "INSERT INTO " +
                    "PERSON (referanse) " +
                    "VALUES (?)"
        ) {
            setParams {
                setUUID(1, identifikator)
            }
        }
        connection.execute(
            "INSERT INTO " +
                    "PERSON_IDENT (ident, primaer, person_id) " +
                    "VALUES (?, ?, ?)"
        ) {
            setParams {
                setString(1, ident.identifikator)
                setBoolean(2, true)
                setLong(3, personId)
            }
        }

        return Person(personId, identifikator, listOf(ident))
    }
}
