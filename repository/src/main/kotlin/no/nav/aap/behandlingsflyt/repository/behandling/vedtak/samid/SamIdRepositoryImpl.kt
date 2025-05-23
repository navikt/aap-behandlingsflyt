package no.nav.aap.behandlingsflyt.repository.behandling.vedtak.samid

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.samid.SamIdRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.dbconnect.DBConnection

class SamIdRepositoryImpl(private val connection: DBConnection) : SamIdRepository {
    override fun hentHvisEksisterer(behandlingId: BehandlingId): String? {
        val query = """
            SELECT * FROM SAM_ID WHERE behandling_id = ?
        """.trimIndent()

        return connection.queryFirstOrNull(
            query, {
                setParams {
                    setLong(1, behandlingId.id)
                }
            }
        )
    }

    override fun lagre(
        behandlingId: BehandlingId,
        samId: String
    ) {
        val query = """
            INSERT INTO SAM_ID (SAM_ID, behandling_id) VALUES (?,?)
            """.trimIndent()
        connection.execute(query){
            setParams {
                setString(1, samId)
                setLong(2, behandlingId.id)
            }
        }
    }

}