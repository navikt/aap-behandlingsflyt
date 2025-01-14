package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.personopplysning

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.Barn
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.Dødsdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Fødselsdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Personopplysning
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.PersonopplysningGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.PersonopplysningRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.RelatertPersonopplysning
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.RelatertePersonopplysninger
import no.nav.aap.behandlingsflyt.repository.sak.PersonRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.db.PersonRepository
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.Row
import no.nav.aap.lookup.repository.Factory

class PersonopplysningRepositoryImpl(
    private val connection: DBConnection, private val personRepository: PersonRepository
) : PersonopplysningRepository {

    companion object : Factory<PersonopplysningRepositoryImpl> {
        override fun konstruer(connection: DBConnection): PersonopplysningRepositoryImpl {
            return PersonopplysningRepositoryImpl(connection, PersonRepositoryImpl(connection))
        }
    }

    override fun hentHvisEksisterer(behandlingId: BehandlingId): PersonopplysningGrunnlag? {
        return connection.queryFirstOrNull(
            """
            SELECT g.bruker_personopplysning_id, g.personopplysninger_id
            FROM PERSONOPPLYSNING_GRUNNLAG g
            WHERE g.AKTIV AND g.BEHANDLING_ID = ?
            """.trimIndent()
        ) {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setRowMapper { row ->
                mapGrunnlag(row)
            }
        }
    }

    private fun mapGrunnlag(row: Row): PersonopplysningGrunnlag? {
        val brukerPersonopplysningId = row.getLongOrNull("bruker_personopplysning_id")
        if (brukerPersonopplysningId == null) {
            return null
        }
        return PersonopplysningGrunnlag(
            brukerPersonopplysning = hentBrukerPersonopplysninger(brukerPersonopplysningId),
            relatertePersonopplysninger = hentRelatertePersonopplysninger(row.getLongOrNull("personopplysninger_id"))
        )
    }

    private fun hentRelatertePersonopplysninger(id: Long?): RelatertePersonopplysninger? {
        if (id == null) {
            return null
        }

        return RelatertePersonopplysninger(
            id = id, personopplysninger = connection.queryList(
                """
            SELECT * FROM PERSONOPPLYSNING 
            WHERE personopplysninger_id = ?
        """.trimIndent()
            ) {
                setParams {
                    setLong(1, id)
                }
                setRowMapper {
                    RelatertPersonopplysning(
                        person = personRepository.hent(it.getLong("person_id")),
                        fødselsdato = Fødselsdato(it.getLocalDate("FODSELSDATO")),
                        dødsdato = it.getLocalDateOrNull("dodsdato")?.let { Dødsdato(it) })
                }
            })
    }

    private fun hentBrukerPersonopplysninger(id: Long): Personopplysning {
        return connection.queryFirst(
            """
            SELECT *
            FROM BRUKER_PERSONOPPLYSNING
            WHERE id = ?
            """.trimIndent()
        ) {
            setParams {
                setLong(1, id)
            }
            setRowMapper { row ->
                Personopplysning(
                    id = id,
                    fødselsdato = Fødselsdato(row.getLocalDate("FODSELSDATO")),
                    dødsdato = row.getLocalDateOrNull("dodsdato")?.let { Dødsdato(it) },
                    land = row.getString("LAND"),
                    gyldigFraOgMed = row.getLocalDateOrNull("GYLDIGFRAOGMED"),
                    gyldigTilOgMed = row.getLocalDateOrNull("GYLDIGTILOGMED"),
                    status = row.getEnum("STATUS")
                )
            }
        }
    }

    override fun lagre(behandlingId: BehandlingId, personopplysning: Personopplysning) {
        val personopplysningGrunnlag = hentHvisEksisterer(behandlingId)

        if (personopplysningGrunnlag?.brukerPersonopplysning == personopplysning) return

        if (personopplysningGrunnlag != null) {
            deaktiverEksisterende(behandlingId)
        }

        val personopplysningId =
            connection.executeReturnKey("INSERT INTO BRUKER_PERSONOPPLYSNING (FODSELSDATO, dodsdato, LAND, GYLDIGFRAOGMED, GYLDIGTILOGMED, STATUS) VALUES (?, ?, ?, ?, ?, ?)") {
                setParams {
                    setLocalDate(1, personopplysning.fødselsdato.toLocalDate())
                    setLocalDate(2, personopplysning.dødsdato?.toLocalDate())
                    setString(3, personopplysning.land)
                    setLocalDate(4, personopplysning.gyldigFraOgMed)
                    setLocalDate(5, personopplysning.gyldigTilOgMed)
                    setEnumName(6, personopplysning.status)
                }
            }

        connection.execute("INSERT INTO PERSONOPPLYSNING_GRUNNLAG (BEHANDLING_ID, bruker_personopplysning_id, personopplysninger_id) VALUES (?, ?, ?)") {
            setParams {
                setLong(1, behandlingId.toLong())
                setLong(2, personopplysningId)
                setLong(3, personopplysningGrunnlag?.relatertePersonopplysninger?.id)
            }
        }
    }

    override fun lagre(behandlingId: BehandlingId, barn: List<Barn>) {
        val personopplysningGrunnlag = hentHvisEksisterer(behandlingId)

        if (personopplysningGrunnlag != null) {
            deaktiverEksisterende(behandlingId)
        }

        val relatertePersonopplysningerId = if (barn.isEmpty()) {
            null
        } else {
            val personopplysningerId = connection.executeReturnKey(
                """
            INSERT INTO PERSONOPPLYSNINGER DEFAULT VALUES
        """.trimIndent()
            )
            connection.executeBatch(
                "INSERT INTO PERSONOPPLYSNING (person_id, personopplysninger_id, FODSELSDATO, dodsdato) VALUES (?, ?, ?, ?)",
                barn
            ) {
                setParams {
                    setLong(1, requireNotNull(personRepository.finn(it.ident)).id)
                    setLong(2, personopplysningerId)
                    setLocalDate(3, it.fødselsdato.toLocalDate())
                    setLocalDate(4, it.dødsdato?.toLocalDate())
                }
            }

            personopplysningerId
        }


        connection.execute("INSERT INTO PERSONOPPLYSNING_GRUNNLAG (BEHANDLING_ID, personopplysninger_id, bruker_personopplysning_id) VALUES (?, ?, ?)") {
            setParams {
                setLong(1, behandlingId.toLong())
                setLong(2, relatertePersonopplysningerId)
                setLong(3, personopplysningGrunnlag?.brukerPersonopplysning?.id)
            }
        }
    }

    private fun deaktiverEksisterende(behandlingId: BehandlingId) {
        connection.execute("UPDATE PERSONOPPLYSNING_GRUNNLAG SET AKTIV = FALSE WHERE AKTIV AND BEHANDLING_ID = ?") {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setResultValidator { rowsUpdated ->
                require(rowsUpdated == 1)
            }
        }
    }

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        require(fraBehandling != tilBehandling)
        connection.execute("INSERT INTO PERSONOPPLYSNING_GRUNNLAG (BEHANDLING_ID, bruker_personopplysning_id, personopplysninger_id) SELECT ?, bruker_personopplysning_id, personopplysninger_id FROM PERSONOPPLYSNING_GRUNNLAG WHERE AKTIV AND BEHANDLING_ID = ?") {
            setParams {
                setLong(1, tilBehandling.toLong())
                setLong(2, fraBehandling.toLong())
            }
        }
    }
}