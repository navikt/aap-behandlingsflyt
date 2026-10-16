package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet

import no.nav.aap.behandlingsflyt.behandling.etableringegenvirksomhet.VirksomhetEtableringResultat
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.Hverdager
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.Hverdager.Companion.antallHverdager
import no.nav.aap.komponenter.type.Periode
import java.time.LocalDate

/**
 * Justerer perioder slik at de ikke overlapper hverandre.
 * Hvis en periode starter dagen etter en annen periode, vil den første perioden få tom satt til dagen før den andre perioden starter.
 *
 * Tror denne blir unødvendig om vi migrerer data først??
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

sealed interface TilOgMedBeregning {
    data class Beregnet(val tom: LocalDate) : TilOgMedBeregning
    data class Feil(val feilmelding: String, val feilObjekt: VirksomhetEtableringResultat.Ugyldig) :
        TilOgMedBeregning
}

fun beregnTomForSistePeriode(
    vurderinger: List<EtableringEgenVirksomhetVurdering>,
    sisteVurdering: EtableringEgenVirksomhetVurdering
): TilOgMedBeregning {
    val fase = requireNotNull(sisteVurdering.fase)

    val maksHverdager = fase.maksHverdager
    val brukteHverdager = vurderinger
        .filter {
            it.fase == fase &&
                    it.fom.isBefore(sisteVurdering.fom)
        }
        .sumOf {
            val tom = it.tom ?: return TilOgMedBeregning.Feil(
                "Tidligere vurdering må ha beregnet tom",
                VirksomhetEtableringResultat.Ugyldig.MåHaTilOgMedDato
            )

            (it.fom to tom)
                .tilPeriode()
                .antallHverdager()
                .asInt
        }


    val gjenståendeHverdager = maksHverdager - brukteHverdager
    if (gjenståendeHverdager <= 0) {
        return TilOgMedBeregning.Feil(
            "Kvoten for $fase er brukt opp. Brukt: $brukteHverdager, maks: $maksHverdager",
            VirksomhetEtableringResultat.Ugyldig.OverstigerKvoter(
                fase = fase,
                brukteDager = brukteHverdager
            )
        )
    }

    return TilOgMedBeregning.Beregnet(Hverdager(gjenståendeHverdager).fraOgMed(sisteVurdering.fom))
}

private fun Pair<LocalDate, LocalDate>.tilPeriode(): Periode = Periode(first, second)
