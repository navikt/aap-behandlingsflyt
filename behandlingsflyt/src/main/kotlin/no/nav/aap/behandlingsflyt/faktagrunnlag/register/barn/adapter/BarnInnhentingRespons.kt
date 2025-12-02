package no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.adapter

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.Barn

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