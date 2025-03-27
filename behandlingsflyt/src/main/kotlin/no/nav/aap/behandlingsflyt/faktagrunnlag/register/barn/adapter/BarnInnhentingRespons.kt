package no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.adapter

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.Barn

class BarnInnhentingRespons(val registerBarn: List<Barn>, val oppgitteBarn: List<Barn>) {
    fun alleBarn(): List<Barn> {
        val alleBarn = registerBarn + oppgitteBarn
        require(alleBarn.map { it.ident }.toSet().size == alleBarn.size) {
            "Det finnes duplikater i barn. Registerbarn: ${registerBarn.size}. Oppgitte barn: ${oppgitteBarn.size}"
        }
        return oppgitteBarn + registerBarn
    }
}