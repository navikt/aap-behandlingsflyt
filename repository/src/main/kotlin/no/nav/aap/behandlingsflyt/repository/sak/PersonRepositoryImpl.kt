package no.nav.aap.behandlingsflyt.repository.sak

import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.db.PersonRepository
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.Row
import no.nav.aap.lookup.repository.Factory
import java.util.*

class PersonRepositoryImpl(private val connection: DBConnection) : PersonRepository {

    companion object : Factory<PersonRepository> {
        override fun konstruer(connection: DBConnection): PersonRepository {
            return PersonRepositoryImpl(connection)
        }
    }

    override fun finnEllerOpprett(identer: List<Ident>): Person {
        require(identer.isNotEmpty())

        val relevantePersoner = connection.queryList(
            """SELECT DISTINCT person.id, person.referanse
                        FROM person
                        INNER JOIN person_ident ON person_ident.person_id = person.id
                        WHERE person_ident.ident = ANY(?::text[])"""
        ) {
            setParams {
                setArray(1, identer.map { it.identifikator })
            }
            setRowMapper(::mapPerson)
        }
        return if (relevantePersoner.isNotEmpty()) {
            if (relevantePersoner.size > 1) {
                throw IllegalStateException("Har flere personer knyttet til denne identen")
            }
            val person = relevantePersoner.first()
            oppdater(person, identer)
            // Henter på nytt etter oppdatering
            hent(person.id)
        } else {
            opprettPerson(identer)
        }
    }

    private fun oppdater(person: Person, identer: List<Ident>) {
        require(identer.filter { it.aktivIdent }.size < 2)

        val oppdaterteIdenter = identer.filterNot { ident -> person.identer().contains(ident) }

        if (oppdaterteIdenter.isEmpty()) {
            return
        }

        if (oppdaterteIdenter.any { ident -> ident.aktivIdent }) {
            connection.execute(
                """
                UPDATE person_ident SET primaer = false WHERE person_id = ?
            """.trimIndent()
            ) {
                setParams {
                    setLong(1, person.id)
                }
            }
        }

        leggTilNyeIdenterPåPerson(oppdaterteIdenter, person)

        if (harKunEndretPrimær(person, oppdaterteIdenter)) {
            val nyPrimær = oppdaterteIdenter.single { it.aktivIdent }
            connection.execute(
                """
                UPDATE person_ident SET primaer = true WHERE person_id = ? and ident = ?
            """.trimIndent()
            ) {
                setParams {
                    setLong(1, person.id)
                    setString(2, nyPrimær.identifikator)
                }
            }
        }
    }

    private fun leggTilNyeIdenterPåPerson(
        oppdaterteIdenter: List<Ident>,
        person: Person
    ) {
        val nyeIdenter = oppdaterteIdenter.filter { ident ->
            person.identer().none { id -> id.identifikator == ident.identifikator }
        }
        if (nyeIdenter.isNotEmpty()) {
            connection.executeBatch(
                """INSERT INTO PERSON_IDENT (ident, primaer, person_id) VALUES (?, ?, ?)""", nyeIdenter
            ) {
                setParams { ident ->
                    setString(1, ident.identifikator)
                    setBoolean(2, ident.aktivIdent)
                    setLong(3, person.id)
                }
            }
        }
    }

    private fun harKunEndretPrimær(person: Person, oppdaterteIdenter: List<Ident>): Boolean {
        if (oppdaterteIdenter.none { ident -> ident.aktivIdent }) {
            return false
        }
        val nyPrimær = oppdaterteIdenter.single { it.aktivIdent }

        return person.aktivIdent().identifikator != nyPrimær.identifikator
    }

    private fun mapPerson(row: Row): Person {
        val personId = row.getLong("id")
        return Person(personId, row.getUUID("referanse"), hentIdenter(personId))
    }


    override fun hent(personId: Long): Person {
        return connection.queryFirst("SELECT referanse FROM PERSON WHERE id = ?") {
            setParams {
                setLong(1, personId)
            }
            setRowMapper { row ->
                val identer = hentIdenter(personId)
                Person(personId, row.getUUID("referanse"), identer)
            }
        }
    }

    private fun hentIdenter(personId: Long): List<Ident> {
        return connection.queryList("SELECT ident, primaer FROM PERSON_IDENT WHERE person_id = ?") {
            setParams {
                setLong(1, personId)
            }
            setRowMapper { row ->
                Ident(row.getString("ident"), row.getBoolean("primaer"))
            }
        }
    }

    private fun opprettPerson(identer: List<Ident>): Person {
        val identifikator = UUID.randomUUID()
        val personId = connection.executeReturnKey(
            """INSERT INTO PERSON (referanse) VALUES (?)"""
        ) {
            setParams {
                setUUID(1, identifikator)
            }
        }
        connection.executeBatch(
            """INSERT INTO PERSON_IDENT (ident, primaer, person_id) VALUES (?, ?, ?)""", identer
        ) {
            setParams { ident ->
                setString(1, ident.identifikator)
                setBoolean(2, ident.aktivIdent)
                setLong(3, personId)
            }
        }

        return Person(personId, identifikator, identer)
    }

    override fun finn(ident: Ident): Person? {
        return connection.queryFirstOrNull(
            """SELECT DISTINCT p.id, p.referanse FROM PERSON p INNER JOIN PERSON_IDENT pi ON pi.person_id = p.id WHERE pi.ident = ?"""
        ) {
            setParams {
                setString(1, ident.identifikator)
            }
            setRowMapper { row ->
                mapPerson(row)
            }
        }
    }

    override fun slett(behandlingId: BehandlingId) {
        // Ikke relevant for å slette sak
    }

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        // Denne trengs ikke implementeres
    }
}
