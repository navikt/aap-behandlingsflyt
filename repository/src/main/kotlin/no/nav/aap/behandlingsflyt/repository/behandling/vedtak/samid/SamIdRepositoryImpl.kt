package no.nav.aap.behandlingsflyt.repository.behandling.vedtak.samid

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.samid.SamIdOgTpNr
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.samid.SamIdRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.repository.Factory

class SamIdRepositoryImpl(private val connection: DBConnection) : SamIdRepository {

    companion object : Factory<SamIdRepository> {
        override fun konstruer(connection: DBConnection): SamIdRepository {
            return SamIdRepositoryImpl(connection)
        }
    }

    override fun hentHvisEksisterer(behandlingId: BehandlingId): List<SamIdOgTpNr> {
        val query = """
            SELECT sam_id, tp_nr FROM SAM_ID WHERE behandling_id = ?
        """.trimIndent()

        return connection.queryList(query) {
            setParams {
                setLong(1, behandlingId.id)
            }
            setRowMapper {
                SamIdOgTpNr(
                    samId = it.getLong("sam_id"),
                    tpNr = it.getLongOrNull("tp_nr")
                )
            }
        }
    }

    override fun lagre(
        behandlingId: BehandlingId,
        samIdOgTpNr: List<SamIdOgTpNr>
    ) {
        val query = """
            INSERT INTO SAM_ID (SAM_ID, TP_NR, behandling_id) VALUES (?,?,?)
            """.trimIndent()
        connection.executeBatch(query, samIdOgTpNr) {
            setParams {
                setLong(1, it.samId)
                setLong(2, it.tpNr)
                setLong(3, behandlingId.id)
            }
        }
    }

}