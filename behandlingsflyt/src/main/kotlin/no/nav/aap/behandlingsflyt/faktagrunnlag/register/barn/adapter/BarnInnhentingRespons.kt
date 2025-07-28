package no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.adapter

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.BarnFraRegister

/**
 * Respons fra PDL.
 */
data class BarnInnhentingRespons(
    val registerBarn: List<BarnFraRegister>,
    val oppgitteBarnFraPDL: List<BarnFraRegister>
) {
    fun alleBarn(): List<BarnFraRegister> {
        val alleBarn = registerBarn + oppgitteBarnFraPDL
        val unikeIdenter = alleBarn.map { it.ident }.toSet()
        return (oppgitteBarnFraPDL + registerBarn).filter { unikeIdenter.contains(it.ident) }.toSet().toList()
    }

}