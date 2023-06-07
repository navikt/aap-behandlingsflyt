package no.nav.aap.domene.behandling.grunnlag.yrkesskade

import no.nav.aap.domene.typer.Ident
import no.nav.aap.domene.typer.Periode

object YrkesskadeRegister {

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
