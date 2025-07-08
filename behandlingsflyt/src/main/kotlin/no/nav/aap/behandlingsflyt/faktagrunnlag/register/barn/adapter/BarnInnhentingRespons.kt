package no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.adapter

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.Barn

data class BarnInnhentingRespons(val registerBarn: List<Barn>, val oppgitteBarn: List<Barn>) {
    fun alleBarn(): Set<Barn> {
        val alleBarn = registerBarn + oppgitteBarn
        val unikeIdenter = alleBarn.map { it.ident }.toSet()
        return (oppgitteBarn + registerBarn).filter { unikeIdenter.contains(it.ident) }.toSet()
    }
}