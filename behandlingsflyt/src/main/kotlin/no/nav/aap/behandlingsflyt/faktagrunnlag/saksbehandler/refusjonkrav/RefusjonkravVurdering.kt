package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.refusjonkrav

import java.time.LocalDate

data class RefusjonkravVurdering(
    val harKrav: Boolean,
    val fom: LocalDate?,
    val tom: LocalDate?
)