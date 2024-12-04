package no.nav.aap.behandlingsflyt.sakogbehandling.sak

import no.nav.aap.behandlingsflyt.sakogbehandling.Ident

interface IdentGateway {
    fun hentAlleIdenterForPerson(ident: Ident): List<Ident>
}