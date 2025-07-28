package no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.adapter.BarnInnhentingRespons
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person
import no.nav.aap.komponenter.gateway.Gateway

interface BarnGateway : Gateway {
    fun hentBarn(person: Person, oppgitteBarnIdenter: List<Ident>): BarnInnhentingRespons
}