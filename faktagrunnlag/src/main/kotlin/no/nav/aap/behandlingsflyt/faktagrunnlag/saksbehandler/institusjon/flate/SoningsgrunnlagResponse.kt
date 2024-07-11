package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon.flate

data class SoningsgrunnlagResponse(
    val soningsopphold: List<SoningsoppholdDto>,
    val soningsvurdering: SoningsvurderingDto?
)
