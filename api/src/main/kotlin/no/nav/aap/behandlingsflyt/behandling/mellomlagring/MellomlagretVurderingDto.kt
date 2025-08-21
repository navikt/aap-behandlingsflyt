package no.nav.aap.behandlingsflyt.behandling.mellomlagring

import com.papsign.ktor.openapigen.annotations.parameters.PathParam
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AvklaringsbehovKode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import java.time.LocalDateTime
import java.util.UUID

data class MellomlagretVurderingDto(
    val behandlingId: BehandlingId,
    val avklaringsbehovkode: AvklaringsbehovKode,
    val data: String,
    val vurdertAv: String,
    val vurdertDato: LocalDateTime
)

data class MellomlagretVurderingResponse(
    val mellomlagretVurdering: MellomlagretVurderingDto?
)

data class BehandlingReferanseMedAvklaringsbehov(@param:PathParam("referanse") val referanse: UUID, @param:PathParam("avklaringsbehovkode") val avklaringsbehovkode: String)
