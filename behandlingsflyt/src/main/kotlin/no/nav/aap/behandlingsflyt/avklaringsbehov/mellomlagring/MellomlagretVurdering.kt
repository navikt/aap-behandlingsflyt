package no.nav.aap.behandlingsflyt.avklaringsbehov.mellomlagring

import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AvklaringsbehovKode
import no.nav.aap.behandling.BehandlingId
import java.time.LocalDateTime

data class MellomlagretVurdering(
    val behandlingId: BehandlingId,
    val avklaringsbehovKode: AvklaringsbehovKode,
    val data: String,
    val vurdertAv: String,
    val vurdertDato: LocalDateTime,
)
