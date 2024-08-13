package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barnetillegg.flate

data class BarnetilleggGrunnlagDto(
    val opplysninger: List<IdentifiserteBarnDto>,
    val vurdering: ManuelleBarnVurderingDto
)