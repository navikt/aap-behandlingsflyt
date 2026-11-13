package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.gjeldendeVurderinger
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Tid
import java.time.LocalDate

/**
 * Hindrer at overstyrte perioder dukker opp igjen ved oppdeling av gamle vurderinger.
 * Utvikling som startet før første gjeldende oppstart avsluttes i tidslinjen, uten å endre historikken.
 * V0/V1-regelsettet beholdes uavhengig av periodeformat.
 */
fun List<EtableringEgenVirksomhetVurdering>.somFasetidslinje(): Tidslinje<EtableringEgenVirksomhetVurdering> {
    val prioriterte = groupBy { it.vurdertIBehandling }.values
        .sortedBy { it.first().opprettet }
        .flatMap { gruppe -> gruppe.sortedBy { it.fom } }

    val gjeldende = prioriterte.utledFasetidslinje()
    val førsteOppstart = gjeldende.perioderForFase(EtableringFase.OPPSTART).minOfOrNull { it.fom }
        ?: return gjeldende

    return prioriterte.utledFasetidslinje(førsteOppstart)
}

fun avsluttUtviklingVedFørsteOppstart(
    vurdering: EtableringEgenVirksomhetVurdering,
    førsteOppstart: LocalDate?
): EtableringEgenVirksomhetVurdering =
    if (
        førsteOppstart != null &&
        vurdering.fase == EtableringFase.UTVIKLING &&
        vurdering.fom.isBefore(førsteOppstart)
    ) {
        vurdering.copy(tom = minOf(vurdering.tom ?: Tid.MAKS, førsteOppstart.minusDays(1)))
    } else {
        vurdering
    }

private fun List<EtableringEgenVirksomhetVurdering>.utledFasetidslinje(
    førsteOppstart: LocalDate? = null
): Tidslinje<EtableringEgenVirksomhetVurdering> =
    flatMapIndexed { index, vurdering ->
        val overstyrendePerioder = drop(index + 1)
            .map { Periode(it.fom, it.tom ?: Tid.MAKS) }
        listOf(vurdering).tilNyttFormat().flatMap { fasevurdering ->
            avsluttUtviklingVedFørsteOppstart(fasevurdering, førsteOppstart)
                .utenPerioder(overstyrendePerioder)
        }
    }.gjeldendeVurderinger()
