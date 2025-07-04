package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.arbeidsgiver

import java.time.LocalDate
import java.time.LocalDateTime

data class SamordningArbeidsgiverGrunnlag(
    val vurdering: SamordningArbeidsgiverVurdering,
)

data class SamordningArbeidsgiverVurdering(
    val begrunnelse: String,
    val fom: LocalDate,
    val tom: LocalDate,
    val vurdertAv: String,
    val vurdertTidspunkt: LocalDateTime? = null,
    val opprettetTid: LocalDateTime? = null,
)

data class SamordningArbeidsgiverVurderingDTO(
    val vurdering: String,
    val fom: LocalDate,
    val tom: LocalDate,
)