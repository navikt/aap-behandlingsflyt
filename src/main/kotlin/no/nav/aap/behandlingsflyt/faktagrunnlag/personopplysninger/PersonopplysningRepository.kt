package no.nav.aap.behandlingsflyt.faktagrunnlag.personopplysninger

import no.nav.aap.behandlingsflyt.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.dbstuff.DBConnection

class PersonopplysningRepository(private val connection: DBConnection) {

    fun hentHvisEksisterer(behandlingId: BehandlingId): PersonopplysningGrunnlag? {
        return connection.queryFirstOrNull("SELECT ID, FODSELSDATO FROM PERSONOPPLYSNING WHERE BEHANDLING_ID = ?") {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setRowMapper { row ->
                PersonopplysningGrunnlag(behandlingId.toLong(), Personopplysning(Fødselsdato(row.getLocalDate("FODSELSDATO"))))
            }
        }
    }

    fun lagre(behandlingId: BehandlingId, personopplysning: Personopplysning?) {
        connection.execute("INSERT INTO PERSONOPPLYSNING (BEHANDLING_ID, FODSELSDATO) VALUES (?, ?)") {
            setParams {
                setLong(1, behandlingId.toLong())
                setLocalDate(2, personopplysning?.fødselsdato?.toLocalDate())
            }
        }
    }

    fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        connection.execute("INSERT INTO PERSONOPPLYSNING (BEHANDLING_ID, FODSELSDATO) SELECT ?, FODSELSDATO FROM PERSONOPPLYSNING WHERE BEHANDLING_ID = ?") {
            setParams {
                setLong(1, tilBehandling.toLong())
                setLong(2, fraBehandling.toLong())
            }
        }
    }
}
