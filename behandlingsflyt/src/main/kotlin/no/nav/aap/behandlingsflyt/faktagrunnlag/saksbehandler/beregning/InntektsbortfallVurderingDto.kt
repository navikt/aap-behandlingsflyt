package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning

data class InntektsbortfallVurderingDto(
    val begrunnelse: String,
    val rettTilUttak: Boolean
)