package no.nav.aap.behandlingsflyt.behandling.beregning.år

import no.nav.aap.behandlingsflyt.behandling.beregning.Månedsinntekt
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.år.BeregningInput
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.år.Inntektsbehov
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.InntektPerÅr
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre.Uføre
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade.Yrkesskade
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade.Yrkesskader
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningYrkeskaderBeløpVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningstidspunktVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.YrkesskadeBeløpVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.YrkesskadeSak
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.Yrkesskadevurdering
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.komponenter.verdityper.Prosent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.LocalDate
import java.time.Year
import java.time.YearMonth

class InntektsbehovTest {
    @Test
    fun `henter ut relevante år, nemlig tre år før nedsettelsen`() {
        val nedsettelsesDato = LocalDate.now().withYear(2005)
        val årsInntekter = setOf(
            InntektPerÅr(nedsettelsesDato.plusYears(1).year, Beløp(123)),
            InntektPerÅr(nedsettelsesDato.minusYears(0).year, Beløp(125)),
            InntektPerÅr(nedsettelsesDato.minusYears(1).year, Beløp(126)),
            InntektPerÅr(nedsettelsesDato.minusYears(2).year, Beløp(127)),
            InntektPerÅr(nedsettelsesDato.minusYears(3).year, Beløp(128)),
            InntektPerÅr(nedsettelsesDato.minusYears(4).year, Beløp(129))
        )
        val forOrdinær = Inntektsbehov(
            BeregningInput(
                nedsettelsesDato,
                årsInntekter = årsInntekter,
                uføregrad = setOf(Uføre(LocalDate.now(), Prosent.`0_PROSENT`)),
                yrkesskadevurdering = null,
                beregningGrunnlag = null,
                registrerteYrkesskader = null,
                inntektsPerioder = inntektsPerioder(årsInntekter)
            )
        ).utledForOrdinær()

        assertThat(forOrdinær).containsExactlyInAnyOrder(
            InntektPerÅr(2004, Beløp(126)), InntektPerÅr(2003, Beløp(127)), InntektPerÅr(2002, Beløp(128))
        )
    }

    @Test
    fun `skal utlede de tre forutgående kalenderårene fra nedsettelsesdato`() {
        val nedsettelsesDato = LocalDate.now().minusYears(3)
        val relevanteÅr = Inntektsbehov.utledAlleRelevanteÅr(nedsettelsesDato, null)

        val nedsattYear = Year.of(nedsettelsesDato.year)

        assertThat(relevanteÅr).hasSize(3)
        assertThat(relevanteÅr).containsExactlyInAnyOrder(
            nedsattYear.minusYears(3), nedsattYear.minusYears(2), nedsattYear.minusYears(1)
        )
    }

    @Test
    fun `skal utlede de tre forutgående kalenderårene fra nedsettelsesdato og tre forutgående kalenderårene fra ytterligere nedsattdato`() {
        val nedsettelsesDato = LocalDate.now().minusYears(6)
        val ytterligereNedsattDato = LocalDate.now().minusYears(2)
        val relevanteÅr = Inntektsbehov.utledAlleRelevanteÅr(nedsettelsesDato, ytterligereNedsattDato)

        val nedsattYear = Year.of(nedsettelsesDato.year)
        val ytterligereNedsattYear = Year.of(ytterligereNedsattDato.year)

        assertThat(relevanteÅr).hasSize(6)
        assertThat(relevanteÅr).containsExactlyInAnyOrder(
            nedsattYear.minusYears(3),
            nedsattYear.minusYears(2),
            nedsattYear.minusYears(1),
            ytterligereNedsattYear.minusYears(3),
            ytterligereNedsattYear.minusYears(2),
            ytterligereNedsattYear.minusYears(1)
        )
    }

