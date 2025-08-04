package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov

import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekst
import no.nav.aap.komponenter.verdityper.Bruker

class AvklaringsbehovKontekst(val bruker: Bruker, val kontekst: FlytKontekst) {
    fun behandlingId() = kontekst.behandlingId
}