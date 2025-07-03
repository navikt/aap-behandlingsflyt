package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsgiver

import java.time.LocalDate
import java.time.LocalDateTime

data class SamordningArbeidsgiverVurdering(
    val vurdering: String,
    val fom: LocalDate?,
    val tom: LocalDate?,
    val vurdertAv: String,
    val opprettetTid: LocalDateTime? = null,
)

data class SamordningArbeidsgiverVurderingDto(
    val vurdering: String,
    val fom: LocalDate?,
    val tom: LocalDate?,
)