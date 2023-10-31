package no.nav.aap.behandlingsflyt.behandling.flate

import no.nav.aap.behandlingsflyt.flyt.steg.StegGruppe
import no.nav.aap.behandlingsflyt.flyt.steg.StegType

class BehandlingFlytOgTilstand2Dto(
    val flyt: List<FlytGruppe>,
    val aktivtSteg: StegType,
    val aktivGruppe: StegGruppe,
    val behandlingVersjon: Long
)
