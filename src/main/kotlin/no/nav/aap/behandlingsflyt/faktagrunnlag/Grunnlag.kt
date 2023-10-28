package no.nav.aap.behandlingsflyt.faktagrunnlag

import no.nav.aap.behandlingsflyt.dbstuff.DBConnection
import no.nav.aap.behandlingsflyt.flyt.FlytKontekst

interface Grunnlag {
    fun oppdater(transaksjonsconnection: DBConnection, kontekst: FlytKontekst): Boolean
}
