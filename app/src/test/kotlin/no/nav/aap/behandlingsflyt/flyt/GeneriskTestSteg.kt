package no.nav.aap.behandlingsflyt.flyt

import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.verdityper.flyt.FlytKontekst

class GeneriskTestSteg : BehandlingSteg {
    override fun utfør(kontekst: FlytKontekst): StegResultat {
        return StegResultat() // DO NOTHING
    }
}
