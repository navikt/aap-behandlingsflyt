package no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn

import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person
import no.nav.aap.verdityper.sakogbehandling.Ident

interface BarnGateway {
    fun hentBarn(person: Person, relaterteBarnIdenter: List<Ident>): List<Barn>
}