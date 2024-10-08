package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.flate

import java.time.LocalDate
import java.time.LocalDateTime

data class FritakMeldepliktGrunnlagDto(
    val vurderinger: List<FritakMeldepliktVurderingDto>
)

data class FritakMeldepliktVurderingDto(
    val begrunnelse: String,
    val vurderingsTidspunkt: LocalDateTime,
    val harFritak: Boolean,
    val fraDato: LocalDate
)

