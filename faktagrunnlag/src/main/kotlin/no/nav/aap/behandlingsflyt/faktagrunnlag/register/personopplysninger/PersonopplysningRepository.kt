package no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger

import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.verdityper.sakogbehandling.BehandlingId

class PersonopplysningRepository(private val connection: DBConnection) {

    fun hentHvisEksisterer(behandlingId: BehandlingId): PersonopplysningGrunnlag? {
        return connection.queryFirstOrNull(
            """
            SELECT p.FODSELSDATO, p.OPPRETTET_TID
            FROM PERSONOPPLYSNING_GRUNNLAG g
            INNER JOIN PERSONOPPLYSNING p ON g.PERSONOPPLYSNING_ID = p.ID
            WHERE g.AKTIV AND g.BEHANDLING_ID = ?
            """.trimIndent()
        ) {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setRowMapper { row ->
                PersonopplysningGrunnlag(
                    behandlingId.toLong(),
                    Personopplysning(
                        fødselsdato = Fødselsdato(row.getLocalDate("FODSELSDATO")),
                        opprettetTid = row.getLocalDateTime("OPPRETTET_TID"),
                    )
                )
            }
        }
    }

    fun lagre(behandlingId: BehandlingId, personopplysning: Personopplysning) {
        val personopplysningGrunnlag = hentHvisEksisterer(behandlingId)

        if (personopplysningGrunnlag?.personopplysning == personopplysning) return

        if (personopplysningGrunnlag != null) {
            deaktiverEksisterende(behandlingId)
        }

        val personopplysningId = connection.executeReturnKey("INSERT INTO PERSONOPPLYSNING (FODSELSDATO) VALUES (?)") {
            setParams {
                setLocalDate(1, personopplysning.fødselsdato.toLocalDate())
            }
        }

        connection.execute("INSERT INTO PERSONOPPLYSNING_GRUNNLAG (BEHANDLING_ID, PERSONOPPLYSNING_ID) VALUES (?, ?)") {
            setParams {
                setLong(1, behandlingId.toLong())
                setLong(2, personopplysningId)
            }
        }
    }

    private fun deaktiverEksisterende(behandlingId: BehandlingId) {
        connection.execute("UPDATE PERSONOPPLYSNING_GRUNNLAG SET AKTIV = FALSE WHERE AKTIV AND BEHANDLING_ID = ?") {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setResultValidator { rowsUpdated ->
                require(rowsUpdated == 1)
            }
        }
    }

    fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        require(fraBehandling != tilBehandling)
        connection.execute("INSERT INTO PERSONOPPLYSNING_GRUNNLAG (BEHANDLING_ID, PERSONOPPLYSNING_ID) SELECT ?, PERSONOPPLYSNING_ID FROM PERSONOPPLYSNING_GRUNNLAG WHERE AKTIV AND BEHANDLING_ID = ?") {
            setParams {
                setLong(1, tilBehandling.toLong())
                setLong(2, fraBehandling.toLong())
            }
        }
    }
}
