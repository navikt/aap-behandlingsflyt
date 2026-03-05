package no.nav.aap.behandlingsflyt.behandling.foreslåvedtak

data class ForeslåVedtakResponse(
    val perioder: List<ForeslåVedtakDto>,
    val stansOpphør: List<StansOpphørDto>,
)