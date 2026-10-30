package no.nav.aap.barnetillegg

import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person
import no.nav.aap.komponenter.gateway.Gateway

interface BarnGateway : Gateway {
    fun hentBarn(
        person: Person,
        oppgitteBarnIdenter: List<Ident>,
        saksbehandlerOppgitteBarnIdenter: List<Ident>
    ): BarnInnhentingRespons
}