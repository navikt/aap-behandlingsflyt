package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.arbeidsevne

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsevne.ArbeidsevneGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsevne.ArbeidsevneRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsevne.ArbeidsevneVurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.lookup.repository.Factory
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.LocalDateTime

class ArbeidsevneRepositoryImpl(private val connection: DBConnection) : ArbeidsevneRepository {

    private val log = LoggerFactory.getLogger(javaClass)

    companion object : Factory<ArbeidsevneRepositoryImpl> {
        override fun konstruer(connection: DBConnection): ArbeidsevneRepositoryImpl {
            return ArbeidsevneRepositoryImpl(connection)
        }
    }

    override fun hentHvisEksisterer(behandlingId: BehandlingId): ArbeidsevneGrunnlag? {
        return connection.queryList(
            """
            SELECT a.ID AS ARBEIDSEVNE_ID, v.BEGRUNNELSE, v.FRA_DATO, v.ANDEL_ARBEIDSEVNE, v.OPPRETTET_TID
            FROM ARBEIDSEVNE_GRUNNLAG g
            INNER JOIN ARBEIDSEVNE a ON g.ARBEIDSEVNE_ID = a.ID
            INNER JOIN ARBEIDSEVNE_VURDERING v ON a.ID = v.ARBEIDSEVNE_ID
            WHERE g.AKTIV AND g.BEHANDLING_ID = ?
            """.trimIndent()
        ) {
            setParams { setLong(1, behandlingId.toLong()) }
            setRowMapper { row ->
                ArbeidsevneInternal(
                    row.getLong("ARBEIDSEVNE_ID"),
                    row.getString("BEGRUNNELSE"),
                    row.getLocalDate("FRA_DATO"),
                    Prosent(row.getInt("ANDEL_ARBEIDSEVNE")),
                    row.getLocalDateTime("OPPRETTET_TID")
                )
            }
        }.toGrunnlag()
    }

    override fun hentAlleVurderinger(sakId: SakId, behandlingId: BehandlingId): Set<ArbeidsevneVurdering> {
        val query = """
            SELECT a.ID AS ARBEIDSEVNE_ID, v.BEGRUNNELSE, v.FRA_DATO, v.ANDEL_ARBEIDSEVNE, v.OPPRETTET_TID
            FROM ARBEIDSEVNE_GRUNNLAG g
            INNER JOIN ARBEIDSEVNE a ON g.ARBEIDSEVNE_ID = a.ID
            INNER JOIN ARBEIDSEVNE_VURDERING v ON a.ID = v.ARBEIDSEVNE_ID
            JOIN BEHANDLING b ON b.ID = g.BEHANDLING_ID
            WHERE g.AKTIV AND b.SAK_ID = ? AND b.opprettet_tid < (SELECT bh.opprettet_tid from behandling bh where id = ?)
            """.trimIndent()

        return connection.queryList(query) {
            setParams {
                setLong(1, sakId.toLong())
                setLong(2, behandlingId.toLong())
            }
            setRowMapper { row ->
                ArbeidsevneInternal(
                    row.getLong("ARBEIDSEVNE_ID"),
                    row.getString("BEGRUNNELSE"),
                    row.getLocalDate("FRA_DATO"),
                    Prosent(row.getInt("ANDEL_ARBEIDSEVNE")),
                    row.getLocalDateTime("OPPRETTET_TID")
                )
            }
        }.map { it.toArbeidsevnevurdering() }.toSet()
    }

    private data class ArbeidsevneInternal(
        val arbeidsevneId: Long,
        val begrunnelse: String,
        val fraDato: LocalDate,
        val arbeidsevne: Prosent,
        val opprettetTid: LocalDateTime
    ) {
        fun toArbeidsevnevurdering(): ArbeidsevneVurdering {
            return ArbeidsevneVurdering(begrunnelse, arbeidsevne, fraDato, opprettetTid)
        }
    }

    private fun List<ArbeidsevneInternal>.toGrunnlag(): ArbeidsevneGrunnlag? {
        return groupBy(ArbeidsevneInternal::arbeidsevneId, ArbeidsevneInternal::toArbeidsevnevurdering)
            .map { (_, arbeidsevneVurderinger) -> ArbeidsevneGrunnlag(arbeidsevneVurderinger) }
            .takeIf { it.isNotEmpty() }
            ?.single()
    }

