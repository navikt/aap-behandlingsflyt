package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.samordning.tjenestepensjon

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.tjenestepensjon.TjenestePensjon
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.tjenestepensjon.TjenestePensjonRepository
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

    override fun hentHvisEksisterer(behandlingId: BehandlingId):TjenestePensjon?{
        val tyIdSql = """SELECT TJENESTEPENSJON_YTELSER_ID FROM TJENESTEPENSJON_GRUNNLAG WHERE BEHANDLING_ID = ? AND AKTIV = true""".trimIndent()

        val tyId = dbConnection.queryFirstOrNull(
            tyIdSql,
            {
                setParams { setLong(1, behandlingId.id) }
                setRowMapper {
                    it.getLong("TJENESTEPENSJON_YTELSER_ID")
                }
            }
        )

        if(tyId==null) return null

        val sql = """
            SELECT t.TP_NUMMER
                FROM TJENESTEPENSJON_YTELSE t
                JOIN TJENESTEPENSJON_YTELSER ty ON t.TJENESTEPENSJON_YTELSER_ID = ty.ID
            WHERE ty.ID=?
        """.trimIndent()

        val tpnummer = dbConnection.queryList<String>(
            sql,
            {
                setParams { setLong(1, tyId) }
                setRowMapper {
                    it.getString("TP_NUMMER")
                }
            }
        )

        return TjenestePensjon(tpnummer)
    }

    override fun hent(behandlingId: BehandlingId): TjenestePensjon {
        return  requireNotNull(hentHvisEksisterer(behandlingId))
    }

    override fun lagre(
        behandlingId: BehandlingId,
        tjenestePensjon: TjenestePensjon
    ) {
        val eksisterende = hentHvisEksisterer(behandlingId)
        if (eksisterende != null) {
            deaktiverGrunnlag(behandlingId)
        }

        val tyId = dbConnection.executeReturnKey("""
            INSERT INTO TJENESTEPENSJON_YTELSER DEFAULT VALUES 
        """.trimIndent()
        )

        val tgId = dbConnection.executeReturnKey("""
            INSERT INTO TJENESTEPENSJON_GRUNNLAG (BEHANDLING_ID, AKTIV, TJENESTEPENSJON_YTELSER_ID)
            VALUES (?, true, ?)
        """.trimIndent()) {
            setParams {
                setLong(1, behandlingId.id)
                setLong(2, tyId)
            }
        }

        dbConnection.executeBatch("""
            INSERT INTO TJENESTEPENSJON_YTELSE (TJENESTEPENSJON_YTELSER_ID,TP_NUMMER)
            VALUES (?, ?)
        """.trimIndent(),
            tjenestePensjon.tpNr
            ) {
            setParams {
                setLong(1, tyId)
                setString(2, it)
            }
        }
    }

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        val sql = """
            INSERT INTO TJENESTEPENSJON_GRUNNLAG (BEHANDLING_ID, AKTIV, TJENESTEPENSJON_YTELSER_ID)
            SELECT ?, true, TJENESTEPENSJON_YTELSER_ID
            FROM TJENESTEPENSJON_GRUNNLAG
            WHERE BEHANDLING_ID = ? AND AKTIV = true;
        """.trimIndent()
        dbConnection.execute(sql) {
            setParams {
                setLong(1, tilBehandling.id)
                setLong(2, fraBehandling.id)
            }
            log.info("Kopiert tjenestepensjon fra behandling $fraBehandling til $tilBehandling")
        }
    }

    private fun deaktiverGrunnlag(behandlingId: BehandlingId) {
        dbConnection.execute("UPDATE TJENESTEPENSJON_GRUNNLAG set aktiv = false WHERE behandling_id = ? and aktiv = true") {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setResultValidator { require(it == 1){"UPDATE må oppdatere minst en linje i tjenestePensjon_Grunnlag"} }
        }
    }
}