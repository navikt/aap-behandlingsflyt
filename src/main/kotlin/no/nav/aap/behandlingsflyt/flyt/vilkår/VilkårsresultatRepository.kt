package no.nav.aap.behandlingsflyt.flyt.vilkår

import no.nav.aap.behandlingsflyt.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.dbconnect.Row
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(VilkårsresultatRepository::class.java)

class VilkårsresultatRepository(private val connection: DBConnection) {

    fun lagre(behandlingId: BehandlingId, vilkårsresultat: Vilkårsresultat) {
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
        vilkår.vilkårsperioder().forEach { periode -> lagre(vilkårId, periode) }
    }

    private fun lagre(vilkårId: Long, periode: Vilkårsperiode) {
        val query = """
                INSERT INTO VILKAR_PERIODE (vilkar_id, periode, utfall, manuell_vurdering, begrunnelse, innvilgelsesarsak, avslagsarsak, faktagrunnlag, versjon) VALUES (?, ?::daterange, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()
        connection.execute(query) {
            setParams {
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

    fun hent(behandlingId: BehandlingId): Vilkårsresultat {
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

        return connection.queryList(query) {
            setParams {
                setLong(1, id)
            }
            setRowMapper(::mapVilkår)
        }
    }

    private fun mapVilkår(row: Row): Vilkår {
        val id = row.getLong("id")
        return Vilkår(
            type = row.getEnum("type"),
            vilkårsperioder = hentPerioder(id)
        )
    }

    private fun hentPerioder(id: Long): Set<Vilkårsperiode> {
        val query = """
            SELECT * FROM VILKAR_PERIODE WHERE vilkar_id = ?
        """.trimIndent()

        return connection.queryList(query) {
            setParams {
                setLong(1, id)
            }
            setRowMapper(::mapPerioder)
        }.toSet()
    }

    private fun mapPerioder(row: Row): Vilkårsperiode {
        return Vilkårsperiode(
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

    fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        val eksisterendeResultat = hent(fraBehandling)
        lagre(tilBehandling, eksisterendeResultat)
    }
}
