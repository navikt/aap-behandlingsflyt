package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn.flate

data class BarnetilleggGrunnlagDto(
    val opplysninger: List<IdentifiserteBarnDto>,
    val vurdering: ManuelleBarnVurderingDto
)