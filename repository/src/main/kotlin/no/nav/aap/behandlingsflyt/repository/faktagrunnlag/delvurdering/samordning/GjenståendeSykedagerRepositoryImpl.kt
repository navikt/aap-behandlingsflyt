package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.samordning

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.GjenståendeSykedagerRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway.GjenståendeSykedager
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.repository.Factory
import java.time.Instant

class GjenståendeSykedagerRepositoryImpl(private val dbConnection: DBConnection) : GjenståendeSykedagerRepository {

    companion object : Factory<GjenståendeSykedagerRepositoryImpl> {
        override fun konstruer(connection: DBConnection): GjenståendeSykedagerRepositoryImpl {
            return GjenståendeSykedagerRepositoryImpl(connection)
        }
    }

    override fun hentHvisEksisterer(behandlingId: BehandlingId): GjenståendeSykedager? {
        return dbConnection.queryFirstOrNull(
            """
            SELECT grunnlag, gjenstaaende_dager, vurdert_til_og_med
            FROM gjenstaaende_sykedager_grunnlag
            WHERE behandling_id = ? AND aktiv = TRUE
            """.trimIndent()
        ) {
            setParams { setLong(1, behandlingId.toLong()) }
            setRowMapper { row ->
                GjenståendeSykedager(
                    grunnlag = row.getEnum("grunnlag"),
                    gjenståendeDager = row.getIntOrNull("gjenstaaende_dager"),
                    vurdertTilOgMed = row.getLocalDateOrNull("vurdert_til_og_med"),
                )
            }
        }
    }

    override fun lagre(behandlingId: BehandlingId, gjenståendeSykedager: GjenståendeSykedager) {
        val eksisterende = hentHvisEksisterer(behandlingId)
        if (eksisterende == gjenståendeSykedager) {
            return
        }
        if (eksisterende != null) {
            deaktiverGrunnlag(behandlingId)
        }

        dbConnection.execute(
            """
            INSERT INTO gjenstaaende_sykedager_grunnlag
                (behandling_id, aktiv, grunnlag, gjenstaaende_dager, vurdert_til_og_med, opprettet_tid)
            VALUES (?, TRUE, ?, ?, ?, ?)
            """.trimIndent()
        ) {
            setParams {
                setLong(1, behandlingId.toLong())
                setEnumName(2, gjenståendeSykedager.grunnlag)
                setInt(3, gjenståendeSykedager.gjenståendeDager)
                setLocalDate(4, gjenståendeSykedager.vurdertTilOgMed)
                setInstant(5, Instant.now())
            }
        }
    }

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        dbConnection.execute(
            """
            INSERT INTO gjenstaaende_sykedager_grunnlag
                (behandling_id, aktiv, grunnlag, gjenstaaende_dager, vurdert_til_og_med, opprettet_tid)
            SELECT ?, TRUE, grunnlag, gjenstaaende_dager, vurdert_til_og_med, ?
            FROM gjenstaaende_sykedager_grunnlag
            WHERE behandling_id = ? AND aktiv = TRUE
            """.trimIndent()
        ) {
            setParams {
                setLong(1, tilBehandling.toLong())
                setInstant(2, Instant.now())
                setLong(3, fraBehandling.toLong())
            }
        }
    }

    override fun slett(behandlingId: BehandlingId) {
        dbConnection.execute("DELETE FROM gjenstaaende_sykedager_grunnlag WHERE behandling_id = ?") {
            setParams { setLong(1, behandlingId.toLong()) }
        }
    }

    private fun deaktiverGrunnlag(behandlingId: BehandlingId) {
        dbConnection.execute(
            "UPDATE gjenstaaende_sykedager_grunnlag SET aktiv = FALSE WHERE behandling_id = ? AND aktiv = TRUE"
        ) {
            setParams { setLong(1, behandlingId.toLong()) }
            setResultValidator { require(it == 1) }
        }
    }
}
