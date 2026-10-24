package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.medlemskaplovvalg

import no.nav.aap.behandlingsflyt.behandling.vilkår.medlemskap.KanBehandlesAutomatiskVurdering
import no.nav.aap.behandlingsflyt.behandling.vilkår.medlemskap.LovvalgDelvurderingerGrunnlag
import no.nav.aap.behandlingsflyt.behandling.vilkår.medlemskap.LovvalgDelvurderingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.repository.Factory

class LovvalgDelvurderingRepositoryImpl(private val connection: DBConnection) : LovvalgDelvurderingRepository {
    companion object : Factory<LovvalgDelvurderingRepositoryImpl> {
        override fun konstruer(connection: DBConnection) = LovvalgDelvurderingRepositoryImpl(connection)
    }

    override fun hentBasertPåDelvurderingId(delvurderingId: Long): LovvalgDelvurderingerGrunnlag? {
        return connection.queryFirstOrNull(
            """
            SELECT vurderinger_id FROM LOVVALG_DELVURDERING WHERE id = ?
            """.trimIndent()
        ) {
            setParams { setLong(1, delvurderingId) }
            setRowMapper {
                mapVurdering(it.getLong("vurderinger_id"))
            }
        }
    }
    override fun hentHvisEksisterer(behandlingId: BehandlingId): LovvalgDelvurderingerGrunnlag? {
        return connection.queryFirstOrNull(
            """
            SELECT vurderinger_id FROM LOVVALG_DELVURDERING_GRUNNLAG
            WHERE behandling_id = ? AND aktiv
            """.trimIndent()
        ) {
            setParams { setLong(1, behandlingId.toLong()) }
            setRowMapper {
                mapVurdering(it.getLong("vurderinger_id"))
            }
        }
    }

    private fun mapVurdering(vurderingerId: Long): LovvalgDelvurderingerGrunnlag =
        connection.queryFirst(
            """
            SELECT id, versjon, vurdering FROM LOVVALG_DELVURDERING
            WHERE vurderinger_id = ?
            """.trimIndent()
        ) {
            setParams { setLong(1, vurderingerId) }
            setRowMapper { row ->
                val vurdering = LovvalgDelvurderingJson.deserialiser(row.getInt("versjon"), row.getString("vurdering"))
                LovvalgDelvurderingerGrunnlag(
                    kanBehandlesAutomatisk = vurdering.kanBehandlesAutomatisk,
                    tilhørighetVurdering = vurdering.tilhørighetVurdering,
                    periode = vurdering.periode,
                    delvurderingId = row.getLong("id")
                )
            }
        }

    override fun lagre(behandlingId: BehandlingId, vurdering: KanBehandlesAutomatiskVurdering) {
        val eksisterende = hentHvisEksisterer(behandlingId)
        if (eksisterende?.vurdering() != vurdering) {
            if (eksisterende != null) {
                deaktiverGrunnlag(behandlingId)
            }

            val json = LovvalgDelvurderingJson.serialiser(vurdering)
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

            connection.execute(
                """
            INSERT INTO LOVVALG_DELVURDERING (vurderinger_id, vurdert_i_behandling, versjon, vurdering)
            VALUES (?, ?, ?, ?::jsonb)
            """.trimIndent()
            ) {
                setParams {
                    setLong(1, vurderingerId)
                    setLong(2, behandlingId.toLong())
                    setInt(3, LovvalgDelvurderingJson.versjon)
                    setString(4, json)
                }
                setResultValidator { require(it == 1) }
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
