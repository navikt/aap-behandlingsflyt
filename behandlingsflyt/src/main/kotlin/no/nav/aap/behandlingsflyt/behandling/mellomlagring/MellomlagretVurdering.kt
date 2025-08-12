package no.nav.aap.behandlingsflyt.behandling.mellomlagring

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import java.time.LocalDateTime

data class MellomlagretVurdering(
    val behandlingId: BehandlingId,
    val avklaringsbehovKode: String,
    val data: String,
    val vurdertAv: String,
    val vurdertDato: LocalDateTime,
)
