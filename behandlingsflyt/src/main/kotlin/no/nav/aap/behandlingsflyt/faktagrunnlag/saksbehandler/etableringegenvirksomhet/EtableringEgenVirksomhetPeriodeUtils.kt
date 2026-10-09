package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet

import no.nav.aap.behandlingsflyt.behandling.underveis.regler.Hverdager
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.Hverdager.Companion.antallHverdager
import no.nav.aap.komponenter.type.Periode
import java.time.LocalDate

internal const val MAKS_UTVIKLING_HVERDAGER = 131
internal const val MAKS_OPPSTART_HVERDAGER = 66

/**
    * Justerer perioder slik at de ikke overlapper hverandre.
    * Hvis en periode starter dagen etter en annen periode, vil den første perioden få tom satt til dagen før den andre perioden starter.
*/
fun justerEtableringPerioder(
    vurderinger: List<EtableringEgenVirksomhetVurdering>
): List<EtableringEgenVirksomhetVurdering> {
    val periodiserte = vurderinger
        .groupBy { it.fase }
        .flatMap { (_, faseVurderinger) ->
        val sortert = faseVurderinger.sortedBy { it.fom }

        sortert.mapIndexed { index, vurdering ->
            val nesteFom = sortert.getOrNull(index + 1)?.fom
            if (nesteFom != null) {
                vurdering.copy(tom = nesteFom.minusDays(1))
            } else {
                vurdering
            }
        }
    }

    return periodiserte.sortedWith(
        compareBy<EtableringEgenVirksomhetVurdering> { it.fom }
            .thenBy { it.fase?.name })
}

fun beregnTomForSistePeriode(
    vurderinger: List<EtableringEgenVirksomhetVurdering>,
    sisteVurdering: EtableringEgenVirksomhetVurdering
): LocalDate {
    val fase = requireNotNull(sisteVurdering.fase)

    val maksHverdager = fase.maksHveradager
    val brukteHverdager = vurderinger
        .filter {
            it.fase == fase &&
                    it.fom.isBefore(sisteVurdering.fom)
        }
        .sumOf {
            val tom = requireNotNull(it.tom) {
                "Tidligere vurdering må ha beregnet tom"
            }

            (it.fom to tom)
                .tilPeriode()
                .antallHverdager()
                .asInt
        }


    val gjenståendeHverdager = maksHverdager - brukteHverdager
    require(gjenståendeHverdager > 0) {
        "Kvoten for $fase er brukt opp. " +
                "Brukt: $brukteHverdager, maks: $maksHverdager"
    }

    //skal heller returne denne?
    return Hverdager(gjenståendeHverdager)
        .fraOgMed(sisteVurdering.fom)

    //val tomEtterFaseperiode = when (fase) {
    //    EtableringFase.OPPSTART ->
    //        sisteVurdering.fom.plusMonths(3).minusDays(1)

    //    EtableringFase.UTVIKLING ->
    //        sisteVurdering.fom.plusMonths(6).minusDays(1)
    //}

    //return minOf(tomEtterKvoten, tomEtterFaseperiode)
}

private fun Pair<LocalDate, LocalDate>.tilPeriode(): Periode = Periode(first, second)
