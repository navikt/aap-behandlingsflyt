package no.nav.aap.behandlingsflyt.behandling.beregning

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.InntektPerÅr
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.ManuellInntektVurdering
import no.nav.aap.komponenter.verdityper.Beløp
import java.math.BigDecimal
import java.time.Year
import java.time.YearMonth

object InntektValidering {

    // Forskjell på inntektene kan ikke være større enn 100 kr – sanity-sjekk før vi bruker månedsinntekter
    fun validerSummertInntekt(år: Year, månedsinntekter: Map<YearMonth, Beløp>, årsInntekter: Set<InntektPerÅr>) {
        val summertMånedsinntekt = Beløp(månedsinntekter.values.sumOf { it.verdi })
        val årsInntekt = årsInntekter.firstOrNull { it.år == år }?.beløp ?: return
        val differanse = (årsInntekt.verdi.stripTrailingZeros() - summertMånedsinntekt.verdi.stripTrailingZeros()).abs()
        require(differanse < BigDecimal(100)) {
            "Håndterer ikke å støtte forskjellig inntekt fra A-Inntekt og POPP. Fikk $summertMånedsinntekt for år $år, men fant $årsInntekt"
        }
    }

    fun validerAtDelperioderDekkerHeleÅret(manuelleInntekter: Set<ManuellInntektVurdering>) {
        manuelleInntekter
            .filter { it.månedsPeriode != null }
            .groupBy { it.år }
            .forEach { (år, vurderinger) ->
                val perioder = vurderinger.mapNotNull { it.månedsPeriode }
                if (perioder.isEmpty()) return@forEach

                val sortert = perioder.sortedBy { it.fom }
                val forventetFom = YearMonth.of(år.value, 1)
                val forventetTom = YearMonth.of(år.value, 12)

                require(YearMonth.from(sortert.first().fom) == forventetFom) {
                    "Delperioder for $år må starte i ${forventetFom}."
                }
                require(YearMonth.from(sortert.last().tom) == forventetTom) {
                    "Delperioder for $år må dekke hele året og slutte i ${forventetTom}."
                }
                sortert.zipWithNext().forEach { (forrige, neste) ->
                    require(YearMonth.from(neste.fom) == YearMonth.from(forrige.tom).plusMonths(1)) {
                        "Delperioder for $år må være sammenhengende uten hull eller overlapp."
                    }
                }
            }
    }
}
