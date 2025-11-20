package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.register.inntekt

import no.nav.aap.behandlingsflyt.behandling.beregning.InntektsPeriode
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
        inntekter: Set<InntektPerÅr>,
        inntektPerMåned: Set<InntektsPeriode>
    ) {
        val eksisterendeGrunnlag = hentHvisEksisterer(behandlingId)
        val nyttGrunnlag = InntektGrunnlag(
            inntekter = inntekter,
            inntektPerMåned = inntektPerMåned
        )

        if (eksisterendeGrunnlag != null) {
            deaktiverGrunnlag(behandlingId)
        }

        lagre(behandlingId, nyttGrunnlag)
    }

    private fun lagre(behandlingId: BehandlingId, nyttGrunnlag: InntektGrunnlag) {
        val inntekterId = lagreInntekter(nyttGrunnlag.inntekter, nyttGrunnlag.inntektPerMåned)

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

    private fun lagreInntekter(inntekter: Set<InntektPerÅr>, inntektPerMåned: Set<InntektsPeriode>): Long {
        // TODO lagre inntektPerMåned
        val query = """
            INSERT INTO INNTEKTER DEFAULT VALUES
        """.trimIndent()

        val inntekterId = connection.executeReturnKey(query)

        val inntektQuery = """
                INSERT INTO INNTEKT (inntekt_id, ar, belop) VALUES (?, ?, ?)
            """.trimIndent()
        connection.executeBatch(inntektQuery, inntekter.toList()) {
            setParams { inntektPerÅr ->
                setLong(1, inntekterId)
                setLong(2, inntektPerÅr.år.value.toLong())
                setBigDecimal(3, inntektPerÅr.beløp.verdi())
            }
        }

        val inntektPerMndInsertQuery = """
            INSERT INTO INNTEKT_PERIODE (inntekt_id, periode, belop) VALUES (?, ?::daterange, ?)
        """.trimIndent()
        connection.executeBatch(inntektPerMndInsertQuery, inntektPerMåned.toList()) {
            setParams { inntektPerMåned ->
                setLong(1, inntekterId)
                setPeriode(2, inntektPerMåned.periode)
                setBigDecimal(3, inntektPerMåned.beløp.toBigDecimal())
            }
        }

        return inntekterId
    }

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        val eksisterendeGrunnlag = hentHvisEksisterer(fraBehandling) ?: return

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

        val deletedRows = connection.executeReturnUpdated(
            """
            delete from inntekt_grunnlag where behandling_id = ?; 
            delete from inntekt where inntekt_id = ANY(?::bigint[]);
            delete from inntekt_periode where inntekt_id = ANY(?::bigint[]);
        """.trimIndent()
        ) {
            setParams {
                setLong(1, behandlingId.id)
                setLongArray(2, inntektIds)
                setLongArray(3, inntektIds)
            }
        }
        log.info("Slettet $deletedRows rader fra inntekt_grunnlag")
    }

    private fun getInntektIds(behandlingId: BehandlingId): List<Long> = connection.queryList(
        """
                    SELECT inntekt_id
                    FROM inntekt_grunnlag
                    WHERE behandling_id = ?
                 
                """.trimIndent()
    ) {
        setParams { setLong(1, behandlingId.id) }
        setRowMapper { row ->
            row.getLong("inntekt_id")
        }
    }

    private fun mapGrunnlag(row: Row): InntektGrunnlag {
        return InntektGrunnlag(
            inntekter = row.getLongOrNull("inntekt_id")?.let(::mapInntekter).orEmpty(),
            inntektPerMåned = row.getLongOrNull("inntekt_id")?.let(::mapInntektPerMåned).orEmpty()
        )
    }

    private fun mapInntektPerMåned(id: Long): Set<InntektsPeriode> =
        connection.querySet("SELECT * FROM INNTEKT_PERIODE WHERE INNTEKT_ID = ?") {
            setParams { setLong(1, id) }
            setRowMapper {
                InntektsPeriode(
                    periode = it.getPeriode("periode"),
                    beløp = it.getDouble("belop")
                )
            }
        }

    private fun mapInntekter(inntektId: Long): Set<InntektPerÅr> {
        val query = """
            SELECT * FROM INNTEKT WHERE inntekt_id = ?
        """.trimIndent()

        return connection.querySet(query) {
            setParams {
                setLong(1, inntektId)
            }
            setRowMapper {
                InntektPerÅr(
                    Year.parse(it.getString("ar")),
                    Beløp(it.getBigDecimal("belop"))
                )
            }
        }
    }

    override fun hent(behandlingId: BehandlingId): InntektGrunnlag {
        return requireNotNull(hentHvisEksisterer(behandlingId))
    }
}