    override fun lagre(behandlingId: BehandlingId, arbeidsevneVurderinger: List<ArbeidsevneVurdering>) {
        val eksisterendeArbeidsevneGrunnlag = hentHvisEksisterer(behandlingId)

        if (eksisterendeArbeidsevneGrunnlag?.vurderinger == arbeidsevneVurderinger) return

        if (eksisterendeArbeidsevneGrunnlag != null) deaktiverEksisterende(behandlingId)

        val arbeidsevneId = connection.executeReturnKey("INSERT INTO ARBEIDSEVNE DEFAULT VALUES")

        connection.execute("INSERT INTO ARBEIDSEVNE_GRUNNLAG (BEHANDLING_ID, ARBEIDSEVNE_ID) VALUES (?, ?)") {
            setParams {
                setLong(1, behandlingId.toLong())
                setLong(2, arbeidsevneId)
            }
        }

        arbeidsevneVurderinger.lagre(arbeidsevneId)
    }

    private fun List<ArbeidsevneVurdering>.lagre(arbeidsevneId: Long) {
        val (nyeVurderinger, eksisterendeVurderinger) = this.partition { it.opprettetTid == null }

        connection.executeBatch(
            """
            INSERT INTO ARBEIDSEVNE_VURDERING 
            (ARBEIDSEVNE_ID, FRA_DATO, BEGRUNNELSE, ANDEL_ARBEIDSEVNE) VALUES (?, ?, ?, ?)
            """.trimIndent(),
            nyeVurderinger
        ) {
            setParams {
                setLong(1, arbeidsevneId)
                setLocalDate(2, it.fraDato)
                setString(3, it.begrunnelse)
                setInt(4, it.arbeidsevne.prosentverdi())
            }
        }

        connection.executeBatch(
            """
            INSERT INTO ARBEIDSEVNE_VURDERING 
            (ARBEIDSEVNE_ID, FRA_DATO, BEGRUNNELSE, ANDEL_ARBEIDSEVNE, OPPRETTET_TID) VALUES (?, ?, ?, ?, ?)
            """.trimIndent(),
            eksisterendeVurderinger
        ) {
            setParams {
                setLong(1, arbeidsevneId)
                setLocalDate(2, it.fraDato)
                setString(3, it.begrunnelse)
                setInt(4, it.arbeidsevne.prosentverdi())
                setLocalDateTime(5, it.opprettetTid)
            }
        }
    }

    private fun deaktiverEksisterende(behandlingId: BehandlingId) {
        connection.execute("UPDATE ARBEIDSEVNE_GRUNNLAG SET AKTIV = FALSE WHERE AKTIV AND BEHANDLING_ID = ?") {
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
        connection.execute("INSERT INTO ARBEIDSEVNE_GRUNNLAG (BEHANDLING_ID, ARBEIDSEVNE_ID) SELECT ?, ARBEIDSEVNE_ID FROM ARBEIDSEVNE_GRUNNLAG WHERE AKTIV AND BEHANDLING_ID = ?") {
            setParams {
                setLong(1, tilBehandling.toLong())
                setLong(2, fraBehandling.toLong())
            }
        }
    }

    override fun slett(behandlingId: BehandlingId) {

        val arbeidsevneIds = getArbeidsevneIds(behandlingId)

        val deletedRows = connection.executeReturnUpdated("""
            delete from arbeidsevne_grunnlag where behandling_id = ?; 
            delete from arbeidsevne_vurdering where arbeidsevne_id = ANY(?::bigint[]);
            delete from arbeidsevne where id = ANY(?::bigint[]);
           
        """.trimIndent()) {
            setParams {
                setLong(1, behandlingId.id)
                setLongArray(2, arbeidsevneIds)
                setLongArray(3, arbeidsevneIds)

            }
        }
        log.info("Slettet $deletedRows fra arbeidsevne_grunnlag")
    }

    private fun getArbeidsevneIds(behandlingId: BehandlingId): List<Long> = connection.queryList(
        """
                    SELECT arbeidsevne_id
                    FROM arbeidsevne_grunnlag
                    WHERE behandling_id = ?
                 
                """.trimIndent()
    ) {
        setParams { setLong(1, behandlingId.id) }
        setRowMapper { row ->
            row.getLong("arbeidsevne_id")
        }
    }
}
