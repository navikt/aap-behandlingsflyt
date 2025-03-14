package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.samordning.ytelsesvurdering

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningVurderingGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningVurderingPeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningVurderingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.lookup.repository.Factory
import java.time.LocalDate

class SamordningVurderingRepositoryImpl(private val connection: DBConnection) :
    SamordningVurderingRepository {

    companion object : Factory<SamordningVurderingRepositoryImpl> {
        override fun konstruer(connection: DBConnection): SamordningVurderingRepositoryImpl {
            return SamordningVurderingRepositoryImpl(connection)
        }
    }

    override fun hentHvisEksisterer(behandlingId: BehandlingId): SamordningVurderingGrunnlag? {
        val query = """
            SELECT * FROM SAMORDNING_YTELSEVURDERING_GRUNNLAG WHERE behandling_id = ? and aktiv = true
        """.trimIndent()
        return connection.queryFirstOrNull(query) {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setRowMapper {
                hentSamordningVurderinger(it.getLongOrNull("vurderinger_id"))
            }
        }
    }

    private fun hentSamordningVurderinger(vurderingerId: Long?): SamordningVurderingGrunnlag? {
        if (vurderingerId == null) {
            return null
        }

        val query = """
            SELECT * FROM SAMORDNING_VURDERINGER svr JOIN SAMORDNING_VURDERING sv on svr.id = sv.vurderinger_id  WHERE vurderinger_id = ?
        """.trimIndent()

        data class FellesFelter(
            val begrunnelse: String,
            val maksDatoEndelig: Boolean,
            val maksDato: LocalDate?
        )

        val vurderinger = connection.queryList(query) {
            setParams {
                setLong(1, vurderingerId)
            }
            setRowMapper {
                Pair(
                    FellesFelter(
                        begrunnelse = it.getString("begrunnelse"),
                        maksDatoEndelig = it.getBoolean("maksdato_endelig"),
                        maksDato = it.getLocalDateOrNull("maksdato")
                    ), SamordningVurdering(
                        ytelseType = it.getEnum("ytelse_type"),
                        vurderingPerioder = hentSamordningVurderingPerioder(it.getLong("id")),
                    )
                )
            }
        }
        val begrunnelse = vurderinger.first().first.begrunnelse
        val maksDatoEndelig = vurderinger.first().first.maksDatoEndelig
        val maksDato = vurderinger.first().first.maksDato
        return SamordningVurderingGrunnlag(
            vurderingerId = vurderingerId,
            begrunnelse = begrunnelse,
            maksDatoEndelig = maksDatoEndelig,
            maksDato = maksDato,
            vurderinger = vurderinger.map { it.second }
        )
    }

    private fun hentSamordningVurderingPerioder(vurderingId: Long): List<SamordningVurderingPeriode> {
        val query = """
            SELECT * FROM SAMORDNING_VURDERING_PERIODE WHERE vurdering_id = ?
        """.trimIndent()
        return connection.queryList(query) {
            setParams {
                setLong(1, vurderingId)
            }
            setRowMapper {
                SamordningVurderingPeriode(
                    periode = it.getPeriode("periode"),
                    gradering = it.getIntOrNull("gradering")?.let { g -> Prosent(g) },
                    kronesum = it.getIntOrNull("kronesum")
                )
            }
        }
    }


    override fun lagreVurderinger(
        behandlingId: BehandlingId,
        samordningVurderinger: SamordningVurderingGrunnlag
    ) {
        val eksisterendeGrunnlag = hentHvisEksisterer(behandlingId)
        if (eksisterendeGrunnlag != null) {
            deaktiverGrunnlag(behandlingId)
        }

        val samordningVurderingerQuery = """
            INSERT INTO SAMORDNING_VURDERINGER (begrunnelse, maksdato_endelig, maksdato)
            VALUES (?, ?, ?)
            """.trimIndent()
        val vurderingerId = connection.executeReturnKey(samordningVurderingerQuery) {
            setParams {
                setString(1, samordningVurderinger.begrunnelse)
                setBoolean(2, samordningVurderinger.maksDatoEndelig)
                setLocalDate(3, samordningVurderinger.maksDato)
            }
        }

        for (vurdering in samordningVurderinger.vurderinger) {
            val vurderingQuery = """
                INSERT INTO SAMORDNING_VURDERING (vurderinger_id, ytelse_type) VALUES (?, ?)
                """.trimIndent()
            val vurderingId = connection.executeReturnKey(vurderingQuery) {
                setParams {
                    setLong(1, vurderingerId)
                    setEnumName(2, vurdering.ytelseType)
                }
            }

            val veriodePeriodeQuery = """
                INSERT INTO SAMORDNING_VURDERING_PERIODE (periode, vurdering_id, gradering, kronesum) VALUES (?::daterange, ?, ?, ?)
                """.trimIndent()
            connection.executeBatch(veriodePeriodeQuery, vurdering.vurderingPerioder) {
                setParams {
                    setPeriode(1, it.periode)
                    setLong(2, vurderingId)
                    setInt(3, it.gradering?.prosentverdi())
                    setInt(4, it.kronesum?.toInt())
                }
            }
        }

        // TODO: Skal denne sjekke om det er nye ytelser, eller skjer det uansett når det kommer nye vurderinger?
        val grunnlagQuery = """
            INSERT INTO SAMORDNING_YTELSEVURDERING_GRUNNLAG (behandling_id, vurderinger_id) VALUES (?, ?)
        """.trimIndent()
        connection.execute(grunnlagQuery) {
            setParams {
                setLong(1, behandlingId.toLong())
                setLong(2, vurderingerId)
            }
        }
    }

    private fun deaktiverGrunnlag(behandlingId: BehandlingId) {
        connection.execute("UPDATE SAMORDNING_YTELSEVURDERING_GRUNNLAG set aktiv = false WHERE behandling_id = ? and aktiv = true") {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setResultValidator { require(it == 1) }
        }
    }

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        val eksisterendeGrunnlag = hentHvisEksisterer(fraBehandling)
        if (eksisterendeGrunnlag == null) {
            return
        }
        val query = """
            INSERT INTO SAMORDNING_YTELSEVURDERING_GRUNNLAG 
                (behandling_id, vurderinger_id)
            SELECT ?, vurderinger_id
                from SAMORDNING_YTELSEVURDERING_GRUNNLAG 
                where behandling_id = ? and aktiv
        """.trimIndent()

        connection.execute(query) {
            setParams {
                setLong(1, tilBehandling.toLong())
                setLong(2, fraBehandling.toLong())
            }
        }
    }
}