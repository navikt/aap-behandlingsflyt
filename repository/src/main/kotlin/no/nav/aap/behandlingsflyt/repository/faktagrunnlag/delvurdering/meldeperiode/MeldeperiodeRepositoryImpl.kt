package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.meldeperiode

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.meldeperiode.MeldeperiodeRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.meldeperiode.MeldeperiodeUtleder
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.lookup.repository.Factory

class MeldeperiodeRepositoryImpl(private val connection: DBConnection) : MeldeperiodeRepository {
    companion object : Factory<MeldeperiodeRepositoryImpl> {
        override fun konstruer(connection: DBConnection): MeldeperiodeRepositoryImpl {
            return MeldeperiodeRepositoryImpl(connection)
        }
    }

    override fun hentFørsteMeldeperiode(behandlingId: BehandlingId): Periode? {
        val query = """
            SELECT periode FROM MELDEPERIODE 
            JOIN MELDEPERIODE_GRUNNLAG ON MELDEPERIODE.meldeperiodegrunnlag_id = MELDEPERIODE_GRUNNLAG.id
            WHERE behandling_id = ? AND aktiv = true
            order by periode
        """.trimIndent()
        val perioder = connection.queryList(query) {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setRowMapper {
                it.getPeriode("periode")
            }
        }.toList()

        return perioder.firstOrNull()
    }

    override fun hentMeldeperioder(
        behandlingId: BehandlingId,
        periode: Periode
    ): List<Periode> = MeldeperiodeUtleder.utledMeldeperiode(hentFørsteMeldeperiode(behandlingId)?.fom, periode)

    /**
     * Lagrer kun ned første meldeperioden - resten kan utledes
     */
    override fun lagreFørsteMeldeperiode(
        behandlingId: BehandlingId,
        meldeperiode: Periode?
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
        meldeperiode?.let {
            connection.execute(query) {
                setParams {
                    setLong(1, key)
                    setPeriode(2, it)
                }
            }
        }

    }

    override fun slett(behandlingId: BehandlingId) {
        // Ikke relevant for trukkede søknader, da man ikke vil ha fått meldeperioder
    }

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        lagreFørsteMeldeperiode(tilBehandling, hentFørsteMeldeperiode(fraBehandling))
    }
}