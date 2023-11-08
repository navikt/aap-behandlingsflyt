package no.nav.aap.behandlingsflyt.flyt.steg

import no.nav.aap.behandlingsflyt.flyt.FlytKontekst

interface BehandlingSteg {

    fun utfør(kontekst: FlytKontekst): StegResultat

    fun vedTilbakeføring(kontekst: FlytKontekst) {

    }
}
