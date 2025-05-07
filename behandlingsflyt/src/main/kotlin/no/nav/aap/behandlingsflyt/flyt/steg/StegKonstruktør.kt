package no.nav.aap.behandlingsflyt.flyt.steg

interface StegKonstruktør {
    fun konstruer(steg: FlytSteg): BehandlingSteg
}