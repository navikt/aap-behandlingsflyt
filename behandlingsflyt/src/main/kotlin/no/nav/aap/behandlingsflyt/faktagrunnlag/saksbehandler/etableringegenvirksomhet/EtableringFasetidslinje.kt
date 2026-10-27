package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.gjeldendeVurderinger
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Tid

/**
 * Hindrer at overstyrte perioder dukker opp igjen ved oppdeling av gamle vurderinger.
 * Kan erstattes med [gjeldendeVurderinger] etter migrering, når frontend og backend kun bruker nytt periodeformat.
 * V0/V1-regelsettet beholdes uavhengig av periodeformat.
 */
fun List<EtableringEgenVirksomhetVurdering>.somFasetidslinje(): Tidslinje<EtableringEgenVirksomhetVurdering> {
    val prioriterte = groupBy { it.vurdertIBehandling }.values
        .sortedBy { it.first().opprettet }
        .flatMap { gruppe -> gruppe.sortedBy { it.fom } }

    return prioriterte.flatMapIndexed { index, vurdering ->
        val overstyrendePerioder = prioriterte.drop(index + 1)
            .map { Periode(it.fom, it.tom ?: Tid.MAKS) }
        listOf(vurdering).tilNyttFormat().flatMap { it.utenPerioder(overstyrendePerioder) }
    }.gjeldendeVurderinger()
}

fun EtableringEgenVirksomhetVurdering.utenPerioder(
    overstyrendePerioder: List<Periode>
): List<EtableringEgenVirksomhetVurdering> =
    overstyrendePerioder.fold(listOf(Periode(fom, tom ?: Tid.MAKS))) { perioder, overstyrende ->
        perioder.flatMap { it.minus(overstyrende).toList() }
    }.map { periode ->
        copy(fom = periode.fom, tom = periode.tom.takeUnless { it == Tid.MAKS && tom == null })
    }
