package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import java.time.LocalDateTime

data class InntektsbortfallVurdering(
    val begrunnelse: String,
    val rettTilUttak: Boolean,
    val vurdertAv: String,
    val vurdertIBehandling: BehandlingId,
    val opprettetTid: LocalDateTime,
)