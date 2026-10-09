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

/**
 * Respons fra PDL.
 */
data class BarnInnhentingRespons(
    val registerBarn: List<Barn>,
    val oppgitteBarnFraPDL: List<Barn>,
    val saksbehandlerOppgitteBarnPDL: List<Barn>
) {
    fun alleBarn(): List<Barn> {
        // Manuell filtrering av unike identer ved bruk av distinctBy.
        return (registerBarn + oppgitteBarnFraPDL + saksbehandlerOppgitteBarnPDL).distinctBy { it.ident }
    }

}