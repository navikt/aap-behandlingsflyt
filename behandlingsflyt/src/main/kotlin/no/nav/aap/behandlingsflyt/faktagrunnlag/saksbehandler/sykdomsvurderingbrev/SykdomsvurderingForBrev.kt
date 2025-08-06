package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdomsvurderingbrev

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import java.time.LocalDateTime


data class SykdomsvurderingForBrev(
    val behandlingId: BehandlingId,
    val vurdering: String?,
    val vurdertAv: String,
    val vurdertTidspunkt: LocalDateTime = LocalDateTime.now(),
)
