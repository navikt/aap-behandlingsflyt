package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.søknad

import no.nav.aap.behandlingsflyt.behandling.kansellerrevurdering.KansellerRevurderingGrunnlag
import no.nav.aap.behandlingsflyt.behandling.kansellerrevurdering.KansellerRevurderingRepository
import no.nav.aap.behandlingsflyt.behandling.kansellerrevurdering.KansellerRevurderingVurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.lookup.repository.Factory

class KansellerRevurderingRepositoryImpl(
    private val connection: DBConnection,
) : KansellerRevurderingRepository {

    override fun lagreKansellerRevurderingVurdering(
        behandlingId: BehandlingId,
        vurdering: KansellerRevurderingVurdering
    ) {
        settGamleGrunnlagTilInaktive(behandlingId)
        val vurderingId = lagreVurdering(vurdering)
        lagreGrunnlag(behandlingId, vurderingId)
    }

    override fun hentKansellertRevurderingGrunnlag(behandlingId: BehandlingId): KansellerRevurderingGrunnlag? {
        return connection.queryFirstOrNull<KansellerRevurderingGrunnlag>(
            """
                select *
                from kanseller_revurdering_grunnlag as grunnlag
                left join kanseller_revurdering_vurdering as vurdering on grunnlag.id = vurdering.id
                where grunnlag.aktiv = true and grunnlag.behandling_id = ?
            """.trimIndent()
        ) {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setRowMapper {
                KansellerRevurderingGrunnlag(
                    vurdering = KansellerRevurderingVurdering(
                        årsak = it.getEnumOrNull("aarsak"),
                        begrunnelse = it.getString("begrunnelse"),
                        vurdertAv = Bruker(ident = it.getString("vurdert_av"))
                    )
                )
            }
        }
    }

    private fun settGamleGrunnlagTilInaktive(behandlingId: BehandlingId) {
        return connection.execute("""
            update kanseller_revurdering_grunnlag
            set aktiv = false
            where behandling_id = ?
        """.trimIndent()
        ) {
            setParams {
                setLong(1, behandlingId.toLong())
            }
        }
    }

    private fun lagreVurdering(vurdering: KansellerRevurderingVurdering): Long {
        return connection.executeReturnKey("""
            insert into kanseller_revurdering_vurdering (aarsak, begrunnelse, vurdert_av)
            values (?, ?, ?)
        """.trimIndent()
        ) {
            setParams {
                setEnumName(1, vurdering.årsak)
                setString(2, vurdering.begrunnelse)
                setString(3, vurdering.vurdertAv.ident)
            }
        }
    }

    private fun lagreGrunnlag(behandlingId: BehandlingId, vurdering: Long): Long {
        return connection.executeReturnKey("""
            insert into kanseller_revurdering_grunnlag(
                BEHANDLING_ID, VURDERING_ID, AKTIV
            ) values (?, ?, TRUE)
        """.trimIndent()) {
            setParams {
                setLong(1, behandlingId.toLong())
                setLong(2, vurdering)
            }
        }
    }

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        // Gjør ingenting
    }

    override fun slett(behandlingId: BehandlingId) {
        // Gjør ingenting
    }

    companion object : Factory<KansellerRevurderingRepository> {
        override fun konstruer(connection: DBConnection): KansellerRevurderingRepository {
            return KansellerRevurderingRepositoryImpl(connection)
        }
    }
}