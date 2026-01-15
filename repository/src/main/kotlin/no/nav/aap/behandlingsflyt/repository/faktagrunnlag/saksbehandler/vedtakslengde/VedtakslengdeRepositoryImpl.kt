package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.vedtakslengde

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.vedtakslengde.VedtakslengdeGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.vedtakslengde.VedtakslengdeRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.vedtakslengde.VedtakslengdeVurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.repository.Factory
import org.slf4j.LoggerFactory

class VedtakslengdeRepositoryImpl(private val connection: DBConnection) : VedtakslengdeRepository {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun lagre(
        behandlingId: BehandlingId,
        vurdering: VedtakslengdeVurdering
    ) {
        val eksisterendeGrunnlag = hentHvisEksisterer(behandlingId)
        val nyttGrunnlag = VedtakslengdeGrunnlag(
            vurdering = vurdering
        )

        if (eksisterendeGrunnlag != nyttGrunnlag) {
            eksisterendeGrunnlag?.let {
                deaktiverGrunnlag(behandlingId)
            }
            lagre(behandlingId, nyttGrunnlag)
        }
    }

    private fun lagre(
        behandlingId: BehandlingId,
        grunnlag: VedtakslengdeGrunnlag
    ) {
        val vurderingId = lagre(grunnlag.vurdering)

        connection.executeReturnKey(
            """
            insert into vedtakslengde_grunnlag (
                behandling_id, vurdering_id, aktiv
            ) values (?, ?, true)
            """.trimIndent()
        ) {
            setParams {
                setLong(1, behandlingId.toLong())
                setLong(2, vurderingId)
            }
        }
    }

    private fun lagre(vurdering: VedtakslengdeVurdering): Long {
        return connection.executeReturnKey(
            """
            insert into vedtakslengde_vurdering (
                sluttdato, utvidet_med, vurdert_i_behandling, vurdert_av, opprettet
            ) values (?, ?, ?, ?, ?)
            """.trimIndent()
        ) {
            setParams {
                setLocalDate(1, vurdering.sluttdato)
                setEnumName(2, vurdering.utvidetMed)
                setLong(3, vurdering.vurdertIBehandling.toLong())
                setString(4, vurdering.vurdertAv)
                setInstant(5, vurdering.opprettet)
            }
        }
    }

    override fun hentHvisEksisterer(behandlingId: BehandlingId): VedtakslengdeGrunnlag? {
        return connection.queryFirstOrNull(
            """
            select 
                v.sluttdato,
                v.utvidet_med,
                v.vurdert_i_behandling,
                v.vurdert_av,
                v.opprettet
                from vedtakslengde_grunnlag g
                join vedtakslengde_vurdering v on g.vurdering_id = v.id
            where g.behandling_id = ? and g.aktiv
            """.trimIndent()
        ) {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setRowMapper { row ->
                VedtakslengdeGrunnlag(
                    vurdering = VedtakslengdeVurdering(
                        sluttdato = row.getLocalDate("sluttdato"),
                        utvidetMed = row.getEnum("utvidet_med"),
                        vurdertIBehandling = BehandlingId(row.getLong("vurdert_i_behandling")),
                        vurdertAv = row.getString("vurdert_av"),
                        opprettet = row.getInstant("opprettet")
                    )
                )
            }
        }
    }

    override fun deaktiverGrunnlag(behandlingId: BehandlingId) {
        connection.execute(
            """
            update vedtakslengde_grunnlag
            set aktiv = false
            where behandling_id = ? and aktiv
            """.trimIndent()
        ) {
            setParams {
                setLong(1, behandlingId.toLong())
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
                insert into vedtakslengde_grunnlag (behandling_id, vurdering_id)
                select ?, vurdering_id
                from vedtakslengde_grunnlag 
                where behandling_id = ? and aktiv
            """.trimIndent()
        ) {
            setParams {
                setLong(1, tilBehandling.toLong())
                setLong(2, fraBehandling.toLong())
            }
        }
    }

    override fun slett(behandlingId: BehandlingId) {
        val antallSlettedeRader = connection.executeReturnUpdated(
            """
            delete from vedtakslengde_grunnlag
            where behandling_id = ?
            """.trimIndent()
        ) {
            setParams {
                setLong(1, behandlingId.toLong())
            }
        }
        log.info("Slettet $antallSlettedeRader rader fra vedtakslengde_grunnlag for behandlingId=${behandlingId.toLong()}")
        val antallSlettedeVurderinger = connection.executeReturnUpdated(
            """
                delete from vedtakslengde_vurdering
                where vurdert_i_behandling = ?
                """.trimIndent()
        ) {
            setParams {
                setLong(1, behandlingId.toLong())
            }
        }
        log.info("Slettet $antallSlettedeVurderinger rader fra vedtakslengde_vurdering for behandlingId=${behandlingId.toLong()}")
    }

    companion object : Factory<VedtakslengdeRepositoryImpl> {
        override fun konstruer(connection: DBConnection): VedtakslengdeRepositoryImpl {
            return VedtakslengdeRepositoryImpl(connection)
        }
    }
}