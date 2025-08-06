package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.søknad

import no.nav.aap.behandlingsflyt.behandling.søknad.TrukketSøknadRepository
import no.nav.aap.behandlingsflyt.behandling.søknad.TrukketSøknadVurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.lookup.repository.Factory
import no.nav.aap.verdityper.dokument.JournalpostId

class TrukketSøknadRepositoryImpl(
    private val connection: DBConnection,
) : TrukketSøknadRepository {

    override fun lagreTrukketSøknadVurdering(behandlingId: BehandlingId, vurdering: TrukketSøknadVurdering) {
        val eksisterendeGrunnlag = hentHvisEksisterer(behandlingId)
        val nyttGrunnlag = Grunnlag(eksisterendeGrunnlag?.vurderinger.orEmpty() + listOf(vurdering))
        lagreGrunnlag(behandlingId, nyttGrunnlag)
    }

    override fun hentTrukketSøknadVurderinger(behandlingId: BehandlingId): List<TrukketSøknadVurdering> {
        return hentHvisEksisterer(behandlingId)?.vurderinger.orEmpty()
    }

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        val eksisterendeGrunnlag = hentHvisEksisterer(fraBehandling) ?: return
        lagreGrunnlag(tilBehandling, eksisterendeGrunnlag)
    }

    private fun lagreGrunnlag(behandlingId: BehandlingId, grunnlag: Grunnlag) {
        connection.execute(
            """
            update trukket_soknad_grunnlag
            set aktiv = false
            where behandling_id = ?
        """.trimIndent()
        ) {
            setParams {
                setLong(1, behandlingId.toLong())
            }
        }

        val vurderingerId = connection.executeReturnKey(
            """insert into trukket_soknad_vurderinger default values"""
        )

        connection.execute(
            """
            insert into trukket_soknad_grunnlag(behandling_id, vurderinger_id) values (?, ?)
        """.trimIndent()
        ) {
            setParams {
                setLong(1, behandlingId.toLong())
                setLong(2, vurderingerId)
            }
        }

        connection.executeBatch(
            """
            insert into trukket_soknad_vurdering
                (vurderinger_id, journalpost_id, begrunnelse, vurdert_av, vurdert)
            values
                (?, ?, ?, ?, ?)
        """.trimIndent(), grunnlag.vurderinger
        ) {
            setParams { vurdering ->
                setLong(1, vurderingerId)
                setString(2, vurdering.journalpostId.identifikator)
                setString(3, vurdering.begrunnelse)
                setString(4, vurdering.vurdertAv.ident)
                setInstant(5, vurdering.vurdert)
            }
        }
    }

    override fun slett(behandlingId: BehandlingId) {
        // Sletting av trukketSøknad-tabellene skal ikke gjøres ved trekking av søknad
    }

    private class Grunnlag(
        val vurderinger: List<TrukketSøknadVurdering>,
    )

    private fun hentHvisEksisterer(behandlingId: BehandlingId): Grunnlag? {
        val vurderingerId = connection.queryFirstOrNull<Long>(
            """
            select vurderinger_id from trukket_soknad_grunnlag
            where behandling_id = ? and aktiv
            limit 1
        """.trimIndent()
        ) {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setRowMapper {
                it.getLong("vurderinger_id")
            }
        } ?: return null

        return Grunnlag(connection.queryList(
            """
            select vurdering.* 
            from trukket_soknad_vurdering vurdering
            join trukket_soknad_vurderinger vurderinger on vurderinger.id = vurdering.vurderinger_id
            where vurderinger.id = ?
            order by vurdering.vurdert
        """.trimIndent()
        ) {
            setParams {
                setLong(1, vurderingerId)
            }
            setRowMapper {
                TrukketSøknadVurdering(
                    journalpostId = JournalpostId(it.getString("journalpost_id")),
                    begrunnelse = it.getString("begrunnelse"),
                    vurdertAv = Bruker(it.getString("vurdert_av")),
                    vurdert = it.getInstant("vurdert"),
                )
            }
        })
    }

    companion object : Factory<TrukketSøknadRepository> {
        override fun konstruer(connection: DBConnection): TrukketSøknadRepository {
            return TrukketSøknadRepositoryImpl(connection)
        }
    }
}