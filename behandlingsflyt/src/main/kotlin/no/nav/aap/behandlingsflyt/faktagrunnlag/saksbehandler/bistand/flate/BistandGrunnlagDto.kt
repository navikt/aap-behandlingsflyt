package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.flate

data class BistandGrunnlagDto(
    val vurdering: BistandVurderingDto?,
    val historiskeVurderinger: List<BistandVurderingDto>
)
