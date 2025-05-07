package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.vilkårsresultat

import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Avslagsårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Innvilgelsesårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.LazyFaktaGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkår
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsresultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.lookup.repository.Factory
import org.slf4j.LoggerFactory
import java.time.LocalDate


class VilkårsresultatRepositoryImpl(private val connection: DBConnection) : VilkårsresultatRepository {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object : Factory<VilkårsresultatRepositoryImpl> {
        override fun konstruer(connection: DBConnection): VilkårsresultatRepositoryImpl {
            return VilkårsresultatRepositoryImpl(connection)
        }
    }

    override fun lagre(behandlingId: BehandlingId, vilkårsresultat: Vilkårsresultat) {
        val eksisterendeResultat = hentVilkårresultat(behandlingId)

        if (eksisterendeResultat != vilkårsresultat) {
            if (eksisterendeResultat != null) {
                deaktiverEksisterende(behandlingId)
            }
            val query = """
                INSERT INTO VILKAR_RESULTAT (behandling_id, aktiv) VALUES (?, ?)
            """.trimIndent()

            val resultatId = connection.executeReturnKey(query) {
                setParams {
                    setLong(1, behandlingId.toLong())
                    setBoolean(2, true)
                }
            }

            vilkårsresultat.alle().forEach { vilkår -> lagre(resultatId, vilkår) }
        } else {
            // Logg likhet og forkast ny versjon
            log.info("Forkastet lagring av nytt vilkårsresultat da disse anses som like")
        }
    }

