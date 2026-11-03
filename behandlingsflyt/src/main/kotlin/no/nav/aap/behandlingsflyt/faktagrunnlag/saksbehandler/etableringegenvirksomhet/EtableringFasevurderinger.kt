package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.gjeldendeVurderinger
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Tid

/** Avklarer overstyringer før faseperiodene flytter fom/tom, slik at eldre vurderinger ikke fortrenges eller gjenoppstår. */
fun List<EtableringEgenVirksomhetVurdering>.gjeldendeFasevurderinger(): List<EtableringEgenVirksomhetVurdering> {
    val segmenter = gjeldendeVurderinger().segmenter().toList()
    val gjeldende = segmenter.map { it.verdi }.toSet()
    return filter { it in gjeldende }.flatMap { vurdering ->
        val andreVurderinger = segmenter.filter { it.verdi != vurdering }.map { it.periode }
        listOf(vurdering).tilNyttFormat().flatMap { it.utenPerioder(andreVurderinger) }
    }
}

fun EtableringEgenVirksomhetVurdering.utenPerioder(
    perioder: List<Periode>
): List<EtableringEgenVirksomhetVurdering> =
    perioder.fold(listOf(Periode(fom, tom ?: Tid.MAKS))) { gjenværende, periode ->
        gjenværende.flatMap { it.minus(periode).toList() }
    }.map { periode ->
        copy(
            fom = periode.fom,
            tom = periode.tom.takeUnless { it == Tid.MAKS && tom == null },
        )
    }
