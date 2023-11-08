package no.nav.aap.behandlingsflyt.flyt.steg.impl

import no.nav.aap.behandlingsflyt.flyt.FlytKontekst
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat

class GeneriskPlaceholderSteg : BehandlingSteg {
    override fun utfør(kontekst: FlytKontekst): StegResultat {
        return StegResultat() // DO NOTHING
    }
}
