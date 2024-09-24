package no.nav.aap.behandlingsflyt.flyt.steg

import no.nav.aap.verdityper.flyt.FlytKontekstMedPerioder

interface BehandlingSteg {

    fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat

    fun vedTilbakeføring(kontekst: FlytKontekstMedPerioder) {

    }
}
