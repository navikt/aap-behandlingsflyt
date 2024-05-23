package no.nav.aap.behandlingsflyt.faktagrunnlag

import no.nav.aap.behandlingsflyt.dbconnect.DBConnection

interface Informasjonskravkonstruktør {
    fun konstruer(connection: DBConnection): Informasjonskrav
}
