package no.nav.aap.behandlingsflyt.faktagrunnlag

import no.nav.aap.behandlingsflyt.sakogbehandling.sak.IdentGateway
import no.nav.aap.verdityper.sakogbehandling.Ident

object FakePdlGateway : IdentGateway {
    override fun hentAlleIdenterForPerson(ident: Ident): List<Ident> {
        return listOf(ident)
    }
}