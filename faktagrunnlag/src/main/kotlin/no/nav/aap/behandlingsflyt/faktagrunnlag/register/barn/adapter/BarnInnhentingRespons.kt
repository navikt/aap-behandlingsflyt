package no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.adapter

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.Barn

class BarnInnhentingRespons(val registerBarn: List<Barn>, val oppgitteBarn: List<Barn>) {
    fun alleBarn(): List<Barn> {
        return oppgitteBarn + registerBarn
    }
}