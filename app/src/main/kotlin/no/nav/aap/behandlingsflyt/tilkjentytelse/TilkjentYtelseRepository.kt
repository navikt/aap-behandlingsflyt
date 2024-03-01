package no.nav.aap.behandlingsflyt.tilkjentytelse

import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.Tilkjent
import no.nav.aap.tidslinje.Tidslinje
import no.nav.aap.verdityper.sakogbehandling.BehandlingId

class TilkjentYtelseRepository(private val connection: DBConnection) {
    fun hent(behandlingId: BehandlingId){

    }

    fun lagre(behandlingId: BehandlingId, tilkjent: Tidslinje<Tilkjent>){

    }

    fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        require(fraBehandling != tilBehandling)
    }

    private fun deaktiverEksisterende(behandlingId: BehandlingId) {
        connection.execute("UPDATE TILKJENT_YTELSE SET AKTIV = FALSE WHERE AKTIV AND BEHANDLING_ID = ?") {
            setParams {
                setLong(1, behandlingId.toLong())
            }
        }
    }

}