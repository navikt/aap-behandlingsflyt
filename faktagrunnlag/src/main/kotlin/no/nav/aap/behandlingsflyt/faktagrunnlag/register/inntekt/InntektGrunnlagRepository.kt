package no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.Row
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.verdityper.sakogbehandling.BehandlingId
import java.time.Year

class InntektGrunnlagRepository(private val connection: DBConnection) {

    private fun deaktiverGrunnlag(behandlingId: BehandlingId) {
        connection.execute("UPDATE INNTEKT_GRUNNLAG set aktiv = false WHERE behandling_id = ? and aktiv = true") {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setResultValidator { require(it == 1) }
        }
    }

    fun lagre(
        behandlingId: BehandlingId,
        inntekter: Set<InntektPerÅr>
    ) {
        val eksisterendeGrunnlag = hentHvisEksisterer(behandlingId)
        val nyttGrunnlag = InntektGrunnlag(
            null,
            inntekter = inntekter,
        )

        if (eksisterendeGrunnlag != nyttGrunnlag) {
            eksisterendeGrunnlag?.let {
                deaktiverGrunnlag(behandlingId)
            }

            lagre(behandlingId, nyttGrunnlag)
        }
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

    private fun lagreInntekter(inntekter: Set<InntektPerÅr>): Long? {
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

    fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
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

    fun hentHvisEksisterer(behandlingId: BehandlingId): InntektGrunnlag? {
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

    private fun mapGrunnlag(row: Row): InntektGrunnlag {
        return InntektGrunnlag(
            row.getLong("id"),
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

    fun hent(behandlingId: BehandlingId): InntektGrunnlag {
        return requireNotNull(hentHvisEksisterer(behandlingId))
    }
}
