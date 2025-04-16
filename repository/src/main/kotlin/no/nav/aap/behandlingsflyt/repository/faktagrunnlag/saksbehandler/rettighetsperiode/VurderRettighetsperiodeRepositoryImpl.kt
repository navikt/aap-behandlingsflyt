package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.rettighetsperiode

import no.nav.aap.behandlingsflyt.behandling.rettighetsperiode.VurderRettighetsperiodeRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.rettighetsperiode.RettighetsperiodeEndringsårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.rettighetsperiode.RettighetsperiodeVurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.repository.Factory

class VurderRettighetsperiodeRepositoryImpl(private val connection: DBConnection) : VurderRettighetsperiodeRepository {
    override fun lagreVurdering(behandlingId: BehandlingId, vurdering: RettighetsperiodeVurdering) {
        lagreGrunnlag(behandlingId, RettighetsperiodeGrunnlag(listOf(vurdering)))
    }

    override fun hentVurderinger(behandlingId: BehandlingId): List<RettighetsperiodeVurdering> {
        return hentHvisEksisterer(behandlingId)?.vurderinger.orEmpty()
    }

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        val eksisterendeGrunnlag = hentHvisEksisterer(fraBehandling) ?: return
        lagreGrunnlag(tilBehandling, eksisterendeGrunnlag)
    }

    private fun lagreGrunnlag(behandlingId: BehandlingId, grunnlag: RettighetsperiodeGrunnlag) {
        val eksisterendeGrunnlag = hentHvisEksisterer(behandlingId)
        if (eksisterendeGrunnlag != null) {
            deaktiverGrunnlag(behandlingId)
        }

        val vurderingerId = connection.executeReturnKey(
            """insert into rettighetsperiode_vurderinger default values"""
        )

        connection.execute(
            """
            insert into rettighetsperiode_grunnlag(behandling_id, vurderinger_id) values (?, ?)
        """.trimIndent()
        ) {
            setParams {
                setLong(1, behandlingId.toLong())
                setLong(2, vurderingerId)
            }
        }

        connection.executeBatch(
            """
            insert into rettighetsperiode_vurdering
                (vurderinger_id, begrunnelse, start_dato, aarsak)
            values
                (?, ?, ?, ?)
        """.trimIndent(), grunnlag.vurderinger
        ) {
            setParams { vurdering ->
                setLong(1, vurderingerId)
                setString(2, vurdering.begrunnelse)
                setLocalDate(3, vurdering.startDato)
                setEnumName(4, vurdering.årsak)
            }
        }
    }


    private fun hentHvisEksisterer(behandlingId: BehandlingId): RettighetsperiodeGrunnlag? {
        val vurderingerId = connection.queryFirstOrNull<Long>(
            """
            select vurderinger_id from rettighetsperiode_grunnlag
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

        return RettighetsperiodeGrunnlag(
            connection.queryList(
                """
            select vurdering.* 
            from rettighetsperiode_vurdering vurdering
            join rettighetsperiode_vurderinger vurderinger on vurderinger.id = vurdering.vurderinger_id
            where vurderinger.id = ?
        """.trimIndent()
            ) {
                setParams {
                    setLong(1, vurderingerId)
                }
                setRowMapper {
                    RettighetsperiodeVurdering(
                        begrunnelse = it.getString("begrunnelse"),
                        startDato = it.getLocalDate("start_dato"),
                        årsak = it.getEnum<RettighetsperiodeEndringsårsak>("aarsak"),
                    )
                }
            })
    }

    private fun deaktiverGrunnlag(behandlingId: BehandlingId) {
        connection.execute("UPDATE RETTIGHETSPERIODE_GRUNNLAG SET AKTIV = FALSE WHERE BEHANDLING_ID = ? AND AKTIV = TRUE") {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setResultValidator { require(it == 1) }
        }
    }

    private data class RettighetsperiodeGrunnlag(val vurderinger: List<RettighetsperiodeVurdering>)

    companion object : Factory<VurderRettighetsperiodeRepository> {
        override fun konstruer(connection: DBConnection): VurderRettighetsperiodeRepository {
            return VurderRettighetsperiodeRepositoryImpl(connection)
        }
    }

}