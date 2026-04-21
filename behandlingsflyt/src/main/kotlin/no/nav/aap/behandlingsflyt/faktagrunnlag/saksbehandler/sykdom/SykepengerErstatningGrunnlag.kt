package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom

import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.StandardSammenslåere
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.somTidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Tid
import java.time.LocalDate


data class SykepengerErstatningGrunnlag(
    val vurderinger: List<SykepengerVurdering>
) {
    fun somTidslinje(kravDato: LocalDate, sisteMuligDagMedYtelse: LocalDate): Tidslinje<SykepengerVurdering> {
        return vurderinger
            .somTidslinje()
            .begrensetTil(Periode(fom = kravDato, tom = sisteMuligDagMedYtelse))
    }
}

fun List<SykepengerVurdering>.somTidslinje(): Tidslinje<SykepengerVurdering> {
    return this
        .groupBy { it.vurdertIBehandling }
        .values
        .sortedBy { it[0].vurdertTidspunkt }
        .flatMap { it.sortedBy { it.gjelderFra } }
        .somTidslinje { Periode(it.gjelderFra, it.gjelderTom ?: Tid.MAKS) }
        .komprimer()
        .begrensetTil(Periode(Tid.MIN, Tid.MAKS))
}

fun List<SykepengerVurdering>.somTidlinjeSegmenter(): List<Segment<SykepengerVurdering>> =
    this.map {
        Segment(
            periode = Periode(fom = it.gjelderFra, tom = it.gjelderTom ?: Tid.MAKS),
            verdi = it
        )
    }


fun LocalDate?.tilTidOrNull(): LocalDate? {
    if (this == null || this == Tid.MAKS) {
        return null
    }
    return this
}
