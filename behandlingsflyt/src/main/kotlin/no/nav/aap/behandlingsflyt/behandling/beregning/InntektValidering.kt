package no.nav.aap.behandlingsflyt.behandling.beregning

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.InntektPerÅr
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
            "Håndterer ikke å støtte forskjellig inntekt fra A-Inntekt og PESYS. Fikk $summertMånedsinntekt for år $år, men fant $årsInntekt"
        }
    }
}
