package no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn

import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person

interface BarnGateway {
    suspend fun hentBarn(person: Person): List<Barn>
}