    private fun lagre(resultatId: Long, vilkår: Vilkår) {
        val query = """
                INSERT INTO VILKAR (resultat_id, type) VALUES (?, ?)
            """.trimIndent()
        val vilkårId = connection.executeReturnKey(query) {
            setParams {
                setLong(1, resultatId)
                setEnumName(2, vilkår.type)
            }
        }
        val queryPeriode = """
                    INSERT INTO VILKAR_PERIODE (vilkar_id, periode, utfall, manuell_vurdering, begrunnelse, innvilgelsesarsak, avslagsarsak, faktagrunnlag, versjon) VALUES (?, ?::daterange, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent()
        connection.executeBatch(queryPeriode, vilkår.vilkårsperioder()) {
            setParams { periode ->
                setLong(1, vilkårId)
                setPeriode(2, periode.periode)
                setEnumName(3, periode.utfall)
                setBoolean(4, periode.manuellVurdering)
                setString(5, periode.begrunnelse)
                setEnumName(6, periode.innvilgelsesårsak)
                setEnumName(7, periode.avslagsårsak)
                setString(8, periode.faktagrunnlagSomString())
                setString(9, periode.versjon)
            }
        }
    }

    private fun deaktiverEksisterende(behandlingId: BehandlingId) {
        connection.execute("UPDATE VILKAR_RESULTAT set aktiv = false WHERE behandling_id = ? and aktiv = true") {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setResultValidator { require(it == 1) { "Oppdaterte $it, forventet 1." } }
        }
    }

    override fun hent(behandlingId: BehandlingId): Vilkårsresultat {
        val vilkårsresultat = hentVilkårresultat(behandlingId)

        if (vilkårsresultat != null) {
            return vilkårsresultat
        }

        return Vilkårsresultat()
    }

    private fun hentVilkårresultat(behandlingId: BehandlingId): Vilkårsresultat? {
        val query = """
SELECT vr.id as vr_id, vilkar.id as vilkar_id, *
FROM vilkar_resultat vr
         left join VILKAR on vr.id = VILKAR.resultat_id
         left JOIN lateral (SELECT vp.vilkar_id                              as vp_vilkar_id,
                                   json_agg(json_build_object('id', id, 'vilkar_id', vilkar_id,
                                                              'periode_fra',
                                                              lower(periode), 'periode_til',
                                                              upper(periode),
                                                              'utfall', utfall, 'manuell_vurdering',
                                                              manuell_vurdering, 'begrunnelse',
                                                              begrunnelse, 'innvilgelsesarsak',
                                                              innvilgelsesarsak, 'faktagrunnlag',
                                                              faktagrunnlag,
                                                              'versjon', versjon, 'avslagsarsak',
                                                              avslagsarsak)) as perioder
                            from vilkar_periode vp
                            where vp.vilkar_id = vilkar.id
                            group by vp.vilkar_id) vpp
                   on vpp.vp_vilkar_id = VILKAR.id
WHERE behandling_id = ?
  and aktiv = true
            """.trimIndent()

        var vilkårsresultatId: Long? = null
        val vilkårene = connection.queryList(query) {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setRowMapper { row ->
                vilkårsresultatId = row.getLong("vr_id")
                VilkårInternal(
                    id = row.getLong("vilkar_id"),
                    type = row.getEnum("type"),
                    perioder = row.getStringOrNull("perioder")?.let { DefaultJsonMapper.fromJson(it) } ?: emptyList()
                )
            }
        }

        if (vilkårsresultatId == null) return null

        val vilkår = vilkårene.map { vilkår -> mapOmTilVilkår(vilkår) }

        return Vilkårsresultat(id = vilkårsresultatId, vilkår = vilkår)
    }

    private fun mapOmTilVilkår(
        vilkår: VilkårInternal,
    ): Vilkår {
        return Vilkår(
            type = vilkår.type,
            vilkårsperioder = vilkår.perioder.map {
                Vilkårsperiode(
                    periode = Periode(it.periodeFra, it.periodeTil.minusDays(1)),
                    utfall = it.utfall,
                    manuellVurdering = it.manuellVurdering,
                    faktagrunnlag = LazyFaktaGrunnlag(connection = connection, periodeId = it.id),
                    begrunnelse = it.begrunnelse,
                    avslagsårsak = it.avslagsårsak,
                    innvilgelsesårsak = it.innvilgelsesårsak,
                    versjon = it.versjon
                )
            }.toSet()
        )
    }

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        val eksisterendeResultat = hent(fraBehandling)
        lagre(tilBehandling, eksisterendeResultat)
    }

    override fun slett(behandlingId: BehandlingId) {
        val resultatIds = getVilkarResultatIds(behandlingId)
        val vilkarIds = getVilkarIds(resultatIds)

        connection.execute("""
            delete from vilkar_periode where vilkar_id = ANY(?::bigint[]);
            delete from vilkar where resultat_id = ANY(?::bigint[]);
            delete from vilkar_resultat where behandling_id = ? 
        """.trimIndent()) {
            setParams {
                setLongArray(1, vilkarIds)
                setLongArray(2, resultatIds)
                setLong(3, behandlingId.id)
            }
        }
    }

    private fun getVilkarResultatIds(behandlingId: BehandlingId): List<Long> = connection.queryList(
        """
                    SELECT id
                    FROM vilkar_resultat
                    WHERE behandlingId = ?;
                 
                """.trimIndent()
    ) {
        setParams { setLong(1, behandlingId.id) }
        setRowMapper { row ->
            row.getLong("id")
        }
    }

    private fun getVilkarIds(resultatIds: List<Long>): List<Long> = connection.queryList(
        """
                    SELECT id
                    FROM vilkar
                    WHERE resultat_id = ANY(?::bigint[]);
                 
                """.trimIndent()
    ) {
        setParams { setLongArray(1, resultatIds ) }
        setRowMapper { row ->
            row.getLong("id")
        }
    }

    private class VilkårInternal(val id: Long, val type: Vilkårtype, val perioder: List<VilkårPeriodeInternal>)

    private class VilkårPeriodeInternal(
        val id: Long,
        @JsonProperty("vilkar_id") val vilkårId: Long,
        @JsonProperty("periode_fra") val periodeFra: LocalDate,
        @JsonProperty("periode_til") val periodeTil: LocalDate,
        val utfall: Utfall,
        @JsonProperty("manuell_vurdering") val manuellVurdering: Boolean = false,
        val begrunnelse: String?,
        @JsonProperty("innvilgelsesarsak") val innvilgelsesårsak: Innvilgelsesårsak? = null,
        @JsonProperty("avslagsarsak") val avslagsårsak: Avslagsårsak? = null,
        val versjon: String
    )
}