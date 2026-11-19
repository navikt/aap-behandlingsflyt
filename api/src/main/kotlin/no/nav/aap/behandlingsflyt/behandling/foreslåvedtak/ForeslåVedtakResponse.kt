package no.nav.aap.behandlingsflyt.behandling.foreslåvedtak

import no.nav.aap.komponenter.type.Periode

data class ForeslåVedtakResponse(
    val perioder: List<ForeslåVedtakDto>,
    val stansOpphør: List<StansOpphørDto>,
    val harTilgangTilÅSaksbehandle: Boolean,
    val endretRettighetsperiode: EndretRettighetsperiodeDto? = null,
)

data class EndretRettighetsperiodeDto(
    val fra: Periode,
    val til: Periode,
)