    @Test
    fun `om det finnes uføredata, skal det oppgis`() {
        val nedsettelsesDato = LocalDate.now().minusYears(3)
        val inntektsbehov = Inntektsbehov(
            BeregningInput(
                nedsettelsesDato,
                årsInntekter = emptySet(),
                uføregrad = setOf(Uføre(LocalDate.now().minusYears(10), Prosent.`30_PROSENT`)),
                yrkesskadevurdering = null,
                registrerteYrkesskader = null,
                beregningGrunnlag = BeregningGrunnlag(
                    tidspunktVurdering = BeregningstidspunktVurdering(
                        begrunnelse = "begrunnelse",
                        nedsattArbeidsevneEllerStudieevneDato = nedsettelsesDato,
                        ytterligereNedsattArbeidsevneDato = LocalDate.now().minusYears(10),
                        ytterligereNedsattBegrunnelse = "begrunnelse",
                        vurdertAv = "saksbehandler"
                    ), yrkesskadeBeløpVurdering = null
                ),
                inntektsPerioder = emptySet()
            )
        )

        assertThat(inntektsbehov.finnesUføreData()).isTrue()
    }

    @Test
    fun `bruker manuell dato for yrkesskade om den er null fra register`() {
        val nedsettelsesDato = LocalDate.now().minusYears(3)
        val inntektsbehov = Inntektsbehov(
            BeregningInput(
                nedsettelsesDato,
                årsInntekter = emptySet(),
                uføregrad = setOf(Uføre(LocalDate.now(), Prosent.`30_PROSENT`)),
                yrkesskadevurdering = Yrkesskadevurdering(
                    begrunnelse = "...",
                    relevanteSaker = listOf(YrkesskadeSak("123", LocalDate.of(2023, 1, 1))),
                    erÅrsakssammenheng = true,
                    andelAvNedsettelsen = Prosent(70),
                    vurdertAv = "Jojo Joyes",
                ),
                registrerteYrkesskader = Yrkesskader(
                    listOf(
                        Yrkesskade(
                            ref = "123", saksnummer = 0, kildesystem = "KLVN", skadedato = null
                        )
                    )
                ),
                beregningGrunnlag = BeregningGrunnlag(
                    tidspunktVurdering = BeregningstidspunktVurdering(
                        begrunnelse = "begrunnelse",
                        nedsattArbeidsevneEllerStudieevneDato = nedsettelsesDato,
                        ytterligereNedsattArbeidsevneDato = LocalDate.now().minusYears(10),
                        ytterligereNedsattBegrunnelse = "begrunnelse",
                        vurdertAv = "saksbehandler"
                    ), yrkesskadeBeløpVurdering = BeregningYrkeskaderBeløpVurdering(
                        vurderinger = listOf(
                            YrkesskadeBeløpVurdering(
                                antattÅrligInntekt = Beløp(1234),
                                referanse = "123",
                                begrunnelse = "...",
                                vurdertAv = "meg",
                            )
                        )
                    )
                ),
                inntektsPerioder = emptySet()
            )
        )

        assertThat(inntektsbehov.skadetidspunkt()).isEqualTo(LocalDate.of(2023, 1, 1))
    }

    @Test
    fun `skal utlede de tre forutgående kalenderårene basert på datoene i beregningsgrunnlaget`() {
        val beregningGrunnlag = BeregningGrunnlag(
            tidspunktVurdering = BeregningstidspunktVurdering(
                begrunnelse = "begrunnelse",
                nedsattArbeidsevneEllerStudieevneDato = 1 januar 2025,
                ytterligereNedsattBegrunnelse = null,
                ytterligereNedsattArbeidsevneDato = 1 januar 2025,
                vurdertAv = "saksbehandler",
            ), yrkesskadeBeløpVurdering = null
        )

        val relevanteÅr = Inntektsbehov.utledAlleRelevanteÅr(beregningGrunnlag)
        assertThat(relevanteÅr).containsExactlyInAnyOrder(
            Year.of(2024), Year.of(2023), Year.of(2022)
        )
    }

