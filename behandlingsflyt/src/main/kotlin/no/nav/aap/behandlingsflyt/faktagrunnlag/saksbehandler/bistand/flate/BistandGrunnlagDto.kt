package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.flate

data class BistandGrunnlagDto(
    @Deprecated("Bruk vurderinger")
    val vurdering: BistandVurderingDto?,
    val vurderinger: List<BistandVurderingDto>?,
    val historiskeVurderinger: List<BistandVurderingDto>
)
