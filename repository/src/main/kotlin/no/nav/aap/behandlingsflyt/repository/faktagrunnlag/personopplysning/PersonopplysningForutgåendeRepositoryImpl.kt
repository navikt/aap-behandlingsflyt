package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.personopplysning

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.Dødsdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Fødselsdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Personopplysning
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.PersonopplysningForutgåendeRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.PersonopplysningGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.RelatertPersonopplysning
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.RelatertePersonopplysninger
import no.nav.aap.behandlingsflyt.repository.sak.PersonRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.db.PersonRepository
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.Row
import no.nav.aap.lookup.repository.Factory

class PersonopplysningForutgåendeRepositoryImpl(
    private val connection: DBConnection, private val personRepository: PersonRepository
) : PersonopplysningForutgåendeRepository {

    companion object : Factory<PersonopplysningForutgåendeRepositoryImpl> {
        override fun konstruer(connection: DBConnection): PersonopplysningForutgåendeRepositoryImpl {
            return PersonopplysningForutgåendeRepositoryImpl(connection, PersonRepositoryImpl(connection))
        }
    }

    override fun hentHvisEksisterer(behandlingId: BehandlingId): PersonopplysningGrunnlag? {
        return connection.queryFirstOrNull(
            """
            SELECT g.bruker_personopplysning_id, g.personopplysninger_id
            FROM PERSONOPPLYSNING_FORUTGAAENDE_GRUNNLAG g
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
        val brukerPersonopplysningId = row.getLongOrNull("BRUKER_PERSONOPPLYSNING_ID")
        if (brukerPersonopplysningId == null) {
            return null
        }
        return PersonopplysningGrunnlag(
            brukerPersonopplysning = hentBrukerPersonopplysninger(brukerPersonopplysningId),
            relatertePersonopplysninger = hentRelatertePersonopplysninger(row.getLongOrNull("PERSONOPPLYSNINGER_ID"))
        )
    }

    private fun hentRelatertePersonopplysninger(id: Long?): RelatertePersonopplysninger? {
        if (id == null) {
            return null
        }

        return RelatertePersonopplysninger(
            id = id, personopplysninger = connection.queryList(
                """
            SELECT * FROM PERSONOPPLYSNING_FORUTGAAENDE 
            WHERE PERSONOPPLYSNINGER_ID = ?
        """.trimIndent()
            ) {
                setParams {
                    setLong(1, id)
                }
                setRowMapper {
                    RelatertPersonopplysning(
                        person = personRepository.hent(it.getLong("PERSON_ID")),
                        fødselsdato = Fødselsdato(it.getLocalDate("FODSELSDATO")),
                        dødsdato = it.getLocalDateOrNull("DODSDATO")?.let { Dødsdato(it) })
                }
            })
    }

    private fun hentBrukerPersonopplysninger(id: Long): Personopplysning {
        return connection.queryFirst(
            """
            SELECT *
            FROM BRUKER_PERSONOPPLYSNING_FORUTGAAENDE
            WHERE ID = ?
            """.trimIndent()
        ) {
            setParams {
                setLong(1, id)
            }
            setRowMapper { row ->
                Personopplysning(
                    id = id,
                    fødselsdato = Fødselsdato(row.getLocalDate("FODSELSDATO")),
                    dødsdato = row.getLocalDateOrNull("DODSDATO")?.let { Dødsdato(it) },
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
            connection.executeReturnKey("INSERT INTO BRUKER_PERSONOPPLYSNING_FORUTGAAENDE (FODSELSDATO, DODSDATO, LAND, GYLDIGFRAOGMED, GYLDIGTILOGMED, STATUS) VALUES (?, ?, ?, ?, ?, ?)") {
                setParams {
                    setLocalDate(1, personopplysning.fødselsdato.toLocalDate())
                    setLocalDate(2, personopplysning.dødsdato?.toLocalDate())
                    setString(3, personopplysning.land)
                    setLocalDate(4, personopplysning.gyldigFraOgMed)
                    setLocalDate(5, personopplysning.gyldigTilOgMed)
                    setEnumName(6, personopplysning.status)
                }
            }

        connection.execute("INSERT INTO PERSONOPPLYSNING_FORUTGAAENDE_GRUNNLAG (BEHANDLING_ID, BRUKER_PERSONOPPLYSNING_ID, PERSONOPPLYSNINGER_ID) VALUES (?, ?, ?)") {
            setParams {
                setLong(1, behandlingId.toLong())
                setLong(2, personopplysningId)
                setLong(3, personopplysningGrunnlag?.relatertePersonopplysninger?.id)
            }
        }
    }

    private fun deaktiverEksisterende(behandlingId: BehandlingId) {
        connection.execute("UPDATE PERSONOPPLYSNING_FORUTGAAENDE_GRUNNLAG SET AKTIV = FALSE WHERE AKTIV AND BEHANDLING_ID = ?") {
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
        connection.execute("""
            INSERT INTO PERSONOPPLYSNING_FORUTGAAENDE_GRUNNLAG (BEHANDLING_ID, BRUKER_PERSONOPPLYSNING_ID, PERSONOPPLYSNINGER_ID) 
            SELECT ?, BRUKER_PERSONOPPLYSNING_ID, PERSONOPPLYSNINGER_ID FROM PERSONOPPLYSNING_FORUTGAAENDE_GRUNNLAG WHERE AKTIV AND BEHANDLING_ID = ?
        """.trimIndent()
        ) {
            setParams {
                setLong(1, tilBehandling.toLong())
                setLong(2, fraBehandling.toLong())
            }
        }
    }
}