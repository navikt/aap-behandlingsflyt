package no.nav.aap.behandlingsflyt.behandling.beregning

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.InntektPerÅr
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre.Uføre
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade.Yrkesskade
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade.Yrkesskader
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.YrkesskadeBeløpVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.YrkesskadeSak
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.Yrkesskadevurdering
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.komponenter.verdityper.Prosent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

class BeregningsGrunnlagApiTest {

    @Test
    fun `hente ut beregningsgrunnlag fra API`() {
        val årsInntekter = setOf(
            InntektPerÅr(2022, Beløp(500000)),
            InntektPerÅr(2021, Beløp(400000)),
            InntektPerÅr(2020, Beløp(300000))
        )

        val beregning = Beregning(
            nedsettelsesDato = LocalDate.of(2023, 1, 1),
            årsInntekter = årsInntekter,
            uføregrad = setOf(Uføre(LocalDate.of(2023, 1, 1), Prosent(30))),
            yrkesskadevurdering = Yrkesskadevurdering(
                begrunnelse = "en begrunnelse",
                andelAvNedsettelsen = Prosent(30),
                erÅrsakssammenheng = true,
                relevanteSaker = listOf(YrkesskadeSak("yrkesskadesaken", null)),
                vurdertAv = "saksbehandler",
                vurdertTidspunkt = LocalDateTime.now()
            ),
            ytterligereNedsettelsesDato = LocalDate.of(2023, 1, 1),
            yrkesskadeBeløpVurderinger = listOf(
                YrkesskadeBeløpVurdering(
                    antattÅrligInntekt = Beløp(50000),
                    referanse = "yrkesskadesaken",
                    begrunnelse = "asdf",
                    vurdertAv = "saksbehandler"
                )
            ),
            registrerteYrkesskader = Yrkesskader(
                listOf(
                    Yrkesskade(
                        ref = "yrkesskadesaken",
                        saksnummer = 123,
                        kildesystem = "INFOTRYGD",
                        skadedato = LocalDate.of(2021, 1, 1)
                    )
                )
            ),
            inntektsPerioder = inntektsPerioder(årsInntekter)
        ).beregnBeregningsgrunnlag()

        val behandlingOpprettet = LocalDate.of(2024, 1, 1)

        val res = beregningDTO(beregning, behandlingOpprettet)

        val grunnlagYrkesskadeUføre = requireNotNull(res.grunnlagYrkesskadeUføre)
        fun inntektForÅr(år: String): BigDecimal {
            return grunnlagYrkesskadeUføre.uføreGrunnlag.inntekter.single { it.år == år }.inntektIKroner
        }
        assertThat(grunnlagYrkesskadeUføre.uføreGrunnlag.inntekter).hasSize(3)
        assertThat(inntektForÅr("2022")).isEqualByComparingTo(BigDecimal(500000))
        assertThat(inntektForÅr("2021")).isEqualByComparingTo(BigDecimal(400000))
        assertThat(inntektForÅr("2020")).isEqualByComparingTo(BigDecimal(300000))

        assertThat(res.gjeldendeGrunnbeløp.grunnbeløp.toInt()).isEqualTo(118620)
        assertThat(res.gjeldendeGrunnbeløp.dato).isEqualTo(behandlingOpprettet)
    }

    @Test
    fun `hente ut beregningsgrunnlag med svært høy yrkesskadeutbetaling fra API`() {
        val årsInntekter = setOf(
            InntektPerÅr(2022, Beløp(500000)),
            InntektPerÅr(2021, Beløp(400000)),
            InntektPerÅr(2020, Beløp(300000))
        )

        val beregning = Beregning(
            nedsettelsesDato = LocalDate.of(2023, 1, 1),
            årsInntekter = årsInntekter,
            uføregrad = setOf(Uføre(LocalDate.of(2021, 1, 1), Prosent(30))),
            yrkesskadevurdering = Yrkesskadevurdering(
                begrunnelse = "en begrunnelse",
                andelAvNedsettelsen = Prosent(30),
                erÅrsakssammenheng = true,
                relevanteSaker = listOf(YrkesskadeSak("yrkesskadesaken", null)),
                vurdertAv = "saksbehandler",
                vurdertTidspunkt = LocalDateTime.now()
            ),
            ytterligereNedsettelsesDato = LocalDate.of(2023, 1, 1),
            yrkesskadeBeløpVurderinger = listOf(
                YrkesskadeBeløpVurdering(
                    antattÅrligInntekt = Beløp(9999999999),
                    referanse = "yrkesskadesaken",
                    begrunnelse = "asdf",
                    vurdertAv = "saksbehandler"
                )
            ),
            registrerteYrkesskader = Yrkesskader(
                listOf(
                    Yrkesskade(
                        ref = "yrkesskadesaken",
                        saksnummer = 123,
                        kildesystem = "INFOTRYGD",
                        skadedato = LocalDate.of(2021, 1, 1)
                    )
                )
            ),
            inntektsPerioder = inntektsPerioder(årsInntekter)
        ).beregnBeregningsgrunnlag()

        val behandlingOpprettet = LocalDate.of(2024, 1, 1)

        val res = beregningDTO(beregning, behandlingOpprettet)

        val grunnlagYrkesskadeUføre = requireNotNull(res.grunnlagYrkesskadeUføre)

        assertThat(grunnlagYrkesskadeUføre.yrkesskadeGrunnlag.inntekter).hasSize(3)
        assertThat(grunnlagYrkesskadeUføre.yrkesskadeGrunnlag.grunnlag).isEqualByComparingTo(
            BigDecimal(6)
        )
        assertThat(grunnlagYrkesskadeUføre.uføreGrunnlag.uføreInntekter.last().inntektsPerioder).hasSize(1).first()
            .extracting { it.periode }
            .isEqualTo(Periode(YearMonth.of(2022, 1).atDay(1), YearMonth.of(2022, 12).atEndOfMonth()))
    }

