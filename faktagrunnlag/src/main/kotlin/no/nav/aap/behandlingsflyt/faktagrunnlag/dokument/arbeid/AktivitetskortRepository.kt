package no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid

import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokumentRepository
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.verdityper.sakogbehandling.BehandlingId

class AktivitetskortRepository(private val connection: DBConnection) {
    private val mottattDokumentRepository = MottattDokumentRepository(connection)

        fun hent(behandlingId: BehandlingId): AktivitetskortGrunnlag {
            return requireNotNull(hentHvisEksisterer(behandlingId))
        }
        fun hentHvisEksisterer(behandlingId: BehandlingId): AktivitetskortGrunnlag? {
            /*
            val query = """
                SELECT * FROM AKTIVITETSKORT_GRUNNLAG WHERE behandling_id = ? and aktiv = true
            """.trimIndent()
            return connection.queryFirstOrNull(query) {
                setParams {
                    setLong(1, behandlingId.toLong())
                }
                setRowMapper {
                    mapGrunnlag(it, behandlingId)
                }
            }*/
            return null
        }
}