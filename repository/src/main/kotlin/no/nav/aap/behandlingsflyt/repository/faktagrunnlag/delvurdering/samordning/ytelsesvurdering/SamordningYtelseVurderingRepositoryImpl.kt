package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.samordning.ytelsesvurdering

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningVurderingGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningVurderingPeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningYtelseVurderingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.lookup.repository.Factory

class SamordningYtelseVurderingRepositoryImpl(private val connection: DBConnection) :
    SamordningYtelseVurderingRepository {

    companion object : Factory<SamordningYtelseVurderingRepositoryImpl> {
        override fun konstruer(connection: DBConnection): SamordningYtelseVurderingRepositoryImpl {
            return SamordningYtelseVurderingRepositoryImpl(connection)
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
                hentSamordningVurderinger(it.getLong("vurderinger_id"))
            }
        }
    }

    private fun hentSamordningVurderinger(vurderingerId: Long?): SamordningVurderingGrunnlag {
        if (vurderingerId == null) {
            return SamordningVurderingGrunnlag(
                vurderingerId = null,
                vurderinger = emptyList()
            )
        }

        val query = """
            SELECT * FROM SAMORDNING_VURDERING WHERE vurderinger_id = ?
        """.trimIndent()
        val vurderinger = connection.queryList(query) {
            setParams {
                setLong(1, vurderingerId)
            }
            setRowMapper {
                SamordningVurdering(
                    ytelseType = it.getEnum("ytelse_type"),
                    vurderingPerioder = hentSamordningVurderingPerioder(it.getLong("id")),
                    begrunnelse = it.getString("begrunnelse"),
                    maksDatoEndelig = it.getBoolean("maksdato_endelig"),
                    maksDato = it.getLocalDate("maksdato"),
                )
            }
        }
        return SamordningVurderingGrunnlag(vurderingerId, vurderinger)
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


    override fun lagreVurderinger(behandlingId: BehandlingId, samordningVurderinger: List<SamordningVurdering>) {
        val eksisterendeGrunnlag = hentHvisEksisterer(behandlingId)
        if (eksisterendeGrunnlag != null) {
            deaktiverGrunnlag(behandlingId)
        }

        val samordningVurderingerQuery = """
            INSERT INTO SAMORDNING_VURDERINGER DEFAULT VALUES
            """.trimIndent()
        val vurderingerId = connection.executeReturnKey(samordningVurderingerQuery)

        for (vurdering in samordningVurderinger) {
            val vurderingQuery = """
                INSERT INTO SAMORDNING_VURDERING (vurderinger_id, ytelse_type, begrunnelse, maksdato_endelig, maksdato) VALUES (?, ?, ?, ?, ?)
                """.trimIndent()
            val vurderingId = connection.executeReturnKey(vurderingQuery) {
                setParams {
                    setLong(1, vurderingerId)
                    setEnumName(2, vurdering.ytelseType)
                    setString(3, vurdering.begrunnelse)
                    setBoolean(4, vurdering.maksDatoEndelig)
                    setLocalDate(5, vurdering.maksDato)
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