package no.nav.aap.behandlingsflyt.faktagrunnlag

import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.flyt.FlytKontekst

interface Grunnlag {
    fun oppdater(connection: DBConnection, kontekst: FlytKontekst): Boolean
}
