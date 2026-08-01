package no.nav.aap.beregning

import java.time.LocalDateTime
import no.nav.aap.behandling.BehandlingId
import no.nav.aap.komponenter.verdityper.Bruker

data class InntektsbortfallVurdering(
    val begrunnelse: String,
    val rettTilUttak: Boolean,
    val vurdertAv: Bruker,
    val vurdertIBehandling: BehandlingId,
    val opprettetTid: LocalDateTime,
)