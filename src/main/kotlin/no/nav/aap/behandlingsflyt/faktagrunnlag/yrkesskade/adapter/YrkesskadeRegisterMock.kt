package no.nav.aap.behandlingsflyt.faktagrunnlag.yrkesskade.adapter

import no.nav.aap.behandlingsflyt.sak.Ident
import no.nav.aap.behandlingsflyt.Periode

object YrkesskadeRegisterMock {

    private val skader = HashMap<Ident, Periode>()

    fun innhent(identer: List<Ident>, periode: Periode): List<Periode> {
        return skader.filter { entry -> identer.contains(entry.key) }
            .filter { entry -> entry.value.overlapper(periode) }
            .map { it.value }
    }

    fun konstruer(ident: Ident, periode: Periode) {
        skader[ident] = periode
    }
}
