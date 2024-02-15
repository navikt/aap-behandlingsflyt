package no.nav.aap.behandlingsflyt.sakogbehandling.sak

import no.nav.aap.verdityper.sakogbehandling.Ident

interface IdentGateway {
    suspend fun hentAlleIdenterForPerson(ident: Ident): List<Ident>
}