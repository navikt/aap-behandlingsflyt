package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.flate

import java.time.LocalDate

data class RimeligGrunnVurderingDto(
    val harRimeligGrunn: Boolean,
    val fraDato: LocalDate,
    val begrunnelse: String,
)
