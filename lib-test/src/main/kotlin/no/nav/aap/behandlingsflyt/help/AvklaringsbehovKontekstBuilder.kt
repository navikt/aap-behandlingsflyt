package no.nav.aap.behandlingsflyt.help

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekst
import no.nav.aap.komponenter.verdityper.Bruker

class AvklaringsbehovKontekstBuilder {
    var bruker: Bruker = Bruker("SAKSBEHANDLER")
    var behandlingId: Long? = 1L
    var flytKontekst: FlytKontekst? = null
    var behandling: Behandling? = null
        set(value) {
            flytKontekst = value?.flytKontekst()
            behandlingId = value?.id?.id
            field = value
        }

    fun build(): AvklaringsbehovKontekst {
        requireNotNull(behandlingId)
        return AvklaringsbehovKontekst(bruker, flytKontekst!!)
    }
}

fun avklaringsbehovKontekst(init: AvklaringsbehovKontekstBuilder.() -> Unit): AvklaringsbehovKontekst =
    AvklaringsbehovKontekstBuilder().apply(init).build()