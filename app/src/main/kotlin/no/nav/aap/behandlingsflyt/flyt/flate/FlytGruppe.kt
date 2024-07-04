package no.nav.aap.behandlingsflyt.flyt.flate

import no.nav.aap.verdityper.flyt.StegGruppe

data class FlytGruppe(
    val stegGruppe: StegGruppe,
    val erFullført: Boolean,
    val steg: List<FlytSteg>,
    val skalVises: Boolean
)
