package no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade.adapter

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade.YrkesskadeRegisterGateway
import no.nav.aap.verdityper.Periode
import no.nav.aap.verdityper.sakogbehandling.Ident

object FakeYrkesskadeRegisterGateway : YrkesskadeRegisterGateway {

    private val skader = HashMap<Ident, Periode>()

    override fun innhent(identer: List<Ident>, periode: Periode): List<Periode> {
        return skader.filter { entry -> identer.contains(entry.key) }
            .filter { entry -> entry.value.overlapper(periode) }
            .map { it.value }
    }

    fun konstruer(ident: Ident, periode: Periode) {
        skader[ident] = periode
    }
}
