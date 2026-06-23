package no.nav.aap.behandlingsflyt.behandling.beregning

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.InntektPerÅr
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre.Uføre
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.komponenter.verdityper.Prosent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.Year
import java.time.YearMonth

class UføreInntektUtlederTest {
    private val ytterligereNedsattDato = LocalDate.of(2023, 4, 1)

    @Test
    fun `år med endring i uføregrad midt i året og avvik mellom A-inntekt og årsinntekt krever manuell periodeinntekt`() {
        val uføregrader = setOf(
            Uføre(virkningstidspunkt = LocalDate.of(2022, 1, 1), uføregrad = Prosent.`0_PROSENT`),
            Uføre(virkningstidspunkt = LocalDate.of(2022, 3, 1), uføregrad = Prosent.`50_PROSENT`),
        )
        // A-inntekt mangler for deler av 2022 -> summen avviker fra årsinntekt (POPP)
        val månedsinntekter = (1..6).map { Månedsinntekt(YearMonth.of(2022, it), Beløp(10_000)) }.toSet()
        val årsInntekter = setOf(InntektPerÅr(2022, Beløp(200_000)))

        val resultat = UføreInntektUtleder.finnÅrSomKreverManuellPeriodeinntekt(
            uføregrader = uføregrader,
            inntektPerMåned = månedsinntekter,
            årsInntekter = årsInntekter,
            ytterligereNedsattDato = ytterligereNedsattDato,
        )

        assertThat(resultat).contains(Year.of(2022))
    }

    @Test
    fun `år med konstant uføregrad hele året krever ikke manuell periodeinntekt`() {
        val uføregrader = setOf(
            Uføre(virkningstidspunkt = LocalDate.of(2022, 1, 1), uføregrad = Prosent.`50_PROSENT`),
        )
        val månedsinntekter = (1..12).map { Månedsinntekt(YearMonth.of(2022, it), Beløp(10_000)) }.toSet()
        val årsInntekter = setOf(InntektPerÅr(2022, Beløp(120_000)))

        val resultat = UføreInntektUtleder.finnÅrSomKreverManuellPeriodeinntekt(
            uføregrader = uføregrader,
            inntektPerMåned = månedsinntekter,
            årsInntekter = årsInntekter,
            ytterligereNedsattDato = ytterligereNedsattDato,
        )

        assertThat(resultat).doesNotContain(Year.of(2022))
    }

    @Test
    fun `år med endring i uføregrad men uten avvik i inntekt krever ikke manuell periodeinntekt`() {
        val uføregrader = setOf(
            Uføre(virkningstidspunkt = LocalDate.of(2022, 1, 1), uføregrad = Prosent.`0_PROSENT`),
            Uføre(virkningstidspunkt = LocalDate.of(2022, 3, 1), uføregrad = Prosent.`50_PROSENT`),
        )
        val månedsinntekter = (1..12).map { Månedsinntekt(YearMonth.of(2022, it), Beløp(10_000)) }.toSet()
        val årsInntekter = setOf(InntektPerÅr(2022, Beløp(120_000)))

        val resultat = UføreInntektUtleder.finnÅrSomKreverManuellPeriodeinntekt(
            uføregrader = uføregrader,
            inntektPerMåned = månedsinntekter,
            årsInntekter = årsInntekter,
            ytterligereNedsattDato = ytterligereNedsattDato,
        )

        assertThat(resultat).doesNotContain(Year.of(2022))
    }

    @Test
    fun `utledDelperioder gir én delperiode per uføregrad-segment innen året`() {
        val uføregrader = setOf(
            Uføre(virkningstidspunkt = LocalDate.of(2022, 1, 1), uføregrad = Prosent.`0_PROSENT`),
            Uføre(virkningstidspunkt = LocalDate.of(2022, 3, 1), uføregrad = Prosent.`50_PROSENT`),
        )

        val delperioder = UføreInntektUtleder.utledDelperioder(uføregrader, Year.of(2022))

        assertThat(delperioder).hasSize(2)
        assertThat(delperioder.first().periode.fom).isEqualTo(LocalDate.of(2022, 1, 1))
        assertThat(delperioder.first().periode.tom).isEqualTo(LocalDate.of(2022, 2, 28))
        assertThat(delperioder.first().uføregrad).isEqualTo(Prosent.`0_PROSENT`)
        assertThat(delperioder.last().periode.fom).isEqualTo(LocalDate.of(2022, 3, 1))
        assertThat(delperioder.last().periode.tom).isEqualTo(LocalDate.of(2022, 12, 31))
        assertThat(delperioder.last().uføregrad).isEqualTo(Prosent.`50_PROSENT`)
    }

    @Test
    fun `utledDelperioder fyller perioden før første uføre-segment med 0 prosent`() {
        // Kun ett segment fra 1. mars -> jan-feb skal fylles med 0 prosent (som i skissen).
        val uføregrader = setOf(
            Uføre(virkningstidspunkt = LocalDate.of(2022, 3, 1), uføregrad = Prosent.`50_PROSENT`),
        )

        val delperioder = UføreInntektUtleder.utledDelperioder(uføregrader, Year.of(2022))

        assertThat(delperioder).hasSize(2)
        assertThat(delperioder.first().periode.fom).isEqualTo(LocalDate.of(2022, 1, 1))
        assertThat(delperioder.first().periode.tom).isEqualTo(LocalDate.of(2022, 2, 28))
        assertThat(delperioder.first().uføregrad).isEqualTo(Prosent.`0_PROSENT`)
        assertThat(delperioder.last().periode.fom).isEqualTo(LocalDate.of(2022, 3, 1))
        assertThat(delperioder.last().periode.tom).isEqualTo(LocalDate.of(2022, 12, 31))
        assertThat(delperioder.last().uføregrad).isEqualTo(Prosent.`50_PROSENT`)
    }

    @Test
    fun `delperioder kan utledes for et år med to uføre-segmenter i historikken`() {
        val uføregrader = setOf(
            Uføre(virkningstidspunkt = LocalDate.of(2024, 1, 1), uføregrad = Prosent.`50_PROSENT`),
            Uføre(virkningstidspunkt = LocalDate.of(2025, 3, 1), uføregrad = Prosent(80)),
        )
        val månedsinntekter = (1..12).map { Månedsinntekt(YearMonth.of(2025, it), Beløp(10_000)) }.toSet()
        val årsInntekter = setOf(InntektPerÅr(2025, Beløp(200_000)))

        val relevanteÅr = UføreInntektUtleder.finnÅrSomKreverManuellPeriodeinntekt(
            uføregrader = uføregrader,
            inntektPerMåned = månedsinntekter,
            årsInntekter = årsInntekter,
            ytterligereNedsattDato = LocalDate.of(2026, 6, 1),
        )

        assertThat(relevanteÅr).contains(Year.of(2025))

        val delperioder = UføreInntektUtleder.utledDelperioder(uføregrader, Year.of(2025))

        assertThat(delperioder).hasSize(2)
        assertThat(delperioder.first().periode.fom).isEqualTo(LocalDate.of(2025, 1, 1))
        assertThat(delperioder.first().periode.tom).isEqualTo(LocalDate.of(2025, 2, 28))
        assertThat(delperioder.first().uføregrad).isEqualTo(Prosent.`50_PROSENT`)
        assertThat(delperioder.last().periode.fom).isEqualTo(LocalDate.of(2025, 3, 1))
        assertThat(delperioder.last().periode.tom).isEqualTo(LocalDate.of(2025, 12, 31))
        assertThat(delperioder.last().uføregrad).isEqualTo(Prosent(80))
    }
}