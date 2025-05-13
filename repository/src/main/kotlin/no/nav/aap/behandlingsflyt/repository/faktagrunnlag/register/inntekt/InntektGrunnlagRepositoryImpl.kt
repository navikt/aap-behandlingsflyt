package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.register.inntekt

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.InntektGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.InntektGrunnlagRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.InntektPerÅr
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.Row
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.lookup.repository.Factory
import org.slf4j.LoggerFactory
import java.time.Year

class InntektGrunnlagRepositoryImpl(private val connection: DBConnection) :
    InntektGrunnlagRepository {

    private val log = LoggerFactory.getLogger(javaClass)

    companion object : Factory<InntektGrunnlagRepository> {
        override fun konstruer(connection: DBConnection): InntektGrunnlagRepository {
            return InntektGrunnlagRepositoryImpl(connection)
        }
    }

    private fun deaktiverGrunnlag(behandlingId: BehandlingId) {
        connection.execute("UPDATE INNTEKT_GRUNNLAG set aktiv = false WHERE behandling_id = ? and aktiv = true") {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setResultValidator { require(it == 1) }
        }
    }

    override fun lagre(
        behandlingId: BehandlingId,
        inntekter: Set<InntektPerÅr>
    ) {
        val eksisterendeGrunnlag = hentHvisEksisterer(behandlingId)
        val nyttGrunnlag = InntektGrunnlag(
            inntekter = inntekter,
        )

        if (eksisterendeGrunnlag != null) {
            deaktiverGrunnlag(behandlingId)
        }

        lagre(behandlingId, nyttGrunnlag)

    }

    private fun lagre(behandlingId: BehandlingId, nyttGrunnlag: InntektGrunnlag) {
        val inntekterId = lagreInntekter(nyttGrunnlag.inntekter)

        val query = """
            INSERT INTO INNTEKT_GRUNNLAG (behandling_id, inntekt_id) VALUES (?, ?)
        """.trimIndent()

        connection.execute(query) {
            setParams {
                setLong(1, behandlingId.toLong())
                setLong(2, inntekterId)
            }
        }
    }

    private fun lagreInntekter(inntekter: Set<InntektPerÅr>): Long {
        val query = """
            INSERT INTO INNTEKTER DEFAULT VALUES
        """.trimIndent()

        val inntekterId = connection.executeReturnKey(query)

        for (inntektPerÅr in inntekter) {
            val inntektQuery = """
                INSERT INTO INNTEKT (inntekt_id, ar, belop) VALUES (?, ?, ?)
            """.trimIndent()
            connection.execute(inntektQuery) {
                setParams {
                    setLong(1, inntekterId)
                    setLong(2, inntektPerÅr.år.value.toLong())
                    setBigDecimal(3, inntektPerÅr.beløp.verdi())
                }
            }
        }
        return inntekterId
    }

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        val eksisterendeGrunnlag = hentHvisEksisterer(fraBehandling)
        if (eksisterendeGrunnlag == null) {
            return
        }
        val query = """
            INSERT INTO INNTEKT_GRUNNLAG (behandling_id, inntekt_id) SELECT ?, inntekt_id from INNTEKT_GRUNNLAG where behandling_id = ? and aktiv
        """.trimIndent()

        connection.execute(query) {
            setParams {
                setLong(1, tilBehandling.toLong())
                setLong(2, fraBehandling.toLong())
            }
        }
    }

    override fun hentHvisEksisterer(behandlingId: BehandlingId): InntektGrunnlag? {
        val query = """
            SELECT * FROM INNTEKT_GRUNNLAG WHERE behandling_id = ? and aktiv = true
        """.trimIndent()
        return connection.queryFirstOrNull(query) {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setRowMapper(::mapGrunnlag)
        }
    }

    override fun slett(behandlingId: BehandlingId) {

        val inntektIds = getInntektIds(behandlingId)

        val deletedRows = connection.executeReturnUpdated("""
            delete from inntekt_grunnlag where behandling_id = ?; 
            delete from inntekt where inntekt_id = ANY(?::bigint[]);
          
        """.trimIndent()) {
            setParams {
                setLong(1, behandlingId.id)
                setLongArray(2, inntektIds)
            }
        }
        log.info("Slettet $deletedRows fra inntekt_grunnlag")
    }

    private fun getInntektIds(behandlingId: BehandlingId): List<Long> = connection.queryList(
        """
                    SELECT inntekt_id
                    FROM inntekt_grunnlag
                    WHERE behandling_id = ? AND inntekt_id is not null
                 
                """.trimIndent()
    ) {
        setParams { setLong(1, behandlingId.id) }
        setRowMapper { row ->
            row.getLong("inntekt_id")
        }
    }

    private fun mapGrunnlag(row: Row): InntektGrunnlag {
        return InntektGrunnlag(
            mapInntekter(row.getLongOrNull("inntekt_id"))
        )
    }

    private fun mapInntekter(inntektId: Long?): Set<InntektPerÅr> {
        if (inntektId == null) {
            return setOf()
        }
        val query = """
            SELECT * FROM INNTEKT WHERE inntekt_id = ?
        """.trimIndent()

        return connection.queryList(query) {
            setParams {
                setLong(1, inntektId)
            }
            setRowMapper {
                InntektPerÅr(
                    Year.parse(it.getString("ar")),
                    Beløp(it.getBigDecimal("belop"))
                )
            }
        }.toSet()
    }

    override fun hent(behandlingId: BehandlingId): InntektGrunnlag {
        return requireNotNull(hentHvisEksisterer(behandlingId))
    }
}