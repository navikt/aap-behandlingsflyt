package no.nav.aap.behandlingsflyt.behandling.klage.behandlendeenhet

import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.behandlendeenhet.BehandlendeEnhetVurdering

data class BehandlendeEnhetGrunnlagDto(
    val vurdering: BehandlendeEnhetVurderingDto?,
)

data class BehandlendeEnhetVurderingDto(
    val skalBehandlesAvNay: Boolean,
    val skalBehandlesAvKontor: Boolean,
    val vurdertAv: String
)

internal fun BehandlendeEnhetVurdering.tilDto() =
    BehandlendeEnhetVurderingDto(
        skalBehandlesAvNay = skalBehandlesAvNay,
        skalBehandlesAvKontor = skalBehandlesAvKontor,
        vurdertAv = vurdertAv,
    )