package no.nav.aap.behandlingsflyt.flyt.flate

import no.nav.aap.behandlingsflyt.flyt.steg.StegGruppe
import no.nav.aap.behandlingsflyt.flyt.steg.StegType

class BehandlingFlytOgTilstandDto(
    val flyt: List<FlytGruppe>,
    val aktivtSteg: StegType,
    val aktivGruppe: StegGruppe,
    val behandlingVersjon: Long
)
