package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.flate

import java.time.LocalDate

data class FritaksvurderingDto(
    val harFritak: Boolean,
    val fraDato: LocalDate,
    val begrunnelse: String,
)
