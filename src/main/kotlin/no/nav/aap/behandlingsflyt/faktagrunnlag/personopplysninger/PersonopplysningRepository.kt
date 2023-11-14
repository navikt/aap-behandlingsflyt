package no.nav.aap.behandlingsflyt.faktagrunnlag.personopplysninger

import no.nav.aap.behandlingsflyt.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.dbconnect.DBConnection

class PersonopplysningRepository(private val connection: DBConnection) {

    fun hentHvisEksisterer(behandlingId: BehandlingId): PersonopplysningGrunnlag? {
        return connection.queryFirstOrNull("SELECT ID, FODSELSDATO FROM PERSONOPPLYSNING WHERE AKTIV AND BEHANDLING_ID = ?") {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setRowMapper { row ->
                PersonopplysningGrunnlag(
                    behandlingId.toLong(),
                    Personopplysning(Fødselsdato(row.getLocalDate("FODSELSDATO")))
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

        connection.execute("INSERT INTO PERSONOPPLYSNING (BEHANDLING_ID, FODSELSDATO) VALUES (?, ?)") {
            setParams {
                setLong(1, behandlingId.toLong())
                setLocalDate(2, personopplysning.fødselsdato.toLocalDate())
            }
        }
    }

    private fun deaktiverEksisterende(behandlingId: BehandlingId) {
        connection.execute("UPDATE PERSONOPPLYSNING SET AKTIV = 'FALSE' WHERE AKTIV AND BEHANDLING_ID = ?") {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setResultValidator { rowsUpdated ->
                require(rowsUpdated == 1)
            }
        }
    }

    fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        connection.execute("INSERT INTO PERSONOPPLYSNING (BEHANDLING_ID, FODSELSDATO) SELECT ?, FODSELSDATO FROM PERSONOPPLYSNING WHERE AKTIV AND BEHANDLING_ID = ?") {
            setParams {
                setLong(1, tilBehandling.toLong())
                setLong(2, fraBehandling.toLong())
            }
        }
    }
}
