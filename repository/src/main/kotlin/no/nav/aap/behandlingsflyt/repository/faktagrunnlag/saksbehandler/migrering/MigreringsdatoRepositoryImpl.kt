package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.migrering

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.migrering.MigreringsdatoGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.migrering.MigreringsdatoRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.migrering.MigreringsdatoVurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.lookup.repository.Factory
import java.time.LocalDateTime

class MigreringsdatoRepositoryImpl(private val connection: DBConnection) : MigreringsdatoRepository {

    override fun hentHvisEksisterer(behandlingId: BehandlingId): MigreringsdatoGrunnlag? {
        val vurderingerId: Long = connection.queryFirstOrNull(
            """
            SELECT vurderinger_id
            FROM migreringsdato_grunnlag
            WHERE aktiv AND behandling_id = ?
            """
        ) {
            setParams { setLong(1, behandlingId.toLong()) }
            setRowMapper { row -> row.getLong("vurderinger_id") }
        } ?: return null

        return MigreringsdatoGrunnlag(hentVurderinger(vurderingerId))
    }

    private fun hentVurderinger(vurderingerId: Long): List<MigreringsdatoVurdering> {
        return connection.queryList(
            """
            SELECT migreringsdato, vurdert_av, vurdert_i_behandling, opprettet
            FROM migreringsdato_vurdering
            WHERE vurderinger_id = ?
            ORDER BY opprettet
            """
        ) {
            setParams { setLong(1, vurderingerId) }
            setRowMapper { row ->
                MigreringsdatoVurdering(
                    migreringsdato = row.getLocalDate("migreringsdato"),
                    vurdertAv = Bruker(row.getString("vurdert_av")),
                    vurdertIBehandling = BehandlingId(row.getLong("vurdert_i_behandling")),
                    opprettet = row.getLocalDateTime("opprettet"),
                )
            }
        }
    }

    override fun lagreVurdering(behandlingId: BehandlingId, vurdering: MigreringsdatoVurdering) {
        if (hentHvisEksisterer(behandlingId) != null) {
            deaktiverGrunnlag(behandlingId)
        }

        val vurderingerId = connection.executeReturnKey(
            "INSERT INTO migreringsdato_vurderinger DEFAULT VALUES"
        )

        connection.execute(
            """
            INSERT INTO migreringsdato_vurdering (migreringsdato, vurdert_av, vurdert_i_behandling, opprettet, vurderinger_id)
            VALUES (?, ?, ?, ?, ?)
            """
        ) {
            setParams {
                setLocalDate(1, vurdering.migreringsdato)
                setString(2, vurdering.vurdertAv.ident)
                setLong(3, vurdering.vurdertIBehandling.toLong())
                setLocalDateTime(4, vurdering.opprettet)
                setLong(5, vurderingerId)
            }
        }

        connection.execute(
            """
            INSERT INTO migreringsdato_grunnlag (behandling_id, vurderinger_id, aktiv, opprettet)
            VALUES (?, ?, true, ?)
            """
        ) {
            setParams {
                setLong(1, behandlingId.toLong())
                setLong(2, vurderingerId)
                setLocalDateTime(3, LocalDateTime.now())
            }
        }
    }

    override fun deaktiverGrunnlag(behandlingId: BehandlingId) {
        connection.execute(
            """
            UPDATE migreringsdato_grunnlag SET aktiv = false
            WHERE aktiv AND behandling_id = ?
            """
        ) {
            setParams { setLong(1, behandlingId.toLong()) }
        }
    }

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        require(fraBehandling != tilBehandling)
        if (hentHvisEksisterer(tilBehandling) != null) {
            deaktiverGrunnlag(tilBehandling)
        }
        connection.execute(
            """
            INSERT INTO migreringsdato_grunnlag (behandling_id, vurderinger_id, opprettet)
            SELECT ?, vurderinger_id, ?
            FROM migreringsdato_grunnlag
            WHERE aktiv AND behandling_id = ?
            """
        ) {
            setParams {
                setLong(1, tilBehandling.toLong())
                setLocalDateTime(2, LocalDateTime.now())
                setLong(3, fraBehandling.toLong())
            }
        }
    }

    override fun slett(behandlingId: BehandlingId) {
        val vurderingerIds = connection.queryList(
            "SELECT vurderinger_id FROM migreringsdato_grunnlag WHERE behandling_id = ?"
        ) {
            setParams { setLong(1, behandlingId.toLong()) }
            setRowMapper { row -> row.getLong("vurderinger_id") }
        }

        connection.execute(
            "DELETE FROM migreringsdato_grunnlag WHERE behandling_id = ?"
        ) {
            setParams { setLong(1, behandlingId.toLong()) }
        }

        if (vurderingerIds.isNotEmpty()) {
            connection.execute(
                "DELETE FROM migreringsdato_vurdering WHERE vurderinger_id = ANY(?::bigint[])"
            ) {
                setParams { setLongArray(1, vurderingerIds) }
            }
            connection.execute(
                "DELETE FROM migreringsdato_vurderinger WHERE id = ANY(?::bigint[])"
            ) {
                setParams { setLongArray(1, vurderingerIds) }
            }
        }
    }

    companion object : Factory<MigreringsdatoRepositoryImpl> {
        override fun konstruer(connection: DBConnection): MigreringsdatoRepositoryImpl {
            return MigreringsdatoRepositoryImpl(connection)
        }
    }
}
