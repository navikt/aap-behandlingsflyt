package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.student

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.sykestipend.SykestipendGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.sykestipend.SykestipendRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.sykestipend.SykestipendVurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.repository.Factory
import org.slf4j.LoggerFactory

class SykestipendRepositoryImpl(private val connection: DBConnection) : SykestipendRepository {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun lagre(
        behandlingId: BehandlingId,
        vurdering: SykestipendVurdering
    ) {
        val eksisterendeGrunnlag = hentHvisEksisterer(behandlingId)
        val nyttGrunnlag = SykestipendGrunnlag(
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
        grunnlag: SykestipendGrunnlag
    ) {
        val vurderingId = lagre(grunnlag.vurdering)

        connection.execute(
            """
            insert into sykestipend_grunnlag (
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

    private fun lagre(vurdering: SykestipendVurdering): Long {
        return connection.executeReturnKey(
            """
            insert into sykestipend_vurdering (
                begrunnelse, perioder, vurdert_i_behandling, vurdert_av_ident, opprettet
            ) values (?, ?, ?, ?, ?)
            """.trimIndent()
        ) {
            setParams {
                setString(1, vurdering.begrunnelse)
                setPeriodeArray(2, vurdering.perioder.toList())
                setLong(3, vurdering.vurdertIBehandling.id)
                setString(4, vurdering.vurdertAv.ident)
                setInstant(5, vurdering.opprettet)
            }
        }
    }

    override fun hentHvisEksisterer(behandlingId: BehandlingId): SykestipendGrunnlag? {
        return connection.queryFirstOrNull(
            """
            select 
                v.begrunnelse,
                v.perioder,
                v.vurdert_i_behandling,
                v.vurdert_av_ident,
                v.opprettet
            from sykestipend_grunnlag g
            join sykestipend_vurdering v on g.vurdering_id = v.id
            where g.behandling_id = ? and g.aktiv
            """.trimIndent()
        ) {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setRowMapper { row ->
                val vurdering = SykestipendVurdering(
                    begrunnelse = row.getString("begrunnelse"),
                    perioder = row.getPeriodeArray("perioder").toSet(),
                    vurdertIBehandling = BehandlingId(row.getLong("vurdert_i_behandling")),
                    vurdertAv = no.nav.aap.komponenter.verdityper.Bruker(row.getString("vurdert_av_ident")),
                    opprettet = row.getInstant("opprettet"),
                )
                SykestipendGrunnlag(
                    vurdering = vurdering
                )
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
                insert into sykestipend_grunnlag (behandling_id, vurdering_id)
                select ?, vurdering_id
                from sykestipend_grunnlag 
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
        val vurderingIder = hentVurderingIderForAlleGrunnlag(behandlingId)
        val antallSlettedeRader = connection.executeReturnUpdated(
            """
            delete from sykestipend_grunnlag where behandling_id = ?;
            delete from sykestipend_vurdering where id = any(?::bigint[]);
        """.trimIndent()) {
            setParams {
                setLong(1, behandlingId.id)
                setLongArray(2, vurderingIder)
            }
        }
        log.info("Slettet $antallSlettedeRader rader fra sykestipend_grunnlag")

    }
    
    private fun hentVurderingIderForAlleGrunnlag(behandlingId: BehandlingId): List<Long> {
        return connection.queryList(
            """
                    SELECT vurdering_id
                    FROM sykestipend_grunnlag
                    WHERE behandling_id = ? AND vurdering_id is not null
                 
                """.trimIndent()
        ) {
            setParams { setLong(1, behandlingId.id) }
            setRowMapper { row ->
                row.getLong("vurdering_id")
            }
        }
    }

    override fun deaktiverGrunnlag(behandlingId: BehandlingId) {
        connection.execute("update sykestipend_grunnlag set aktiv = false where behandling_id = ? and aktiv = true") {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setResultValidator { require(it == 1) }
        }
    }

    companion object : Factory<SykestipendRepositoryImpl> {
        override fun konstruer(connection: DBConnection): SykestipendRepositoryImpl {
            return SykestipendRepositoryImpl(connection)
        }
    }

}