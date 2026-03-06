package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.vedtakslengde

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.vedtakslengde.VedtakslengdeGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.vedtakslengde.VedtakslengdeRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.vedtakslengde.VedtakslengdeSluttdatoÅrsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.vedtakslengde.VedtakslengdeVurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.lookup.repository.Factory
import org.slf4j.LoggerFactory

class VedtakslengdeRepositoryImpl(private val connection: DBConnection) : VedtakslengdeRepository {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun lagre(
        behandlingId: BehandlingId,
        vurderinger: List<VedtakslengdeVurdering>
    ) {
        val eksisterendeGrunnlag = hentHvisEksisterer(behandlingId)
        val nyttGrunnlag = VedtakslengdeGrunnlag(vurderinger)

        if (eksisterendeGrunnlag != nyttGrunnlag) {
            eksisterendeGrunnlag?.let {
                deaktiverGrunnlag(behandlingId)
            }
            lagreGrunnlag(behandlingId, nyttGrunnlag)
        }
    }

    private fun lagreGrunnlag(
        behandlingId: BehandlingId,
        grunnlag: VedtakslengdeGrunnlag
    ) {
        val vurderingerId = lagreVurderinger(grunnlag.vurderinger)

        connection.executeReturnKey(
            """
            insert into vedtakslengde_grunnlag (
                behandling_id, vurderinger_id, aktiv
            ) values (?, ?, true)
            """.trimIndent()
        ) {
            setParams {
                setLong(1, behandlingId.toLong())
                setLong(2, vurderingerId)
            }
        }
    }

    private fun lagreVurderinger(vurderinger: List<VedtakslengdeVurdering>): Long {
        val vurderingerId = connection.executeReturnKey(
            "insert into vedtakslengde_vurderinger default values"
        )

        connection.executeBatch(
            """
            insert into vedtakslengde_vurdering (
                sluttdato, utvidet_med, vurdert_i_behandling, vurdert_av, opprettet, vurderinger_id, sluttdato_aarsak
            ) values (?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            vurderinger
        ) {
            setParams { vurdering ->
                setLocalDate(1, vurdering.sluttdato)
                setEnumName(2, vurdering.utvidetMed)
                setLong(3, vurdering.vurdertIBehandling.toLong())
                setString(4, vurdering.vurdertAv.ident)
                setInstant(5, vurdering.opprettet)
                setLong(6, vurderingerId)
                setArray(7, vurdering.sluttdatoÅrsak.map { it.name })
            }
        }

        return vurderingerId
    }

    override fun hentHvisEksisterer(behandlingId: BehandlingId): VedtakslengdeGrunnlag? {
        val vurderingerId: Long = connection.queryFirstOrNull(
            """
            select vurderinger_id from vedtakslengde_grunnlag
            where behandling_id = ? and aktiv
            """.trimIndent()
        ) {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setRowMapper { row -> row.getLong("vurderinger_id") }
        } ?: return null

        val vurderinger = hentVurderinger(vurderingerId)
        return VedtakslengdeGrunnlag(vurderinger)
    }

    private fun hentVurderinger(vurderingerId: Long): List<VedtakslengdeVurdering> {
        return connection.queryList(
            """
            select sluttdato, utvidet_med, vurdert_i_behandling, vurdert_av, opprettet, sluttdato_aarsak
            from vedtakslengde_vurdering
            where vurderinger_id = ?
            order by opprettet
            """.trimIndent()
        ) {
            setParams {
                setLong(1, vurderingerId)
            }
            setRowMapper { row ->
                VedtakslengdeVurdering(
                    sluttdato = row.getLocalDate("sluttdato"),
                    utvidetMed = row.getEnum("utvidet_med"),
                    vurdertIBehandling = BehandlingId(row.getLong("vurdert_i_behandling")),
                    vurdertAv = Bruker(row.getString("vurdert_av")),
                    opprettet = row.getInstant("opprettet"),
                    sluttdatoÅrsak = row.getArray("sluttdato_aarsak", String::class).map { VedtakslengdeSluttdatoÅrsak.valueOf(it) }
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

        val eksisterendeGrunnlag = hentHvisEksisterer(tilBehandling)
        if (eksisterendeGrunnlag != null) {
            deaktiverGrunnlag(tilBehandling)
        }

        connection.execute(
            """
                insert into vedtakslengde_grunnlag (behandling_id, vurderinger_id)
                select ?, vurderinger_id
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
        val vurderingerIds = connection.queryList(
            """
            select vurderinger_id from vedtakslengde_grunnlag where behandling_id = ?
            """.trimIndent()
        ) {
            setParams { setLong(1, behandlingId.toLong()) }
            setRowMapper { row -> row.getLong("vurderinger_id") }
        }

        val antallSlettedeRader = connection.executeReturnUpdated(
            "delete from vedtakslengde_grunnlag where behandling_id = ?"
        ) {
            setParams { setLong(1, behandlingId.toLong()) }
        }
        log.info("Slettet $antallSlettedeRader rader fra vedtakslengde_grunnlag for behandlingId=${behandlingId.toLong()}")

        if (vurderingerIds.isNotEmpty()) {
            val antallSlettedeVurderinger = connection.executeReturnUpdated(
                "delete from vedtakslengde_vurdering where vurderinger_id = ANY(?::bigint[])"
            ) {
                setParams { setLongArray(1, vurderingerIds) }
            }
            log.info("Slettet $antallSlettedeVurderinger rader fra vedtakslengde_vurdering for behandlingId=${behandlingId.toLong()}")

            connection.executeReturnUpdated(
                "delete from vedtakslengde_vurderinger where id = ANY(?::bigint[])"
            ) {
                setParams { setLongArray(1, vurderingerIds) }
            }
        }
    }

    companion object : Factory<VedtakslengdeRepositoryImpl> {
        override fun konstruer(connection: DBConnection): VedtakslengdeRepositoryImpl {
            return VedtakslengdeRepositoryImpl(connection)
        }
    }
}