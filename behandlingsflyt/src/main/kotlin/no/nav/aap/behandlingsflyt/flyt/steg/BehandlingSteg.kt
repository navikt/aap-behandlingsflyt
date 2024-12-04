package no.nav.aap.behandlingsflyt.flyt.steg

import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder

interface BehandlingSteg {

    fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat

    fun vedTilbakeføring(kontekst: FlytKontekstMedPerioder) {

    }
}
