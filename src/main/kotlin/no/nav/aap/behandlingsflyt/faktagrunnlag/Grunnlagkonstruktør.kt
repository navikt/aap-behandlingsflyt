package no.nav.aap.behandlingsflyt.faktagrunnlag

import no.nav.aap.behandlingsflyt.dbconnect.DBConnection

interface Grunnlagkonstruktør {
    fun konstruer(connection: DBConnection): Grunnlag
}
