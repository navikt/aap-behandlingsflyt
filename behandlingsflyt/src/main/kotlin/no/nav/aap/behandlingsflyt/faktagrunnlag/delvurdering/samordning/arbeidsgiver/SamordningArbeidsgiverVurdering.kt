package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.arbeidsgiver

import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.StandardSammenslåere
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import java.time.LocalDate
import java.time.LocalDateTime

data class SamordningArbeidsgiverGrunnlag(
    val vurdering: SamordningArbeidsgiverVurdering,
) {
    fun tilTidslinje(): Tidslinje<Unit>{
        return vurdering.tilTidslinje()
    }
}

data class SamordningArbeidsgiverVurdering(
    val begrunnelse: String,
    val perioder: List<Periode>,
    val vurdertAv: String,
    val vurdertTidspunkt: LocalDateTime? = null,
    val opprettetTid: LocalDateTime? = null,
)



data class SamordningArbeidsgiverVurderingerDTO(
    val begrunnelse: String,
    val perioder: List<Periode>,
)

fun SamordningArbeidsgiverVurdering.tilTidslinje(): Tidslinje<Unit> {
    val segmenter = perioder.map { periode -> Segment(
        periode,Unit)
     }
    return Tidslinje(segmenter )
}