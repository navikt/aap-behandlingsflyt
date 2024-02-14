package no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn

import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Fødselsdato
import no.nav.aap.verdityper.sakogbehandling.BehandlingId
import no.nav.aap.verdityper.sakogbehandling.Ident

class BarnRepository(private val connection: DBConnection) {

    fun hentHvisEksisterer(behandlingId: BehandlingId): BarnGrunnlag {
        val barn = connection.queryList(
            """
                SELECT p.IDENT, p.FODSELSDATO, p.OPPRETTET_TID, p.DODSDATO
                FROM BARNOPPLYSNING_GRUNNLAG g
                INNER JOIN BARNOPPLYSNING p ON g.BGB_ID = p.BGB_ID
                WHERE g.AKTIV AND g.BEHANDLING_ID = ?
            """.trimIndent()
        ) {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setRowMapper { row ->
                Barn (
                    ident = Ident(row.getString("IDENT")),
                    fødselsdato = Fødselsdato(row.getLocalDate("FODSELSDATO")),
                    dødsdato = row.getLocalDateOrNull("DODSDATO")?.let { Dødsdato(it) }
                )
            }
        }

        return BarnGrunnlag(
            barn = barn
        )
    }

    fun lagre(behandlingId: BehandlingId, barn: List<Barn>) {
        deaktiverEksisterende(behandlingId)

        val bgbId = connection.executeReturnKey("INSERT INTO BARNOPPLYSNING_GRUNNLAG_BARNOPPLYSNING DEFAULT VALUES") {}

        connection.execute(
            """
                INSERT INTO BARNOPPLYSNING_GRUNNLAG (BEHANDLING_ID, BGB_ID) VALUES (?, ?)
            """.trimIndent()
        ) {
            setParams {
                setLong(1, behandlingId.toLong())
                setLong(2, bgbId)
            }
        }

        connection.executeBatch(
            """
                INSERT INTO BARNOPPLYSNING (IDENT, FODSELSDATO, DODSDATO, BGB_ID) VALUES (?, ?, ?, ?)
            """.trimIndent(),
            barn
        ) {
            setParams {  barnet ->
                setString(1, barnet.ident.identifikator)
                setLocalDate(2, barnet.fødselsdato.toLocalDate())
                setLocalDate(3, barnet.dødsdato?.toLocalDate())
                setLong(4, bgbId)
            }
        }
    }

    fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        require(fraBehandling != tilBehandling)
    }

    private fun deaktiverEksisterende(behandlingId: BehandlingId) {
        connection.execute("UPDATE BARNOPPLYSNING_GRUNNLAG SET AKTIV = FALSE WHERE AKTIV AND BEHANDLING_ID = ?") {
            setParams {
                setLong(1, behandlingId.toLong())
            }
        }
    }
}
