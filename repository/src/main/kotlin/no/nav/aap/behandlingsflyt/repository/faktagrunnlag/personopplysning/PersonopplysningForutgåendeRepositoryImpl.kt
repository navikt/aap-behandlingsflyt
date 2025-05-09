package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.personopplysning

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.Dødsdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.FolkeregisterStatus
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Fødselsdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.PersonopplysningForutgåendeRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.PersonopplysningMedHistorikk
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.PersonopplysningMedHistorikkGrunnlag
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

// Mangler full støtte for barn / relaterte personer
class PersonopplysningForutgåendeRepositoryImpl(
    private val connection: DBConnection, private val personRepository: PersonRepository
) : PersonopplysningForutgåendeRepository {

    companion object : Factory<PersonopplysningForutgåendeRepositoryImpl> {
        override fun konstruer(connection: DBConnection): PersonopplysningForutgåendeRepositoryImpl {
            return PersonopplysningForutgåendeRepositoryImpl(connection, PersonRepositoryImpl(connection))
        }
    }

    override fun hentHvisEksisterer(behandlingId: BehandlingId): PersonopplysningMedHistorikkGrunnlag? {
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

    private fun mapGrunnlag(row: Row): PersonopplysningMedHistorikkGrunnlag? {
        val brukerPersonopplysningId = row.getLongOrNull("BRUKER_PERSONOPPLYSNING_ID")
        if (brukerPersonopplysningId == null) {
            return null
        }
        return PersonopplysningMedHistorikkGrunnlag(
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

    private fun hentBrukerPersonopplysninger(id: Long): PersonopplysningMedHistorikk {
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
                PersonopplysningMedHistorikk(
                    id = id,
                    fødselsdato = Fødselsdato(row.getLocalDate("FODSELSDATO")),
                    dødsdato = row.getLocalDateOrNull("DODSDATO")?.let { Dødsdato(it) },
                    statsborgerskap = hentStatsborgerskap(row.getLong("LANDKODER_ID")),
                    folkeregisterStatuser = hentStatuser(row.getLong("STATUSER_ID")),
                    utenlandsAddresser = hentUtenlandsAdresser((row.getLongOrNull("UTENLANDSADRESSER_ID")))
                )
            }
        }
    }

    override fun lagre(behandlingId: BehandlingId, personopplysning: PersonopplysningMedHistorikk) {
        val personopplysningGrunnlag = hentHvisEksisterer(behandlingId)

        if (personopplysningGrunnlag?.brukerPersonopplysning == personopplysning) return

        if (personopplysningGrunnlag != null) {
            deaktiverEksisterende(behandlingId)
        }

        val landkoderId = connection.executeReturnKey("INSERT INTO BRUKER_LAND_FORUTGAAENDE_AGGREGAT DEFAULT VALUES"){}
        connection.executeBatch("INSERT INTO BRUKER_LAND_FORUTGAAENDE (LAND, GYLDIGFRAOGMED, GYLDIGTILOGMED, LANDKODER_ID) VALUES (?, ?, ?, ?)", personopplysning.statsborgerskap){
            setParams {
                setString(1, it.land)
                setLocalDate(2, it.gyldigFraOgMed)
                setLocalDate(3, it.gyldigTilOgMed)
                setLong(4, landkoderId)
            }
        }

        val statuserId = connection.executeReturnKey("INSERT INTO BRUKER_STATUSER_FORUTGAAENDE_AGGREGAT DEFAULT VALUES"){}
        connection.executeBatch("INSERT INTO BRUKER_STATUSER_FORUTGAAENDE (STATUS, STATUSER_ID, GYLDIGHETSTIDSPUNKT, OPPHOERSTIDSPUNKT) VALUES (?, ?, ?, ?)", personopplysning.folkeregisterStatuser){
            setParams {
                setEnumName(1, it.status)
                setLong(2, statuserId)
                setLocalDate(3, it.gyldighetstidspunkt)
                setLocalDate(4, it.opphoerstidspunkt)
            }
        }

        var utenlandsAdresserId: Long? = null
        if (!personopplysning.utenlandsAddresser.isNullOrEmpty()) {
            utenlandsAdresserId = connection.executeReturnKey("INSERT INTO BRUKER_UTENLANDSADRESSER_FORUTGAAENDE_AGGREGAT DEFAULT VALUES"){}
            connection.executeBatch(
                """
                    INSERT INTO BRUKER_UTENLANDSADRESSE_FORUTGAAENDE (UTENLANDSADRESSER_ID, ADRESSENAVN, POSTKODE, BYSTED, LANDKODE, GYLDIGFRAOGMED, GYLDIGTILOGMED, ADRESSE_TYPE) 
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
            connection.executeReturnKey("INSERT INTO BRUKER_PERSONOPPLYSNING_FORUTGAAENDE (FODSELSDATO, DODSDATO, LANDKODER_ID, STATUSER_ID, UTENLANDSADRESSER_ID) VALUES (?, ?, ?, ?, ?)") {
                setParams {
                    setLocalDate(1, personopplysning.fødselsdato.toLocalDate())
                    setLocalDate(2, personopplysning.dødsdato?.toLocalDate())
                    setLong(3, landkoderId)
                    setLong(4, statuserId)
                    setLong(5, utenlandsAdresserId)
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

    override fun slett(behandlingId: BehandlingId) {
        // Sletter ikke bruker_land_forutgaaende, bruker_statuser_forutgaaende, og bruker_land_forutgaaende_aggregat, da det ikke er personopplysninger her
        val brukerPersonopplysningIds = getBrukerPersonopplysningIds(behandlingId)
        val personopplysningerIds = getPersonOpplysningerIds(behandlingId)
        val personopplysningIds = getPersonOpplysningIds(personopplysningerIds)
        val utenlandsAdresserIds = getUtenlandsAdresserIds(brukerPersonopplysningIds)

        connection.execute("""
            delete from personopplysning_forutgaaende_grunnlag where behandling_id = ?; 
            delete from bruker_utenlandsadresse_forutgaaende where utenlandsadresser_id = ANY(?::bigint[]);
            delete from bruker_utenlandsadresser_forutgaaende_aggregat where id = ANY(?::bigint[]);
            delete from bruker_personopplysning_forutgaaende where id = ANY(?::bigint[]);
            delete from personopplysning_forutgaaende where id = ANY(?::bigint[]);
            delete from personopplysninger_forutgaaende where id = ANY(?::bigint[]);        
        """.trimIndent()) {
            setParams {
                setLong(1, behandlingId.id)
                setLongArray(2, utenlandsAdresserIds)
                setLongArray(3, utenlandsAdresserIds)
                setLongArray(4, brukerPersonopplysningIds)
                setLongArray(5, personopplysningIds)
                setLongArray(6, personopplysningerIds)
            }
        }
    }

    private fun getBrukerPersonopplysningIds(behandlingId: BehandlingId): List<Long> = connection.queryList(
        """
                    SELECT bruker_personopplysning_id
                    FROM personopplysning_forutgaaende_grunnlag
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
                    FROM personopplysning_forutgaaende_grunnlag
                    WHERE behandling_id = ?
                 
                """.trimIndent()
    ) {
        setParams { setLong(1, behandlingId.id) }
        setRowMapper { row ->
            row.getLong("personopplysninger_id")
        }
    }

    private fun getPersonOpplysningIds(personopplysningerIds: List<Long>): List<Long> = connection.queryList(
        """
                    SELECT id
                    FROM personopplysninger_forutgaaende
                    WHERE id = ANY(?::bigint[]);
                 
                """.trimIndent()
    ) {
        setParams { setLongArray(1, personopplysningerIds) }
        setRowMapper { row ->
            row.getLong("id")
        }
    }

    private fun getUtenlandsAdresserIds(brukerPersonopplysningIds: List<Long>): List<Long> = connection.queryList(
        """
                    SELECT utenlandsadresser_id
                    FROM bruker_personopplysning_forutgaaende
                    WHERE id = ANY(?::bigint[]);
                 
                """.trimIndent()
    ) {
        setParams { setLongArray(1, brukerPersonopplysningIds) }
        setRowMapper { row ->
            row.getLong("utenlandsadresser_id")
        }
    }

    private fun hentStatsborgerskap(id: Long): List<Statsborgerskap> {
        return connection.queryList(
            """
                SELECT * FROM BRUKER_LAND_FORUTGAAENDE
                WHERE LANDKODER_ID = ?
            """.trimIndent()
        ) {
            setParams {
                setLong(1, id)
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
            SELECT * FROM BRUKER_UTENLANDSADRESSE_FORUTGAAENDE
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

    private fun hentStatuser(id: Long): List<FolkeregisterStatus> {
        return connection.queryList(
            """
                SELECT * FROM BRUKER_STATUSER_FORUTGAAENDE
                WHERE STATUSER_ID = ?
            """.trimIndent()
        ) {
            setParams {
                setLong(1, id)
            }
            setRowMapper { row ->
                FolkeregisterStatus(
                    row.getEnum("STATUS"),
                    row.getLocalDateOrNull("GYLDIGHETSTIDSPUNKT"),
                    row.getLocalDateOrNull("OPPHOERSTIDSPUNKT")
                )
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