package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.lovvalgmedlemskap

import no.nav.aap.behandlingsflyt.behandling.vilkår.medlemskap.LovvalgDelvurderingerGrunnlag
import no.nav.aap.behandlingsflyt.behandling.vilkår.medlemskap.LovvalgDelvurderingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.repository.Factory

class LovvalgDelvurderingRepositoryImpl(private val connection: DBConnection) : LovvalgDelvurderingRepository {
    companion object : Factory<LovvalgDelvurderingRepositoryImpl> {
        override fun konstruer(connection: DBConnection) = LovvalgDelvurderingRepositoryImpl(connection)
    }

    override fun hentAlle(behandlingId: BehandlingId): List<LovvalgDelvurderingerGrunnlag> {
        val vurderingerId = connection.queryFirstOrNull(
            """
            SELECT vurderinger_id FROM LOVVALG_DELVURDERING_GRUNNLAG
            WHERE behandling_id = ? AND aktiv
            """.trimIndent()
        ) {
            setParams { setLong(1, behandlingId.toLong()) }
            setRowMapper { it.getLong("vurderinger_id") }
        }
        return vurderingerId?.let(::mapVurderinger).orEmpty()
    }

    private fun mapVurderinger(vurderingerId: Long): List<LovvalgDelvurderingerGrunnlag> =
        connection.queryList(
            """
            SELECT versjon, vurdering, relevant_dato, vurdert_i_behandling FROM LOVVALG_DELVURDERING
            WHERE vurderinger_id = ?
            ORDER BY relevant_dato
            """.trimIndent()
        ) {
            setParams { setLong(1, vurderingerId) }
            setRowMapper { row ->
                val vurdering = LovvalgDelvurderingJson.deserialiser(row.getInt("versjon"), row.getString("vurdering"))
                LovvalgDelvurderingerGrunnlag(
                    kanBehandlesAutomatisk = vurdering.kanBehandlesAutomatisk,
                    tilhørighetVurdering = vurdering.tilhørighetVurdering,
                    relevantDato = row.getLocalDate("relevant_dato"),
                    vurdertIBehandling = BehandlingId(row.getLong("vurdert_i_behandling")),
                )
            }
        }

    override fun lagre(behandlingId: BehandlingId, delvurderinger: List<LovvalgDelvurderingerGrunnlag>) {
        require(delvurderinger.isNotEmpty()) { "Må lagre minst én delvurdering" }
        require(delvurderinger.map { it.relevantDato }.distinct().size == delvurderinger.size) {
            "Kun én delvurdering per relevant dato"
        }

        val eksisterende = hentAlle(behandlingId)
        if (eksisterende == delvurderinger) return

        if (eksisterende.isNotEmpty()) {
            deaktiverGrunnlag(behandlingId)
        }

        val vurderingerId = connection.executeReturnKey("INSERT INTO LOVVALG_DELVURDERINGER DEFAULT VALUES")

        connection.execute(
            "INSERT INTO LOVVALG_DELVURDERING_GRUNNLAG (behandling_id, vurderinger_id) VALUES (?, ?)"
        ) {
            setParams {
                setLong(1, behandlingId.toLong())
                setLong(2, vurderingerId)
            }
            setResultValidator { require(it == 1) }
        }

        connection.executeBatch(
            """
            INSERT INTO LOVVALG_DELVURDERING
                (vurderinger_id, vurdert_i_behandling, versjon, vurdering, relevant_dato)
            VALUES (?, ?, ?, ?::jsonb, ?)
            """.trimIndent(),
            delvurderinger
        ) {
            setParams { delvurdering ->
                setLong(1, vurderingerId)
                setLong(2, delvurdering.vurdertIBehandling.toLong())
                setInt(3, LovvalgDelvurderingJson.versjon)
                setString(4, LovvalgDelvurderingJson.serialiser(delvurdering.vurdering()))
                setLocalDate(5, delvurdering.relevantDato)
            }
        }
    }

    override fun kopier(
        fraBehandling: BehandlingId,
        tilBehandling: BehandlingId
    ) {
        require(fraBehandling != tilBehandling)
        connection.execute(
            """
            INSERT INTO LOVVALG_DELVURDERING_GRUNNLAG (behandling_id, vurderinger_id)
            SELECT ?, vurderinger_id FROM LOVVALG_DELVURDERING_GRUNNLAG
            WHERE behandling_id = ? AND aktiv
            """.trimIndent()
        ) {
            setParams {
                setLong(1, tilBehandling.toLong())
                setLong(2, fraBehandling.toLong())
            }
        }
    }

    override fun slett(behandlingId: BehandlingId) {
        val vurderingerIds = connection.queryList(
            "SELECT vurderinger_id FROM LOVVALG_DELVURDERING_GRUNNLAG WHERE behandling_id = ?"
        ) {
            setParams { setLong(1, behandlingId.toLong()) }
            setRowMapper { it.getLong("vurderinger_id") }
        }
        connection.execute("DELETE FROM LOVVALG_DELVURDERING_GRUNNLAG WHERE behandling_id = ?") {
            setParams { setLong(1, behandlingId.toLong()) }
        }
        connection.execute(
            "DELETE FROM LOVVALG_DELVURDERING v WHERE v.vurderinger_id = ANY(?::bigint[])".trimIndent()
        ) {
            setParams { setLongArray(1, vurderingerIds) }
        }
        connection.execute(
            """
            DELETE FROM LOVVALG_DELVURDERINGER v WHERE v.id = ANY(?::bigint[])
            """.trimIndent()
        ) {
            setParams { setLongArray(1, vurderingerIds) }
        }
    }

    private fun deaktiverGrunnlag(behandlingId: BehandlingId) {
        connection.execute(
            "UPDATE LOVVALG_DELVURDERING_GRUNNLAG SET aktiv = FALSE WHERE behandling_id = ? AND aktiv"
        ) {
            setParams { setLong(1, behandlingId.toLong()) }
        }
    }
}
