package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.samordning.refusjonskrav

import java.time.LocalDate

data class TjenestepensjonRefusjonskravVurdering (
    val harKrav: Boolean,
    val fom: LocalDate?,
    val tom: LocalDate?
)