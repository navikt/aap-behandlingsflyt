package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.personopplysning

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.Barn
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.Dødsdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Fødselsdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Personopplysning
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.PersonopplysningGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.PersonopplysningRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.RelatertPersonopplysning
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.RelatertePersonopplysninger
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Statsborgerskap
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.UtenlandsAdresse
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
                    statsborgerskap = hentStatsborgerskap(row),
                    status = row.getEnum("STATUS"),
                    utenlandsAddresser = hentUtenlandsAdresser((row.getLongOrNull("UTENLANDSADRESSER_ID")))
                )
            }
        }
    }

    private fun hentStatsborgerskap(row: Row): List<Statsborgerskap> {
        val gyldigFraOgMedDeprecated = row.getLocalDateOrNull("GYLDIGFRAOGMED")
        val gyldigTilOgMedDeprecated = row.getLocalDateOrNull("GYLDIGTILOGMED")
        val landDeprecated = row.getString("LAND")

        val landKoderId = row.getLongOrNull("LANDKODER_ID") ?: return listOf(Statsborgerskap(
            land = landDeprecated,
            gyldigFraOgMed = gyldigFraOgMedDeprecated,
            gyldigTilOgMed = gyldigTilOgMedDeprecated,
        ))

        return connection.queryList(
            """
                SELECT * FROM BRUKER_LAND
                WHERE LANDKODER_ID = ?
            """.trimIndent()
        ) {
            setParams {
                setLong(1, landKoderId)
            }
            setRowMapper { row ->
                Statsborgerskap(
                    land = row.getString("LAND"),
                    gyldigFraOgMed = row.getLocalDateOrNull("GYLDIGFRAOGMED"),
                    gyldigTilOgMed = row.getLocalDateOrNull("GYLDIGTILOGMED"),
                )
            }
        }
    }

    private fun hentUtenlandsAdresser(id: Long?): List<UtenlandsAdresse> {
        if (id == null) return emptyList()
        return connection.queryList(
            """
            SELECT * FROM BRUKER_UTENLANDSADRESSE
            WHERE UTENLANDSADRESSER_ID = ?
        """.trimIndent()
        ) {
            setParams {
                setLong(1, id)
            }
            setRowMapper { row ->
                UtenlandsAdresse(
                    gyldigFraOgMed = row.getLocalDateTimeOrNull("GYLDIGFRAOGMED"),
                    gyldigTilOgMed = row.getLocalDateTimeOrNull("GYLDIGTILOGMED"),
                    adresseNavn = row.getStringOrNull("ADRESSENAVN"),
                    postkode = row.getStringOrNull("POSTKODE"),
                    bySted = row.getStringOrNull("BYSTED"),
                    landkode = row.getStringOrNull("LANDKODE"),
                    adresseType = row.getEnumOrNull("ADRESSE_TYPE")
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

        val landkoderId = connection.executeReturnKey("INSERT INTO BRUKER_LAND_AGGREGAT DEFAULT VALUES"){}
        connection.executeBatch("INSERT INTO BRUKER_LAND (LAND, GYLDIGFRAOGMED, GYLDIGTILOGMED, LANDKODER_ID) VALUES (?, ?, ?, ?)", personopplysning.statsborgerskap){
            setParams {
                setString(1, it.land)
                setLocalDate(2, it.gyldigFraOgMed)
                setLocalDate(3, it.gyldigTilOgMed)
                setLong(4, landkoderId)
            }
        }

        var utenlandsAdresserId: Long? = null
        if (!personopplysning.utenlandsAddresser.isNullOrEmpty()) {
            utenlandsAdresserId = connection.executeReturnKey("INSERT INTO BRUKER_UTENLANDSADRESSER_AGGREGAT DEFAULT VALUES"){}
            connection.executeBatch(
                """
                    INSERT INTO BRUKER_UTENLANDSADRESSE (UTENLANDSADRESSER_ID, ADRESSENAVN, POSTKODE, BYSTED, LANDKODE, GYLDIGFRAOGMED, GYLDIGTILOGMED, ADRESSE_TYPE) 
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                personopplysning.utenlandsAddresser!!
            ) {
                setParams {
                    setLong(1, utenlandsAdresserId)
                    setString(2, it.adresseNavn)
                    setString(3, it.postkode)
                    setString(4, it.bySted)
                    setString(5, it.landkode)
                    setLocalDateTime(6, it.gyldigFraOgMed)
                    setLocalDateTime(7, it.gyldigTilOgMed)
                    setEnumName(8, it.adresseType)
                }
            }
        }

        val personopplysningId =
            connection.executeReturnKey("INSERT INTO BRUKER_PERSONOPPLYSNING (FODSELSDATO, dodsdato, LANDKODER_ID, STATUS, UTENLANDSADRESSER_ID) VALUES (?, ?, ?, ?, ?)") {
                setParams {
                    setLocalDate(1, personopplysning.fødselsdato.toLocalDate())
                    setLocalDate(2, personopplysning.dødsdato?.toLocalDate())
                    setLong(3, landkoderId)
                    setEnumName(4, personopplysning.status)
                    setLong(5, utenlandsAdresserId)
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

    override fun lagre(behandlingId: BehandlingId, barn: Set<Barn>) {
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

    override fun slett(behandlingId: BehandlingId) {
        // Sletter ikke bruker_land og bruker_land_aggregat, da det ikke er personopplysninger her
        val brukerPersonopplysningIds = getBrukerPersonopplysningIds(behandlingId)
        val personopplysningerIds = getPersonOpplysningerIds(behandlingId)

        connection.execute("""
            delete from bruker_personopplysning where id = ANY(?::bigint[]);
            delete from personopplysning where personopplysninger_id = ANY(?::bigint[]);
            delete from personopplysninger where id = ANY(?::bigint[]);
            delete from personopplysning_grunnlag where behandling_id = ? 
        """.trimIndent()) {
            setParams {
                setLongArray(1, brukerPersonopplysningIds)
                setLongArray(2, personopplysningerIds)
                setLongArray(3, personopplysningerIds)
                setLong(4, behandlingId.id)
            }
        }
    }

    private fun getBrukerPersonopplysningIds(behandlingId: BehandlingId): List<Long> = connection.queryList(
        """
                    SELECT bruker_personopplysning_id
                    FROM personopplysning_grunnlag
                    WHERE behandling_id = ?
                 
                """.trimIndent()
    ) {
        setParams { setLong(1, behandlingId.id) }
        setRowMapper { row ->
            row.getLong("bruker_personopplysning_id")
        }
    }

    private fun getPersonOpplysningerIds(behandlingId: BehandlingId): List<Long> = connection.queryList(
        """
                    SELECT personopplysninger_id
                    FROM personopplysning_grunnlag
                    WHERE behandling_id = ?
                 
                """.trimIndent()
    ) {
        setParams { setLong(1, behandlingId.id) }
        setRowMapper { row ->
            row.getLong("personopplysninger_id")
        }
    }
}