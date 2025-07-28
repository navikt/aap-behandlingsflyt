package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.klage

import no.nav.aap.behandlingsflyt.behandling.trekkklage.TrekkKlageGrunnlag
import no.nav.aap.behandlingsflyt.behandling.trekkklage.TrekkKlageRepository
import no.nav.aap.behandlingsflyt.behandling.trekkklage.TrekkKlageVurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.httpklient.auth.Bruker
import no.nav.aap.lookup.repository.Factory

class TrekkKlageRepositoryImpl(
    private val connection: DBConnection
): TrekkKlageRepository {
    override fun lagreTrekkKlageVurdering(
        behandlingId: BehandlingId,
        vurdering: TrekkKlageVurdering
    ) {
        settGamleGrunnlagTilInaktive(behandlingId)
        val vurderingId = lagreVurdering(vurdering = vurdering)
        lagreGrunnlag(behandlingId = behandlingId, vurderingId = vurderingId)
    }

    override fun hentTrekkKlageGrunnlag(behandlingId: BehandlingId): TrekkKlageGrunnlag? {
        return connection.queryFirstOrNull<TrekkKlageGrunnlag>(
            """
                select *
                from trekk_klage_grunnlag as grunnlag
                left join trekk_klage_vurdering as vurdering on grunnlag.vurdering_id = vurdering.id
                where grunnlag.aktiv = true and grunnlag.behandling_id = ?
            """.trimIndent()
        ) {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setRowMapper {
                TrekkKlageGrunnlag(
                    vurdering = TrekkKlageVurdering(
                        begrunnelse = it.getString("begrunnelse"),
                        skalTrekkes = it.getBoolean("skal_trekkes"),
                        hvorforTrekkes = it.getEnumOrNull("hvorfor_trekkes"),
                        vurdertAv = Bruker(ident = it.getString("vurdert_av")),
                        vurdert = it.getInstant("opprettet_tid")
                    )
                )
            }
        }
    }

    private fun settGamleGrunnlagTilInaktive(behandlingId: BehandlingId) {
        return connection.execute("""
            UPDATE trekk_klage_grunnlag
            SET aktiv = false
            WHERE behandling_id = ?
        """.trimIndent()) {
            setParams {
                setLong(1, behandlingId.toLong())
            }
        }
    }

    private fun lagreVurdering(vurdering: TrekkKlageVurdering): Long  {
        return connection.executeReturnKey("""
            INSERT INTO trekk_klage_vurdering(
                skal_trekkes, hvorfor_trekkes, begrunnelse, vurdert_av
            ) VALUES (?, ?, ?, ?)
        """.trimIndent()) {
            setParams {
                setBoolean(1, vurdering.skalTrekkes)
                setEnumName(2, vurdering.hvorforTrekkes)
                setString(3, vurdering.begrunnelse)
                setString(4, vurdering.vurdertAv.ident)
            }
        }
    }

    private fun lagreGrunnlag(behandlingId: BehandlingId, vurderingId: Long): Long {
        return connection.executeReturnKey("""
            INSERT INTO trekk_klage_grunnlag(
                BEHANDLING_ID, VURDERING_ID, AKTIV
            ) VALUES (?, ?, TRUE)
        """.trimIndent()) {
            setParams {
                setLong(1, behandlingId.toLong())
                setLong(2, vurderingId)
            }
        }
    }

    override fun kopier(
        fraBehandling: BehandlingId,
        tilBehandling: BehandlingId
    ) {
    }

    override fun slett(behandlingId: BehandlingId) {
        // Ska likke slette klage-årsak når klage trekkes
    }

    companion object : Factory<TrekkKlageRepository> {
        override fun konstruer(connection: DBConnection): TrekkKlageRepository {
            return TrekkKlageRepositoryImpl(connection)
        }
    }
}