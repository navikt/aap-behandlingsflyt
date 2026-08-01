package no.nav.aap.samordning.arbeidsgiver

import java.time.LocalDateTime
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Bruker

data class SamordningArbeidsgiverGrunnlag(
    val vurdering: SamordningArbeidsgiverVurdering,
) {
    fun tilTidslinje(): Tidslinje<Unit> {
        return vurdering.tilTidslinje()
    }
}

data class SamordningArbeidsgiverVurdering(
    val begrunnelse: String,
    val perioder: List<Periode>,
    val vurdertAv: Bruker,
    val vurdertTidspunkt: LocalDateTime? = null,
    val opprettetTid: LocalDateTime? = null,
)

fun SamordningArbeidsgiverVurdering.tilTidslinje(): Tidslinje<Unit> {
    val segmenter = perioder.map { periode ->
        Segment(
            periode, Unit
        )
    }
    return Tidslinje(segmenter).komprimer()
}