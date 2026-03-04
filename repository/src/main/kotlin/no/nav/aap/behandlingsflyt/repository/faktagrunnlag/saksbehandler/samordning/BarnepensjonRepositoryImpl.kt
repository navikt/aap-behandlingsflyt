package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.samordning

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.samordning.barnepensjon.BarnepensjonGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.samordning.barnepensjon.BarnepensjonRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.samordning.barnepensjon.BarnepensjonVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.samordning.barnepensjon.BarnepensjonPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.komponenter.verdityper.GUnit
import no.nav.aap.lookup.repository.Factory
import org.slf4j.LoggerFactory

class BarnepensjonRepositoryImpl(private val connection: DBConnection) : BarnepensjonRepository {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun lagre(behandlingId: BehandlingId, vurdering: BarnepensjonVurdering) {
        val eksisterendeGrunnlag = hentHvisEksisterer(behandlingId)
        val nyttGrunnlag = BarnepensjonGrunnlag(vurdering = vurdering)

        if (eksisterendeGrunnlag != nyttGrunnlag) {
            eksisterendeGrunnlag?.let { deaktiverGrunnlag(behandlingId) }
            lagreGrunnlag(behandlingId, nyttGrunnlag)
        }
    }

    private fun lagreGrunnlag(behandlingId: BehandlingId, grunnlag: BarnepensjonGrunnlag) {
        val vurderingId = lagreVurdering(grunnlag.vurdering)

        connection.execute(
            """
            insert into samordning_barnepensjon_grunnlag (
                behandling_id, vurdering_id, aktiv
            ) values (?, ?, true)
            """.trimIndent()
        ) {
            setParams {
                setLong(1, behandlingId.toLong())
                setLong(2, vurderingId)
            }
        }

        lagrePerioder(vurderingId, grunnlag.vurdering.perioder)
    }

    private fun lagreVurdering(vurdering: BarnepensjonVurdering): Long {
        return connection.executeReturnKey(
            """
            insert into samordning_barnepensjon_vurdering (
                begrunnelse, vurdert_i_behandling, vurdert_av_ident, opprettet
            ) values (?, ?, ?, ?)
            """.trimIndent()
        ) {
            setParams {
                setString(1, vurdering.begrunnelse)
                setLong(2, vurdering.vurdertIBehandling.id)
                setString(3, vurdering.vurdertAv.ident)
                setInstant(4, vurdering.opprettet)
            }
        }
    }

    private fun lagrePerioder(vurderingId: Long, perioder: Set<BarnepensjonPeriode>) {
        connection.executeBatch(
            """
            insert into samordning_barnepensjon_vurdering_periode (
                vurdering_id, periode, grunnbelop
            ) values (?, ?::daterange, ?)
            """.trimIndent(),
            perioder.toList()
        ) {
            setParams {
                setLong(1, vurderingId)
                setPeriode(2, it.periode)
                setBigDecimal(3, it.grunnbeløp.verdi())
            }
        }
    }

    override fun hentHvisEksisterer(behandlingId: BehandlingId): BarnepensjonGrunnlag? {
        val vurderingId = hentVurderingId(behandlingId) ?: return null
        val perioder = hentPerioder(vurderingId)

        return connection.queryFirstOrNull(
            """
            select 
                begrunnelse,
                vurdert_i_behandling,
                vurdert_av_ident,
                opprettet
            from samordning_barnepensjon_vurdering
            where id = ?
            """.trimIndent()
        ) {
            setParams { setLong(1, vurderingId) }
            setRowMapper { row ->
                val vurdering = BarnepensjonVurdering(
                    begrunnelse = row.getString("begrunnelse"),
                    perioder = perioder.toSet(),
                    vurdertIBehandling = BehandlingId(row.getLong("vurdert_i_behandling")),
                    vurdertAv = Bruker(row.getString("vurdert_av_ident")),
                    opprettet = row.getInstant("opprettet"),
                )
                BarnepensjonGrunnlag(vurdering = vurdering)
            }
        }
    }

    private fun hentVurderingId(behandlingId: BehandlingId): Long? {
        return connection.queryFirstOrNull(
            """
            select vurdering_id
            from samordning_barnepensjon_grunnlag
            where behandling_id = ? and aktiv
            """.trimIndent()
        ) {
            setParams { setLong(1, behandlingId.toLong()) }
            setRowMapper { row -> row.getLong("vurdering_id") }
        }
    }

    private fun hentPerioder(vurderingId: Long): List<BarnepensjonPeriode> {
        return connection.queryList(
            """
            select periode, grunnbelop
            from samordning_barnepensjon_vurdering_periode
            where vurdering_id = ?
            """.trimIndent()
        ) {
            setParams { setLong(1, vurderingId) }
            setRowMapper { row ->
                BarnepensjonPeriode(
                    periode = row.getPeriode("periode"),
                    grunnbeløp = GUnit(row.getBigDecimal("grunnbelop"))
                )
            }
        }
    }

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        require(fraBehandling != tilBehandling)
        hentHvisEksisterer(fraBehandling) ?: return

        connection.execute(
            """
            insert into samordning_barnepensjon_grunnlag (behandling_id, vurdering_id)
            select ?, vurdering_id
            from samordning_barnepensjon_grunnlag
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
        if (vurderingIder.isEmpty()) return

        val antallSlettedeRader = connection.executeReturnUpdated(
            """
            delete from samordning_barnepensjon_grunnlag where behandling_id = ?;
            delete from samordning_barnepensjon_vurdering_periode where vurdering_id = any(?::bigint[]);
            delete from samordning_barnepensjon_vurdering where id = any(?::bigint[]);
            """.trimIndent()
        ) {
            setParams {
                setLong(1, behandlingId.id)
                setLongArray(2, vurderingIder)
                setLongArray(3, vurderingIder)
            }
        }
        log.info("Slettet $antallSlettedeRader rader fra samordning_barnepensjon_grunnlag")
    }

    private fun hentVurderingIderForAlleGrunnlag(behandlingId: BehandlingId): List<Long> {
        return connection.queryList(
            """
            select vurdering_id
            from samordning_barnepensjon_grunnlag
            where behandling_id = ? and vurdering_id is not null
            """.trimIndent()
        ) {
            setParams { setLong(1, behandlingId.id) }
            setRowMapper { row -> row.getLong("vurdering_id") }
        }
    }

    override fun deaktiverGrunnlag(behandlingId: BehandlingId) {
        connection.execute("update samordning_barnepensjon_grunnlag set aktiv = false where behandling_id = ? and aktiv = true") {
            setParams { setLong(1, behandlingId.toLong()) }
            setResultValidator { require(it == 1) }
        }
    }

    companion object : Factory<BarnepensjonRepositoryImpl> {
        override fun konstruer(connection: DBConnection): BarnepensjonRepositoryImpl {
            return BarnepensjonRepositoryImpl(connection)
        }
    }
}
