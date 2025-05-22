package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.refusjonkrav

import java.time.LocalDate
import java.time.LocalDateTime

data class RefusjonkravVurdering(
    val harKrav: Boolean,
    val fom: LocalDate?,
    val tom: LocalDate?,
    val vurdertAv: String,
    val opprettetTid: LocalDateTime? = null,
)

data class RefusjonkravVurderingDto(
    val harKrav: Boolean,
    val fom: LocalDate?,
    val tom: LocalDate?,
)