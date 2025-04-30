package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.effektuer11_7

import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.BrevbestillingReferanse
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.effektuer11_7.Effektuer11_7Forhåndsvarsel
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.effektuer11_7.Effektuer11_7Grunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.effektuer11_7.Effektuer11_7Repository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.effektuer11_7.Effektuer11_7Vurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisperiodeId
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.underveis.UnderveisRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.repository.Factory
import kotlin.collections.List

class Effektuer11_7RepositoryImpl(private val connection: DBConnection) : Effektuer11_7Repository {
    companion object : Factory<Effektuer11_7RepositoryImpl> {
        override fun konstruer(connection: DBConnection): Effektuer11_7RepositoryImpl {
            return Effektuer11_7RepositoryImpl(connection)
        }
    }

    override fun lagreVurdering(behandlingId: BehandlingId, vurdering: Effektuer11_7Vurdering) {
        val vurderingId = connection.executeReturnKey("insert into effektuer_11_7_vurdering(begrunnelse) values(?)") {
            setParams { setString(1, vurdering.begrunnelse) }
        }
        val gjeldendeGrunnlagId = deaktiverGjeldendeGrunnlag(behandlingId)

        if (gjeldendeGrunnlagId == null) {
            connection.execute(" insert into effektuer_11_7_grunnlag (behandling_id, vurdering_id) values (?, ?)") {
                setParams {
                    setLong(1, behandlingId.id)
                    setLong(2, vurderingId)
                }
            }
        } else {
            connection.execute(
                """
                    insert into effektuer_11_7_grunnlag (behandling_id, vurdering_id, varslinger_id)
                    select behandling_id, ? as vurdering_id, varslinger_id from effektuer_11_7_grunnlag where id = ?
                """.trimIndent()
            ) {
                setParams {
                    setLong(1, vurderingId)
                    setLong(2, gjeldendeGrunnlagId)
                }
            }
        }
    }

    override fun lagreVarsel(behandlingId: BehandlingId, varsel: Effektuer11_7Forhåndsvarsel) {
        val varslingerId = connection.executeReturnKey("insert into effektuer_11_7_varslinger default values")

        val varselId =
            connection.executeReturnKey("insert into effektuer_11_7_varsel(varslinger_id, dato_varslet, frist) values(?, ?, ?)") {
                setParams {
                    setLong(1, varslingerId)
                    setLocalDate(2, varsel.datoVarslet)
                    setLocalDate(3, varsel.frist)
                }
            }

        connection.executeBatch(
            "insert into effektuer_11_7_brudd(varsel_id, underveis_periode_id) values(?, ?)",
            varsel.underveisperioder
        ) {
            setParams {
                setLong(1, varselId)
                setLong(2, it.id!!.asLong)
            }
        }

        val gjeldendeGrunnlagId = deaktiverGjeldendeGrunnlag(behandlingId)

        if (gjeldendeGrunnlagId == null) {
            connection.execute(" insert into effektuer_11_7_grunnlag (behandling_id, varslinger_id) values (?, ?)") {
                setParams {
                    setLong(1, behandlingId.id)
                    setLong(2, varslingerId)
                }
            }
        } else {
            connection.execute(
                """
                    insert into effektuer_11_7_grunnlag (behandling_id, vurdering_id, varslinger_id)
                    select behandling_id, vurdering_id, ? as varslinger_id from effektuer_11_7_grunnlag where id = ?
                """.trimIndent()
            ) {
                setParams {
                    setLong(1, varslingerId)
                    setLong(2, gjeldendeGrunnlagId)
                }
            }
        }
    }

    override fun hentHvisEksisterer(behandlingId: BehandlingId): Effektuer11_7Grunnlag? {
        val query = """
            select * from effektuer_11_7_grunnlag grunnlag
            join effektuer_11_7_varslinger varslinger on grunnlag.varslinger_id = varslinger.id
            left join effektuer_11_7_vurdering vurdering on grunnlag.vurdering_id = vurdering.id 
            where grunnlag.behandling_id = ? and grunnlag.aktiv = true
            """.trimIndent()
        return connection.queryFirstOrNull(query) {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setRowMapper { row ->
                Effektuer11_7Grunnlag(
                    vurdering = row.getStringOrNull("begrunnelse")?.let { Effektuer11_7Vurdering(it) },
                    varslinger = hentVarslinger(row.getLong("varslinger_id")),
                )
            }
        }
    }

    override fun slett(behandlingId: BehandlingId)  {
      // Ikke relevant for trukkede søknader, da man ikke vil ha fått aktivitetsplikt
        // Vi sjekker om det finnes innhold i effektuer_11_7_grunnlag. Vi forventer ikke innhold her av en trukket søknad.
         val aktivitetsbrudd = connection.queryList(
            """
                    SELECT id
                    FROM effektuer_11_7_grunnlag
                    WHERE behandling_id = ?
                 
                """.trimIndent()
        ) {
            setParams { setLong(1, behandlingId.id) }
            setRowMapper { row ->
                row.getLong("id")
            }
        }
       if (aktivitetsbrudd.isNotEmpty()) { // Her må vi enten feile hardt siden det ikke er forventet at det skal være noe}
          // innhold i tabellene, evt som minimum logge at det skjedde, så vi kan rydde manuelt
       }
    }

    private fun hentVarslinger(varslingerId: Long): List<Effektuer11_7Forhåndsvarsel> {
        val query = """
            select varsel.dato_varslet, varsel.frist, array_agg(brudd.underveis_periode_id) as underveisperioder, varsel.brev_referanse
            from effektuer_11_7_varslinger varslinger
            join effektuer_11_7_varsel varsel on varslinger.id = varsel.varslinger_id
            join effektuer_11_7_brudd brudd on varsel.id = brudd.varsel_id
            where varslinger.id = ?
            group by varsel.id
            """.trimIndent()
        return connection.queryList(query) {
            setParams {
                setLong(1, varslingerId)
            }
            setRowMapper { row ->
                Effektuer11_7Forhåndsvarsel(
                    datoVarslet = row.getLocalDate("dato_varslet"),
                    frist = row.getLocalDateOrNull("frist"),
                    underveisperioder = row.getArray("underveisperioder", Long::class)
                        .let {
                            UnderveisRepositoryImpl(connection).hentPerioder(it.map { UnderveisperiodeId(it) }).sorted()
                        },
                    referanse = row.getUUIDOrNull("brev_referanse")?.let { BrevbestillingReferanse(it) },
                )
            }
        }
            .sortedBy { it.datoVarslet }
    }

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        require(fraBehandling != tilBehandling)
        connection.execute(
            """
            insert into effektuer_11_7_grunnlag (behandling_id, varslinger_id, vurdering_id)
            select ? as behandling_id, varslinger_id, vurdering_id from effektuer_11_7_grunnlag
            where aktiv and behandling_id = ?
        """.trimIndent()
        ) {
            setParams {
                setLong(1, tilBehandling.id)
                setLong(2, fraBehandling.id)
            }
        }
    }

    private fun deaktiverGjeldendeGrunnlag(behandlingId: BehandlingId): Long? {
        return connection.queryFirstOrNull(
            """
            update effektuer_11_7_grunnlag
            set aktiv = false
            where aktiv and behandling_id = ? returning id
            """.trimIndent()
        ) {
            setParams { setLong(1, behandlingId.id) }
            setRowMapper {
                it.getLongOrNull("id")
            }
        }
    }
}