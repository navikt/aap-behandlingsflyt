package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.meldeperiode

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.meldeperiode.MeldeperiodeRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisGrunnlag
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.lookup.repository.Factory

class MeldeperiodeRepositoryImpl(private val connection: DBConnection): MeldeperiodeRepository {
    companion object : Factory<MeldeperiodeRepositoryImpl> {
        override fun konstruer(connection: DBConnection): MeldeperiodeRepositoryImpl {
            return MeldeperiodeRepositoryImpl(connection)
        }
    }

    override fun hent(behandlingId: BehandlingId): List<Periode> {
        val query = """
            SELECT periode FROM MELDEPERIODE 
            JOIN MELDEPERIODE_GRUNNLAG ON MELDEPERIODE.meldeperiodegrunnlag_id = MELDEPERIODE_GRUNNLAG.id
            WHERE behandling_id = ? AND aktiv = true
            order by periode
        """.trimIndent()
        return connection.queryList(query) {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setRowMapper {
                it.getPeriode("periode")
            }
        }.toList()
    }

    override fun lagre(
        behandlingId: BehandlingId,
        meldeperioder: List<Periode>
    ) {
        val disableQuery = """
            UPDATE MELDEPERIODE_GRUNNLAG SET aktiv = false WHERE behandling_id = ?
        """.trimIndent()
        connection.execute(disableQuery) {
            setParams {
                setLong(1, behandlingId.toLong())
            }
        }

        val insertNewMeldeperiode_Grunnlag = """
            INSERT INTO MELDEPERIODE_GRUNNLAG (behandling_id, aktiv)
            VALUES (?, true)
        """.trimIndent()
        val key = connection.executeReturnKey(insertNewMeldeperiode_Grunnlag) {
            setParams {
                setLong(1, behandlingId.toLong())
            }
        }

        val query = """
            INSERT INTO MELDEPERIODE (meldeperiodegrunnlag_id, periode)
            VALUES (?, ?::daterange)
        """.trimIndent()
        connection.executeBatch(query, meldeperioder) {
            setParams {
                setLong(1, key)
                setPeriode(2, it)
            }
        }
    }

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        lagre(tilBehandling, hent(fraBehandling))
    }
}