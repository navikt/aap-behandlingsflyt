package no.nav.aap.behandlingsflyt.sak

import no.nav.aap.behandlingsflyt.dbstuff.DbConnection
import no.nav.aap.behandlingsflyt.dbstuff.Row
import java.util.*

class PersonRepository(private val connection: DbConnection) {

    fun finnEllerOpprett(ident: Ident): Person {
        val relevantePersoner = connection.prepareListQueryStatement(
            """SELECT person.id, person.referanse 
                    FROM person 
                    INNER JOIN person_ident ON person_ident.person_id = person.id 
                    WHERE person_ident.ident = ?"""
        ) {
            setParams {
                setString(1, ident.identifikator)
            }
            setRowMapper { row ->
                mapPerson(row)
            }
        }
        return if (relevantePersoner.isNotEmpty()) {
            if (relevantePersoner.size > 1) {
                throw IllegalStateException("Har flere personer knyttet til denne identen")
            }
            relevantePersoner.first()
        } else {
            opprettPerson(ident)
        }
    }

    fun hent(identifikator: UUID): Person {
        return connection.prepareFirstQueryStatement("SELECT id, referanse FROM PERSON WHERE referanse = ?") {
            setParams {
                setUUID(1, identifikator)
            }
            setRowMapper { row ->
                mapPerson(row)
            }
        }
    }

    private fun mapPerson(row: Row): Person {
        val personId = row.getLong("id")
        return Person(personId, row.getUUID("referanse"), hentIdenter(personId))
    }


    fun hent(personId: Long): Person {
        return connection.prepareFirstQueryStatement("SELECT referanse FROM PERSON WHERE id = ?") {
            setParams {
                setLong(1, personId)
            }
            setRowMapper { row ->
                Person(personId, row.getUUID("referanse"), hentIdenter(personId))
            }
        }
    }

    private fun hentIdenter(personId: Long): List<Ident> {
        return connection.prepareListQueryStatement("SELECT ident FROM PERSON_IDENT WHERE person_id = ?") {
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
        val personId = connection.prepareExecuteStatementReturnAutoGenKeys(
            "INSERT INTO " +
                    "PERSON (referanse) " +
                    "VALUES (?)"
        ) {
            setParams {
                setUUID(1, identifikator)
            }
        }.first()
        connection.prepareExecuteStatement(
            "INSERT INTO " +
                    "PERSON_IDENT (ident, person_id) " +
                    "VALUES (?, ?)"
        ) {
            setParams {
                setString(1, ident.identifikator)
                setLong(2, personId)
            }
        }

        return Person(personId, identifikator, listOf(ident))
    }

    fun finn(ident: Ident): Person? {
        return connection.prepareFirstOrNullQueryStatement(
            "SELECT unique p.id, p.referanse " +
                    "FROM PERSON p " +
                    "INNER JOIN PERSON_IDENT pi ON pi.person_id = p.id" +
                    "WHERE pi.ident = ?"
        ) {
            setParams {
                setString(1, ident.identifikator)
            }
            setRowMapper { row ->
                mapPerson(row)
            }
        }
    }
}
