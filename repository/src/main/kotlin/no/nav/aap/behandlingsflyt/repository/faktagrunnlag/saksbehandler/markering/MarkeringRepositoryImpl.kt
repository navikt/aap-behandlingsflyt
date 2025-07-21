package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.markering

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.markering.Markering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.markering.MarkeringRepository
import no.nav.aap.behandlingsflyt.kontrakt.behandling.MarkeringType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.Row
import no.nav.aap.lookup.repository.Factory
import org.slf4j.LoggerFactory

class MarkeringRepositoryImpl(
    private val connection: DBConnection
) : MarkeringRepository {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun lagre(markering: Markering) {
        // Skal ikke kunne ha to aktive markeringer av samme type. Kommer det inn en ny, så deaktiver den gamle.
        val eksisterendeMarkeringer =
            hentAktiveMarkeringerForBehandling(markering.forBehandling).filter {
                it.markeringType ==
                    markering.markeringType
            }
        if (eksisterendeMarkeringer.isNotEmpty()) {
            eksisterendeMarkeringer.forEach { deaktiverMarkering(it.forBehandling, it.markeringType) }
        }

        val insertQuery =
            """
            INSERT INTO MARKERING (behandling_id, markering_type, begrunnelse, er_aktiv, opprettet_av)
            VALUES (?, ?, ?, ?, ?)
            """.trimIndent()

        connection.execute(insertQuery) {
            setParams {
                setLong(1, markering.forBehandling.id)
                setEnumName(2, markering.markeringType)
                setString(3, markering.begrunnelse)
                setBoolean(4, markering.erAktiv)
                setString(5, markering.opprettetAv)
            }
        }
    }

    override fun hentAktiveMarkeringerForBehandling(behandling: BehandlingId): List<Markering> {
        val query =
            """
            SELECT * FROM MARKERING WHERE behandling_id = ?
            """.trimIndent()

        return connection
            .queryList(query) {
                setParams {
                    setLong(1, behandling.id)
                }
                setRowMapper { row ->
                    mapMarkering(row)
                }
            }.filter { it.erAktiv }
    }

    override fun deaktiverMarkering(behandling: BehandlingId, type: MarkeringType) {
        val query =
            """
            UPDATE MARKERING SET er_aktiv = FALSE WHERE behandling_id = ? AND markering_type = ? AND er_aktiv = ?;
            """.trimIndent()

        connection.execute(query) {
            setParams {
                setLong(1, behandling.id)
                setEnumName(2, type)
                setBoolean(3, true)
            }
        }
    }

    override fun kopier(
        fraBehandling: BehandlingId,
        tilBehandling: BehandlingId
    ) {
        // Trengs ikke
    }

    override fun slett(behandlingId: BehandlingId) {
        val deletedRows =
            connection.executeReturnUpdated(
                """
                delete from markering where behandling_id = ?;
                """.trimIndent()
            ) {
                setParams {
                    setLong(1, behandlingId.id)
                }
            }
        log.info("Slettet $deletedRows rader fra tabell markering")
    }

    private fun mapMarkering(row: Row): Markering =
        Markering(
            forBehandling = BehandlingId(row.getLong("behandling_id")),
            markeringType = row.getEnum("markering_type"),
            begrunnelse = row.getString("begrunnelse"),
            erAktiv = row.getBoolean("er_aktiv"),
            opprettetAv = row.getString("opprettet_av")
        )

    companion object : Factory<MarkeringRepositoryImpl> {
        override fun konstruer(connection: DBConnection): MarkeringRepositoryImpl = MarkeringRepositoryImpl(connection)
    }
}