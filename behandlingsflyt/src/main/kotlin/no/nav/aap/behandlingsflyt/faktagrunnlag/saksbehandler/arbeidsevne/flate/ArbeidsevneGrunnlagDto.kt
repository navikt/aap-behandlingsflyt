package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsevne.flate

import java.time.LocalDate
import java.time.LocalDateTime

data class ArbeidsevneGrunnlagDto(
    val harTilgangTilÅSaksbehandle: Boolean,
    val vurderinger: List<ArbeidsevneVurderingDto>,
    val gjeldendeVedtatteVurderinger: List<ArbeidsevneVurderingDto>,
    val historikk: Set<ArbeidsevneVurderingDto>
)

data class ArbeidsevneVurderingDto(
    val begrunnelse: String,
    val vurderingsTidspunkt: LocalDateTime,
    val arbeidsevne: Int,
    val fraDato: LocalDate
)

data class SimulerArbeidsevneDto(val vurderinger: List<FastsettArbeidsevneDto>)

data class SimulertArbeidsevneResultatDto(
    val gjeldendeVedtatteVurderinger: List<ArbeidsevneVurderingDto>
)