    @Test
    fun `skal utlede de tre forutgående kalenderårene for både nedsettelsesdato og ytterligereNedsattArbeidsevneDato`() {
        val nedsettelsesDato = 1 januar 2025
        val ytterligereNedsattArbeidsevneDato = 1 januar 2020

        val relevanteÅr = Inntektsbehov.utledAlleRelevanteÅr(nedsettelsesDato, ytterligereNedsattArbeidsevneDato)
        assertThat(relevanteÅr).containsExactlyInAnyOrder(
            Year.of(2024),
            Year.of(2023),
            Year.of(2022),
            Year.of(2019),
            Year.of(2018),
            Year.of(2017),
        )
    }

    @Test
    fun `skal feile når forskjellen mellom inntekt fra A-inntekt og PESYS er mer enn 1kr`() {
        val årsInntekter = setOf(
            InntektPerÅr(2022, Beløp(500000)),
            InntektPerÅr(2021, Beløp(400000)),
            InntektPerÅr(2020, Beløp(300000))
        )
        val inputMedForStorForskjell = BeregningInput(
            nedsettelsesDato = LocalDate.of(2015, 1, 1),
            årsInntekter = årsInntekter,
            uføregrad = setOf(Uføre(LocalDate.now().minusYears(5), Prosent(30))),
            yrkesskadevurdering = null,
            beregningGrunnlag = BeregningGrunnlag(
                tidspunktVurdering = BeregningstidspunktVurdering(
                    begrunnelse = "test",
                    nedsattArbeidsevneEllerStudieevneDato = LocalDate.of(2023, 1, 1),
                    ytterligereNedsattBegrunnelse = "test2",
                    ytterligereNedsattArbeidsevneDato = LocalDate.of(2023, 1, 1),
                    vurdertAv = "saksbehandler"
                ), yrkesskadeBeløpVurdering = null
            ),
            registrerteYrkesskader = null,
            inntektsPerioder = inntektsPerioder(
                setOf(
                    InntektPerÅr(2022, Beløp(500002)),
                    InntektPerÅr(2021, Beløp(400000)),
                    InntektPerÅr(2020, Beløp(300000))
                )
            )
        )

        // 2kr forskjell, da skal feil kastes
        assertThrows<IllegalArgumentException> { Inntektsbehov(inputMedForStorForskjell).validerSummertInntekt() }

        val inputMedLitenNokForskjell = BeregningInput(
            nedsettelsesDato = LocalDate.of(2015, 1, 1),
            årsInntekter = årsInntekter,
            uføregrad = setOf(Uføre(LocalDate.now().minusYears(5), Prosent(30))),
            yrkesskadevurdering = null,
            beregningGrunnlag = BeregningGrunnlag(
                tidspunktVurdering = BeregningstidspunktVurdering(
                    begrunnelse = "test",
                    nedsattArbeidsevneEllerStudieevneDato = LocalDate.of(2023, 1, 1),
                    ytterligereNedsattBegrunnelse = "test2",
                    ytterligereNedsattArbeidsevneDato = LocalDate.of(2023, 1, 1),
                    vurdertAv = "saksbehandler"
                ), yrkesskadeBeløpVurdering = null
            ),
            registrerteYrkesskader = null,
            inntektsPerioder = inntektsPerioder(
                setOf(
                    InntektPerÅr(2022, Beløp(BigDecimal(499999.5))),
                    InntektPerÅr(2021, Beløp(400000)),
                    InntektPerÅr(2020, Beløp(300000))
                )
            )
        )

        // 0.5kr forskjell, skal ikke feile
        assertDoesNotThrow { Inntektsbehov(inputMedLitenNokForskjell).validerSummertInntekt() }

    }

    private fun inntektsPerioder(inntektPerÅr: Set<InntektPerÅr>): Set<Månedsinntekt> {
        return inntektPerÅr.map {
            Månedsinntekt(
                // TODO
                YearMonth.of(it.år.value, 1), it.beløp
            )
        }.toSet()
    }
}
