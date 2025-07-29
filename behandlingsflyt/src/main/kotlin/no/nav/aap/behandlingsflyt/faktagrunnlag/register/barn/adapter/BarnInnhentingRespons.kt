package no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.adapter

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.Barn

/**
 * Respons fra PDL.
 */
data class BarnInnhentingRespons(val registerBarn: List<Barn>, val oppgitteBarnFraPDL: List<Barn>) {
    fun alleBarn(): List<Barn> {
        val alleBarn = registerBarn + oppgitteBarnFraPDL
        val unikeIdenter = alleBarn.map { it.ident }.toSet()
        return (oppgitteBarnFraPDL + registerBarn).filter { unikeIdenter.contains(it.ident) }.toSet().toList()
    }

}