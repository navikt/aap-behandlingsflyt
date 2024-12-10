package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.vilkårsresultat

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Avslagsårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Faktagrunnlag
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
import no.nav.aap.komponenter.dbconnect.Row
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.lookup.repository.Factory
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(VilkårsresultatRepositoryImpl::class.java)

class VilkårsresultatRepositoryImpl(private val connection: DBConnection) : VilkårsresultatRepository {

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
            setResultValidator { require(it == 1) }
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
                SELECT * FROM VILKAR_RESULTAT WHERE behandling_id = ? AND aktiv = true
            """.trimIndent()

        return connection.queryFirstOrNull(query) {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setRowMapper(::mapResultat)
        }
    }

    private fun mapResultat(row: Row): Vilkårsresultat {
        val id = row.getLong("id")
        return Vilkårsresultat(id = id, vilkår = hentVilkår(id))
    }

    private fun hentVilkår(id: Long): List<Vilkår> {
        val query = """
            SELECT * FROM VILKAR WHERE resultat_id = ?
        """.trimIndent()

        val vilkårene = connection.queryList(query) {
            setParams {
                setLong(1, id)
            }
            setRowMapper(::mapVilkår)
        }

        val perioderQuery = """
            SELECT * FROM VILKAR_PERIODE WHERE vilkar_id = ANY(?::bigint[])
        """.trimIndent()

        val periodene = connection.queryList(perioderQuery) {
            setParams {
                setArray(1, vilkårene.map { vilkår -> "${vilkår.id}" })
            }
            setRowMapper(::mapPerioder)
        }

        return vilkårene.map { vilkår -> mapOmTilVilkår(vilkår, periodene) }
    }

    private fun mapOmTilVilkår(
        vilkår: VilårInternal,
        perioder: List<VilkårPeriodeInternal>
    ): Vilkår {
        val relevantePerioder = perioder.filter { periode -> periode.vilkårId == vilkår.id }.map {
            Vilkårsperiode(
                periode = it.periode,
                utfall = it.utfall,
                manuellVurdering = it.manuellVurdering,
                faktagrunnlag = LazyFaktaGrunnlag(connection = connection, periodeId = it.id),
                begrunnelse = it.begrunnelse,
                avslagsårsak = it.avslagsårsak,
                innvilgelsesårsak = it.innvilgelsesårsak,
                versjon = it.versjon
            )
        }

        return Vilkår(vilkår.type, relevantePerioder.toSet())
    }

    private fun mapVilkår(row: Row): VilårInternal {
        return VilårInternal(
            id = row.getLong("id"),
            type = row.getEnum("type"),
        )
    }

    private fun mapPerioder(row: Row): VilkårPeriodeInternal {
        return VilkårPeriodeInternal(
            id = row.getLong("id"),
            vilkårId = row.getLong("vilkar_id"),
            periode = row.getPeriode("periode"),
            utfall = row.getEnum("utfall"),
            manuellVurdering = row.getBoolean("manuell_vurdering"),
            faktagrunnlag = LazyFaktaGrunnlag(connection = connection, periodeId = row.getLong("id")),
            begrunnelse = row.getStringOrNull("begrunnelse"),
            avslagsårsak = row.getEnumOrNull("avslagsarsak"),
            innvilgelsesårsak = row.getEnumOrNull("innvilgelsesarsak"),
            versjon = row.getString("versjon")
        )
    }

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        val eksisterendeResultat = hent(fraBehandling)
        lagre(tilBehandling, eksisterendeResultat)
    }

    internal class VilårInternal(val id: Long, val type: Vilkårtype)

    internal class VilkårPeriodeInternal(
        val id: Long,
        val vilkårId: Long,
        val periode: Periode,
        val utfall: Utfall,
        val manuellVurdering: Boolean = false,
        val begrunnelse: String?,
        val innvilgelsesårsak: Innvilgelsesårsak? = null,
        val avslagsårsak: Avslagsårsak? = null,
        val faktagrunnlag: Faktagrunnlag?,
        val versjon: String
    )
}