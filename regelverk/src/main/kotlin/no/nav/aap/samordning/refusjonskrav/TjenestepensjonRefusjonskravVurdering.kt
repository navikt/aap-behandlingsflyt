package no.nav.aap.samordning.refusjonskrav

import java.time.LocalDate

data class TjenestepensjonRefusjonskravVurdering (
    val harKrav: Boolean,
    val fom: LocalDate?,
    val tom: LocalDate?,
    val begrunnelse: String
)