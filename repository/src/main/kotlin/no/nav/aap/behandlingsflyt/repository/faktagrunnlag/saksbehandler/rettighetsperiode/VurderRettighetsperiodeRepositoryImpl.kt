package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.rettighetsperiode

import no.nav.aap.behandlingsflyt.behandling.rettighetsperiode.VurderRettighetsperiodeRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.rettighetsperiode.RettighetsperiodeVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.rettighetsperiode.RettighetsperiodeVurderingDTO
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.repository.Factory
import org.slf4j.LoggerFactory

class VurderRettighetsperiodeRepositoryImpl(private val connection: DBConnection) : VurderRettighetsperiodeRepository {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun lagreVurdering(behandlingId: BehandlingId, vurdering: RettighetsperiodeVurdering) {
        lagreGrunnlag(behandlingId, RettighetsperiodeGrunnlag(vurdering))
    }

    override fun hentVurdering(behandlingId: BehandlingId): RettighetsperiodeVurdering? {
        return hentHvisEksisterer(behandlingId)?.vurdering
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

        connection.execute(
            """
            insert into rettighetsperiode_vurdering
                (vurderinger_id, begrunnelse, start_dato, har_rett_utover_soknadsdato, har_krav_paa_renter, vurdert_av)
            values
                (?, ?, ?, ?, ?, ?)
            """.trimIndent()
        ) {
            setParams {
                setLong(1, vurderingerId)
                setString(2, grunnlag.vurdering.begrunnelse)
                setLocalDate(3, grunnlag.vurdering.startDato)
                setBoolean(4, grunnlag.vurdering.harRettUtoverSøknadsdato)
                setBoolean(5, grunnlag.vurdering.harKravPåRenter)
                setString(6, grunnlag.vurdering.vurdertAv)
            }
        }
    }

    override fun slett(behandlingId: BehandlingId) {

        val rettighetsPeriodeVurderingerIds = getRettighetsPeriodeVurderingerIds(behandlingId)

        val deletedRows = connection.executeReturnUpdated("""
            delete from rettighetsperiode_grunnlag where behandling_id = ?; 
            delete from rettighetsperiode_vurdering where vurderinger_id = ANY(?::bigint[]);
            delete from rettighetsperiode_vurderinger where id = ANY(?::bigint[]);
       
        """.trimIndent()) {
            setParams {
                setLong(1, behandlingId.id)
                setLongArray(2, rettighetsPeriodeVurderingerIds)
                setLongArray(3, rettighetsPeriodeVurderingerIds)
            }
        }
        log.info("Slettet $deletedRows fra rettighetsperiode_grunnlag")
    }

    private fun getRettighetsPeriodeVurderingerIds(behandlingId: BehandlingId): List<Long> = connection.queryList(
        """
                    SELECT vurderinger_id
                    FROM rettighetsperiode_grunnlag
                    WHERE behandling_id = ? AND vurderinger_id is not null
                 
                """.trimIndent()
    ) {
        setParams { setLong(1, behandlingId.id) }
        setRowMapper { row ->
            row.getLong("vurderinger_id")
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
            connection.queryFirst(
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
                        startDato = it.getLocalDateOrNull("start_dato"),
                        harRettUtoverSøknadsdato = it.getBoolean("har_rett_utover_soknadsdato"),
                        harKravPåRenter = it.getBooleanOrNull("har_krav_paa_renter"),
                        vurdertAv = it.getString("vurdert_av"),
                        vurdertDato = it.getLocalDateTime("opprettet")
                    )
                }
            }
        )
    }

    private fun deaktiverGrunnlag(behandlingId: BehandlingId) {
        connection.execute("UPDATE RETTIGHETSPERIODE_GRUNNLAG SET AKTIV = FALSE WHERE BEHANDLING_ID = ? AND AKTIV = TRUE") {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setResultValidator { require(it == 1) }
        }
    }

    private data class RettighetsperiodeGrunnlag(val vurdering: RettighetsperiodeVurdering)

    companion object : Factory<VurderRettighetsperiodeRepository> {
        override fun konstruer(connection: DBConnection): VurderRettighetsperiodeRepository {
            return VurderRettighetsperiodeRepositoryImpl(connection)
        }
    }

}