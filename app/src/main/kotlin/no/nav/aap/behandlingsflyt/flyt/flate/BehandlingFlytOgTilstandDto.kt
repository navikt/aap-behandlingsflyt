package no.nav.aap.behandlingsflyt.flyt.flate

import no.nav.aap.behandlingsflyt.flyt.flate.visning.Prosessering
import no.nav.aap.behandlingsflyt.flyt.flate.visning.Visning
import no.nav.aap.verdityper.flyt.StegGruppe
import no.nav.aap.verdityper.flyt.StegType

data class BehandlingFlytOgTilstandDto(
    val flyt: List<FlytGruppe>,
    val aktivtSteg: StegType,
    val aktivGruppe: StegGruppe,
    val vurdertGruppe: StegGruppe?,
    val behandlingVersjon: Long,
    val prosessering: Prosessering,
    val visning: Visning
)
