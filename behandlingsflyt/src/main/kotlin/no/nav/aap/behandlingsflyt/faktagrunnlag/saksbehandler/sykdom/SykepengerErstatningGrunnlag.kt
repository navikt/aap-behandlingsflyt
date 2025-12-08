package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom

import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.StandardSammenslåere
import no.nav.aap.komponenter.tidslinje.Tidslinje
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
    val vurderingerPerBehandling = this.groupBy { it.vurdertIBehandling.id }
    val behandlingIdSortert = vurderingerPerBehandling.keys.sortedBy { vurderingerPerBehandling[it]!!.first().vurdertIBehandling.id }

    return behandlingIdSortert
        .map { Tidslinje(initSegmenter = vurderingerPerBehandling[it]!!.somTidlinjeSegmenter()) }
        .fold(Tidslinje<SykepengerVurdering>()) { acc, curr ->
            acc.kombiner(curr, StandardSammenslåere.prioriterHøyreSideCrossJoin())
        }
        .komprimer()
        .map { periode, vurdering -> vurdering.copy(gjelderFra = periode.fom, gjelderTom = periode.tom.tilTidOrNull()) }
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
