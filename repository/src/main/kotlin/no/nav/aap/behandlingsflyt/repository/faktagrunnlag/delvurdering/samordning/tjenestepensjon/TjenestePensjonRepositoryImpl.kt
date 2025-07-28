package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.samordning.tjenestepensjon

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.tjenestepensjon.TjenestePensjonForhold
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.tjenestepensjon.TjenestePensjonOrdning
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.tjenestepensjon.TjenestePensjonRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.tjenestepensjon.TjenestePensjonYtelse
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.tjenestepensjon.YtelseTypeCode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.repository.Factory
import org.slf4j.LoggerFactory

class TjenestePensjonRepositoryImpl(private val dbConnection: DBConnection) : TjenestePensjonRepository {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object : Factory<TjenestePensjonRepository> {
        override fun konstruer(connection: DBConnection): TjenestePensjonRepository {
            return TjenestePensjonRepositoryImpl(connection)
        }
    }

    override fun hentHvisEksisterer(behandlingId: BehandlingId): List<TjenestePensjonForhold>? {
        val tyIdSql =
            """SELECT TJENESTEPENSJON_ORDNINGER_ID FROM TJENESTEPENSJON_FORHOLD_GRUNNLAG WHERE BEHANDLING_ID = ? AND AKTIV = true""".trimIndent()

        val tpOrdningId = dbConnection.queryFirstOrNull(
            tyIdSql
        ) {
            setParams { setLong(1, behandlingId.id) }
            setRowMapper {
                it.getLong("TJENESTEPENSJON_ORDNINGER_ID")
            }
        }

        if (tpOrdningId == null) return null

        val ordninger = hentOrdning(tpOrdningId)

        return ordninger
    }

    override fun hent(behandlingId: BehandlingId): List<TjenestePensjonForhold> {
        return requireNotNull(hentHvisEksisterer(behandlingId))
    }

    private fun hentOrdning(ordningerId: Long): List<TjenestePensjonForhold> {
        val sql = """
            SELECT *
                FROM TJENESTEPENSJON_ORDNING WHERE TJENESTEPENSJON_ORDNINGER_ID = ?
        """.trimIndent()

        val ordning = dbConnection.queryList(
            sql
        ) {
            setParams {
                setLong(1, ordningerId)
            }
            setRowMapper {
                TjenestePensjonForhold(
                    ordning = TjenestePensjonOrdning(
                        navn = it.getString("navn"),
                        tpNr = it.getString("tpNr"),
                        orgNr = it.getString("orgNr"),
                    ),
                    ytelser = hentYtelse(it.getLong("ID")),
                )

            }
        }
        return ordning
    }

    private fun hentYtelse(ordningId: Long): List<TjenestePensjonYtelse> {
        val sql = """
            SELECT *
                FROM TJENESTEPENSJON_YTELSE WHERE TJENESTEPENSJON_ORDNING_ID = ?
        """.trimIndent()

        return dbConnection.queryList(sql) {
            setParams { setLong(1, ordningId) }
            setRowMapper {
                TjenestePensjonYtelse(
                    innmeldtYtelseFom = it.getLocalDateOrNull("INNMELDT_FOM"),
                    ytelseIverksattFom = it.getLocalDate("IVERKSATT_FOM"),
                    ytelseIverksattTom = it.getLocalDateOrNull("IVERKSATT_TOM"),
                    ytelseType = YtelseTypeCode.valueOf(it.getString("YTELSE_TYPE")),
                    ytelseId = it.getLong("EXTERN_ID")
                )
            }
        }
    }


    override fun lagre(
        behandlingId: BehandlingId,
        tjenestePensjon: List<TjenestePensjonForhold>
    ) {
        val eksisterende = hentHvisEksisterer(behandlingId)
        if (eksisterende != null) {
            deaktiverGrunnlag(behandlingId)
        }

        val tyId = dbConnection.executeReturnKey(
            """
            INSERT INTO TJENESTEPENSJON_ORDNINGER DEFAULT VALUES 
        """.trimIndent()
        )

        dbConnection.execute(
            """
            INSERT INTO TJENESTEPENSJON_FORHOLD_GRUNNLAG (BEHANDLING_ID, AKTIV, TJENESTEPENSJON_ORDNINGER_ID)
            VALUES (?, true, ?)
        """.trimIndent()
        ) {
            setParams {
                setLong(1, behandlingId.id)
                setLong(2, tyId)
            }
        }

        tjenestePensjon.forEach {
            val forholdKey = dbConnection.executeReturnKey(
                """
                    INSERT INTO TJENESTEPENSJON_ORDNING (TJENESTEPENSJON_ORDNINGER_ID, navn, tpNr, orgNr)
                    VALUES (?, ?, ?, ?)
                """.trimIndent(),

                ) {
                setParams {
                    setLong(1, tyId)
                    setString(2, it.ordning.navn)
                    setString(3, it.ordning.tpNr)
                    setString(4, it.ordning.orgNr)
                }
            }
            lagreYtelse(it.ytelser, forholdKey)
        }
    }

