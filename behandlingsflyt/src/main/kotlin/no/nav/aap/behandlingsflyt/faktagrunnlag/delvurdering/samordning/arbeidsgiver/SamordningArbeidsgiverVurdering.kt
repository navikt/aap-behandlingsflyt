package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.arbeidsgiver

import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import java.time.LocalDate
import java.time.LocalDateTime

data class SamordningArbeidsgiverGrunnlag(
    val vurdering: SamordningArbeidsgiverVurdering,
)

data class SamordningArbeidsgiverVurdering(
    val begrunnelse: String,
    val perioder: List<Periode>,
    val vurdertAv: String,
    val vurdertTidspunkt: LocalDateTime? = null,
    val opprettetTid: LocalDateTime? = null,
)

data class SamordningArbeidsgiverVurderingDTO(
    val vurdering: String,
    val fom: LocalDate,
    val tom: LocalDate,
)

fun SamordningArbeidsgiverVurdering.tilTidslinje(): Tidslinje<SamordningArbeidsgiverVurdering> {
    val periode = Periode(fom, tom)
    return Tidslinje(periode, this)
}