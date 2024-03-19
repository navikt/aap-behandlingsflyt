package no.nav.aap.behandlingsflyt.flyt

import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.verdityper.flyt.FlytKontekst

@Deprecated("Skal bort når alle steg er implementert")
class GeneriskPlaceholderSteg : BehandlingSteg {
    override fun utfør(kontekst: FlytKontekst): StegResultat {
        return StegResultat() // DO NOTHING
    }
}
