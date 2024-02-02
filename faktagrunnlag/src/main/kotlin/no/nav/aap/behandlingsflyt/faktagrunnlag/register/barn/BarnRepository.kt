package no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn

import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.verdityper.sakogbehandling.BehandlingId

class BarnRepository(private val connection: DBConnection) {

    fun hentHvisEksisterer(behandlingId: BehandlingId): BarnGrunnlag? {
        return null
    }

    fun lagre(behandlingId: BehandlingId, barn: List<Barn>) {
    }


    fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        require(fraBehandling != tilBehandling)
    }
}