    @Test
    fun `inntektIKroner i periodene skal være korrekt ved konstant uføregrad hele året`() {
        // Konstant uføregrad lagrer årsbeløp direkte i perioden (ikke månedlig)
        // Å multiplisere årsbeløpet med antall måneder gir feil svar (12x for fullt år)
        val årsInntekter = setOf(
            InntektPerÅr(2021, Beløp(600000)),
            InntektPerÅr(2022, Beløp(600000)),
            InntektPerÅr(2023, Beløp(600000))
        )

        val beregning = Beregning(
            nedsettelsesDato = LocalDate.of(2024, 1, 1),
            ytterligereNedsettelsesDato = LocalDate.of(2024, 1, 1),
            årsInntekter = årsInntekter,
            uføregrad = setOf(Uføre(LocalDate.of(2021, 1, 1), Prosent(30))),
            yrkesskadevurdering = null,
            registrerteYrkesskader = null,
            yrkesskadeBeløpVurderinger = null,
            inntektsPerioder = inntektsPerioder(årsInntekter)
        ).beregnBeregningsgrunnlag()

        val res = beregningDTO(beregning, LocalDate.of(2024, 1, 1))

        val inntektsPerioder2022 = requireNotNull(res.grunnlagUføre)
            .uføreInntekter
            .single { it.år == "2022" }
            .inntektsPerioder

        assertThat(inntektsPerioder2022).hasSize(1)
        // Forventet inntektIKroner = 600 000 (årsbeløpet), ikke 600 000 * 12
        assertThat(inntektsPerioder2022.first().inntektIKroner.verdi)
            .isEqualByComparingTo(BigDecimal(600000))
    }

    @Test
    fun `inntektIKroner i periodene skal multipliseres med korrekt antall måneder`() {
        // Uføregrad endrer seg midt i 2022: 0% jan-jun, 50% jul-des
        val årsInntekter = setOf(
            InntektPerÅr(2021, Beløp(600000)),
            InntektPerÅr(2022, Beløp(600000)),
            InntektPerÅr(2023, Beløp(600000))
        )

        val beregning = Beregning(
            nedsettelsesDato = LocalDate.of(2024, 1, 1),
            ytterligereNedsettelsesDato = LocalDate.of(2024, 1, 1),
            årsInntekter = årsInntekter,
            uføregrad = setOf(
                Uføre(LocalDate.of(2021, 1, 1), Prosent(0)),
                Uføre(LocalDate.of(2022, 7, 1), Prosent(50))
            ),
            yrkesskadevurdering = null,
            registrerteYrkesskader = null,
            yrkesskadeBeløpVurderinger = null,
            inntektsPerioder = inntektsPerioder(årsInntekter)
        ).beregnBeregningsgrunnlag()

        val res = beregningDTO(beregning, LocalDate.of(2024, 1, 1))

        val inntektsPerioder2022 = requireNotNull(res.grunnlagUføre)
            .uføreInntekter
            .single { it.år == "2022" }
            .inntektsPerioder

        assertThat(inntektsPerioder2022).hasSize(2)

        // Månedsinntekt for 2022 = 600 000 / 12 = 50 000; begge halvår har 6 måneder → 300 000
        val forventetHalvår = BigDecimal(50000).multiply(BigDecimal(6))

        inntektsPerioder2022.forEach { periode ->
            assertThat(periode.inntektIKroner.verdi)
                .`as`("inntektIKroner for periode ${periode.periode}")
                .isEqualByComparingTo(forventetHalvår)
        }
    }

    private fun inntektsPerioder(inntektPerÅr: Set<InntektPerÅr>): Set<Månedsinntekt> {
        return inntektPerÅr.flatMap { inntektPerÅr ->
            val år = inntektPerÅr.år
            (1..12).map { mnd ->
                Månedsinntekt(
                    YearMonth.of(år.value, mnd),
                    Beløp(inntektPerÅr.beløp.verdi.divide(12.toBigDecimal(), MathContext(10, RoundingMode.HALF_UP))),
                )
            }
        }.toSet()
    }
}
