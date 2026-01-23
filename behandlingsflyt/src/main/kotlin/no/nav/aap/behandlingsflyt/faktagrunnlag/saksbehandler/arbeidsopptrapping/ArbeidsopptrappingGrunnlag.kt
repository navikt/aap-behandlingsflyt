package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsopptrapping

import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.somTidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Tid
import java.time.LocalDate

data class ArbeidsopptrappingGrunnlag(
    val vurderinger: List<ArbeidsopptrappingVurdering>
) {
    fun gjeldendeVurderinger(maksDato: LocalDate = Tid.MAKS): Tidslinje<ArbeidsopptrappingVurdering> {
        return vurderinger
            .groupBy { it.vurdertIBehandling }
            .values
            .sortedBy { it[0].opprettetTid }
            .flatMap { it.sortedBy { it.vurderingenGjelderFra } }
            .somTidslinje { Periode(it.vurderingenGjelderFra, it.vurderingenGjelderTil ?: maksDato) }
    }
}

fun ArbeidsopptrappingGrunnlag?.innvilgedePerioder(): List<Periode> {
    val vurderinger = this?.vurderinger.orEmpty()
        .sortedBy { it.vurderingenGjelderFra }

    val vurderingMedPerioder = vurderinger.mapIndexed { index, vurdering ->
        val fom = vurdering.vurderingenGjelderFra
        val tom = when {
            vurdering.vurderingenGjelderTil != null ->
                vurdering.vurderingenGjelderTil

            index < vurderinger.lastIndex ->
                vurderinger[index + 1].vurderingenGjelderFra

            else ->
                vurdering.vurderingenGjelderFra.plusMonths(12)
        }
        vurdering to Periode(fom, tom)
    }

    return vurderingMedPerioder
        .filter { (v) -> v.rettPaaAAPIOpptrapping && v.reellMulighetTilOpptrapping }
        .map { (_, periode) -> periode }
}