package no.nav.aap.behandlingsflyt.sakogbehandling.sak

import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.lookup.gateway.Gateway

interface IdentGateway : Gateway {
    fun hentAlleIdenterForPerson(ident: Ident): List<Ident>
}