    private fun lagreYtelse(ytelseDto: List<TjenestePensjonYtelse>, forholdKey: Long) {
        val sql = """
            INSERT INTO TJENESTEPENSJON_YTELSE (TJENESTEPENSJON_ORDNING_ID, YTELSE_TYPE, INNMELDT_FOM, IVERKSATT_FOM, IVERKSATT_TOM, EXTERN_ID)
            VALUES (?, ?, ?, ?, ?, ?)
        """.trimIndent()

        dbConnection.executeBatch(sql, ytelseDto) {
            setParams {
                setLong(1, forholdKey)
                setEnumName(2, it.ytelseType)
                setLocalDate(3, it.innmeldtYtelseFom)
                setLocalDate(4, it.ytelseIverksattFom)
                setLocalDate(5, it.ytelseIverksattTom)
                setLong(6, it.ytelseId)
            }
        }
    }


    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        val sql = """
            INSERT INTO TJENESTEPENSJON_FORHOLD_GRUNNLAG (BEHANDLING_ID, AKTIV, TJENESTEPENSJON_ORDNINGER_ID)
            SELECT ?, true, TJENESTEPENSJON_ORDNINGER_ID
            FROM TJENESTEPENSJON_FORHOLD_GRUNNLAG
            WHERE BEHANDLING_ID = ?
              AND AKTIV = true;
        """.trimIndent()
        dbConnection.execute(sql) {
            setParams {
                setLong(1, tilBehandling.id)
                setLong(2, fraBehandling.id)
            }
            log.info("Kopiert tjenestepensjon fra behandling $fraBehandling til $tilBehandling")
        }
    }

    override fun slett(behandlingId: BehandlingId) {

        val tjenestePensjonOrdningerIds = getTjenestepensjonOrdningerIds(behandlingId)
        val tjenestePensjonOrdningIds = getTjenestepensjonOrdningIds(tjenestePensjonOrdningerIds)

        val deletedRows = dbConnection.executeReturnUpdated("""
            delete from tjenestepensjon_forhold_grunnlag where behandling_id = ?;       
            delete from tjenestepensjon_ytelse where tjenestepensjon_ordning_id = ANY(?::bigint[]);
            delete from tjenestepensjon_ordning where tjenestepensjon_ordninger_id = ANY(?::bigint[]); 
            delete from tjenestepensjon_ordninger where id = ANY(?::bigint[]);       
        """.trimIndent()) {
            setParams {
                setLong(1, behandlingId.id)
                setLongArray(2, tjenestePensjonOrdningIds)
                setLongArray(3, tjenestePensjonOrdningerIds)
                setLongArray(4, tjenestePensjonOrdningerIds)
            }
        }
        log.info("Slettet $deletedRows rader fra tjenestepensjon_forhold_grunnlag")
    }

    private fun getTjenestepensjonOrdningerIds(behandlingId: BehandlingId): List<Long> = dbConnection.queryList(
        """
                    SELECT tjenestepensjon_ordninger_id
                    FROM tjenestepensjon_forhold_grunnlag
                    WHERE behandling_id = ? AND tjenestepensjon_ordninger_id is not null
                 
                 
                """.trimIndent()
    ) {
        setParams { setLong(1, behandlingId.id) }
        setRowMapper { row ->
            row.getLong("tjenestepensjon_ordninger_id")
        }
    }

    private fun getTjenestepensjonOrdningIds(ordningerIds: List<Long>): List<Long> = dbConnection.queryList(
        """
                    SELECT id
                    FROM tjenestepensjon_ordning
                    WHERE tjenestepensjon_ordninger_id = ANY(?::bigint[]);
                 
                """.trimIndent()
    ) {
        setParams { setLongArray(1, ordningerIds) }
        setRowMapper { row ->
            row.getLong("id")
        }
    }

    private fun deaktiverGrunnlag(behandlingId: BehandlingId) {
        dbConnection.execute("UPDATE TJENESTEPENSJON_FORHOLD_GRUNNLAG set aktiv = false WHERE behandling_id = ? and aktiv = true") {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setResultValidator { require(it == 1) { "UPDATE må oppdatere minst en linje i tjenestePensjon_Grunnlag" } }
        }
    }
}