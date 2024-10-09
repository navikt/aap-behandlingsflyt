package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsevne.flate

import java.time.LocalDate
import java.time.LocalDateTime

data class ArbeidsevneGrunnlagDto(
    val vurderinger: List<ArbeidsevneVurderingDto>
)

data class ArbeidsevneVurderingDto(
    val begrunnelse: String,
    val vurderingsdato: LocalDateTime,
    val arbeidsevne: Int,
    val fraDato: LocalDate
)