package no.nav.aap.behandlingsflyt.behandling.beregning.år

import no.nav.aap.behandlingsflyt.behandling.beregning.InntektsPeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.år.Inntektsbehov
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.år.Input
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.InntektPerÅr
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre.Uføre
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade.Yrkesskade
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade.Yrkesskader
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningYrkeskaderBeløpVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningstidspunktVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.YrkesskadeBeløpVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.StudentGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.StudentVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.YrkesskadeSak
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.Yrkesskadevurdering
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.komponenter.verdityper.Prosent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.Year

class InntektsbehovTest {
    @Test
    fun `henter ut relevante år, nemlig tre år før nedsettelsen`() {
        val nedsettelsesDato = LocalDate.now().withYear(2005)
        val forOrdinær = Inntektsbehov(
            Input(
                nedsettelsesDato,
                årsInntekter = setOf(
                    InntektPerÅr(nedsettelsesDato.plusYears(1).year, Beløp(123)),
                    InntektPerÅr(nedsettelsesDato.minusYears(0).year, Beløp(125)),
                    InntektPerÅr(nedsettelsesDato.minusYears(1).year, Beløp(126)),
                    InntektPerÅr(nedsettelsesDato.minusYears(2).year, Beløp(127)),
                    InntektPerÅr(nedsettelsesDato.minusYears(3).year, Beløp(128)),
                    InntektPerÅr(nedsettelsesDato.minusYears(4).year, Beløp(129))
                ),
                uføregrad = setOf(Uføre(LocalDate.now(), Prosent.`0_PROSENT`)),
                yrkesskadevurdering = null,
                beregningGrunnlag = null,
                registrerteYrkesskader = null,
                inntektsPerioder = listOf(
                    InntektsPeriode(
                        periode = Periode(LocalDate.of(2022, 1, 1), LocalDate.of(2022, 12, 31)),
                        beløp = 500000.toDouble(),
                        inntektType = "lønn"
                    ),
                    InntektsPeriode(
                        periode = Periode(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 12, 31)),
                        beløp = 400000.toDouble(),
                        inntektType = "lønn"
                    ),
                    InntektsPeriode(
                        periode = Periode(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 12, 31)),
                        beløp = 300000.toDouble(),
                        inntektType = "lønn"
                    ),

                    ),
            )
        ).utledForOrdinær()

        assertThat(forOrdinær).containsExactlyInAnyOrder(
            InntektPerÅr(2004, Beløp(126)),
            InntektPerÅr(2003, Beløp(127)),
            InntektPerÅr(2002, Beløp(128))
        )
    }

    @Test
    fun `skal utlede de tre forutgående kalenderårene fra nedsettelsesdato`() {
        val nedsettelsesDato = LocalDate.now().minusYears(3)
        val relevanteÅr = Inntektsbehov(
            Input(
                nedsettelsesDato,
                årsInntekter = emptySet(),
                uføregrad = setOf(Uføre(LocalDate.now(), Prosent.`0_PROSENT`)),
                yrkesskadevurdering = null,
                beregningGrunnlag = null,
                registrerteYrkesskader = null,
                inntektsPerioder = listOf(
                    InntektsPeriode(
                        periode = Periode(LocalDate.of(2022, 1, 1), LocalDate.of(2022, 12, 31)),
                        beløp = 500000.toDouble(),
                        inntektType = "lønn"
                    ),
                    InntektsPeriode(
                        periode = Periode(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 12, 31)),
                        beløp = 400000.toDouble(),
                        inntektType = "lønn"
                    ),
                    InntektsPeriode(
                        periode = Periode(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 12, 31)),
                        beløp = 300000.toDouble(),
                        inntektType = "lønn"
                    ),

                    )
            )
        ).utledAlleRelevanteÅr()

        val nedsattYear = Year.of(nedsettelsesDato.year)

        assertThat(relevanteÅr).hasSize(3)
        assertThat(relevanteÅr).containsExactlyInAnyOrder(
            nedsattYear.minusYears(3),
            nedsattYear.minusYears(2),
            nedsattYear.minusYears(1)
        )
    }

    @Test
    fun `skal utlede de tre forutgående kalenderårene fra nedsettelsesdato og tre forutgående kalenderårene fra ytterligere nedsattdato`() {
        val nedsettelsesDato = LocalDate.now().minusYears(6)
        val ytterligereNedsattDato = LocalDate.now().minusYears(2)
        val relevanteÅr = Inntektsbehov(
            Input(
                nedsettelsesDato = nedsettelsesDato,
                årsInntekter = emptySet(),
                uføregrad = setOf(Uføre(LocalDate.now(), Prosent.`0_PROSENT`)),
                inntektsPerioder = listOf(
                    InntektsPeriode(
                        periode = Periode(LocalDate.of(2022, 1, 1), LocalDate.of(2022, 12, 31)),
                        beløp = 500000.toDouble(),
                        inntektType = "lønn"
                    ),
                    InntektsPeriode(
                        periode = Periode(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 12, 31)),
                        beløp = 400000.toDouble(),
                        inntektType = "lønn"
                    ),
                    InntektsPeriode(
                        periode = Periode(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 12, 31)),
                        beløp = 300000.toDouble(),
                        inntektType = "lønn"
                    ),

                    ),
                beregningGrunnlag = BeregningGrunnlag(
                    tidspunktVurdering = BeregningstidspunktVurdering(
                        begrunnelse = "asdf",
                        ytterligereNedsattArbeidsevneDato = ytterligereNedsattDato,
                        nedsattArbeidsevneDato = nedsettelsesDato,
                        ytterligereNedsattBegrunnelse = "asdf",
                        vurdertAv = "saksbehandler"
                    ), yrkesskadeBeløpVurdering = null
                ),
                yrkesskadevurdering = null,
                registrerteYrkesskader = null,
            )
        ).utledAlleRelevanteÅr()

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
            Input(
                nedsettelsesDato,
                årsInntekter = emptySet(),
                uføregrad = setOf(Uføre(LocalDate.now(), Prosent.`30_PROSENT`)),
                yrkesskadevurdering = null,
                registrerteYrkesskader = null,
                beregningGrunnlag = BeregningGrunnlag(
                    tidspunktVurdering = BeregningstidspunktVurdering(
                        begrunnelse = "begrunnelse",
                        nedsattArbeidsevneDato = nedsettelsesDato,
                        ytterligereNedsattArbeidsevneDato = LocalDate.now().minusYears(10),
                        ytterligereNedsattBegrunnelse = "begrunnelse",
                        vurdertAv = "saksbehandler"
                    ), yrkesskadeBeløpVurdering = null
                ),
                inntektsPerioder = listOf(
                    InntektsPeriode(
                        periode = Periode(LocalDate.of(2022, 1, 1), LocalDate.of(2022, 12, 31)),
                        beløp = 500000.toDouble(),
                        inntektType = "lønn"
                    ),
                    InntektsPeriode(
                        periode = Periode(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 12, 31)),
                        beløp = 400000.toDouble(),
                        inntektType = "lønn"
                    ),
                    InntektsPeriode(
                        periode = Periode(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 12, 31)),
                        beløp = 300000.toDouble(),
                        inntektType = "lønn"
                    ),

                    ),
            )
        )

        assertThat(inntektsbehov.finnesUføreData()).isTrue()
    }

    @Test
    fun `bruker manuell dato for yrkesskade om den er null fra register`() {
        val nedsettelsesDato = LocalDate.now().minusYears(3)
        val inntektsbehov = Inntektsbehov(
            Input(
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
                            ref = "123",
                            saksnummer = 0,
                            kildesystem = "KLVN",
                            skadedato = null
                        )
                    )
                ),
                beregningGrunnlag = BeregningGrunnlag(
                    tidspunktVurdering = BeregningstidspunktVurdering(
                        begrunnelse = "begrunnelse",
                        nedsattArbeidsevneDato = nedsettelsesDato,
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
                inntektsPerioder = listOf(
                    InntektsPeriode(
                        periode = Periode(LocalDate.of(2022, 1, 1), LocalDate.of(2022, 12, 31)),
                        beløp = 500000.toDouble(),
                        inntektType = "lønn"
                    ),
                    InntektsPeriode(
                        periode = Periode(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 12, 31)),
                        beløp = 400000.toDouble(),
                        inntektType = "lønn"
                    ),
                    InntektsPeriode(
                        periode = Periode(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 12, 31)),
                        beløp = 300000.toDouble(),
                        inntektType = "lønn"
                    ),

                    ),
            )
        )

        assertThat(inntektsbehov.skadetidspunkt()).isEqualTo(LocalDate.of(2023, 1, 1))
    }

    @Test
    fun `skal utlede de tre forutgående kalenderårene basert på datoene i beregningsgrunnlaget`() {
        val beregningGrunnlag = BeregningGrunnlag(
            tidspunktVurdering = BeregningstidspunktVurdering(
                begrunnelse = "begrunnelse",
                nedsattArbeidsevneDato = 1 januar 2025,
                ytterligereNedsattBegrunnelse = null,
                ytterligereNedsattArbeidsevneDato = LocalDate.now(),
                vurdertAv = "saksbehandler",
            ),
            yrkesskadeBeløpVurdering = null
        )

        val relevanteÅr = Inntektsbehov.utledAlleRelevanteÅr(beregningGrunnlag, null)
        assertThat(relevanteÅr).containsExactlyInAnyOrder(
            Year.of(2024),
            Year.of(2023),
            Year.of(2022)
        )
    }

    @Test
    fun `skal utlede de tre forutgående kalenderårene basert på datoene i studentgrunnlaget`() {
        val studentGrunnlag = StudentGrunnlag(
            studentvurdering = StudentVurdering(
                begrunnelse = "begrunnelse",
                vurdertAv = "saksbehandler",
                harAvbruttStudie = true,
                godkjentStudieAvLånekassen = true,
                avbruttPgaSykdomEllerSkade = true,
                harBehovForBehandling = true,
                avbruttStudieDato = 1 januar 2025,
                avbruddMerEnn6Måneder = true,
            ),
            oppgittStudent = null
        )

        val relevanteÅr = Inntektsbehov.utledAlleRelevanteÅr(null, studentGrunnlag)
        assertThat(relevanteÅr).containsExactlyInAnyOrder(
            Year.of(2024),
            Year.of(2023),
            Year.of(2022)
        )
    }

    @Test
    fun `skal utlede de tre forutgående kalenderårene for både nedsettelsesdato og ytterligereNedsattArbeidsevneDato`() {
        val nedsettelsesDato =  1 januar 2025
        val ytterligereNedsattArbeidsevneDato =  1 januar 2020

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
    fun `skal returnere den tidligste datoen fra beregningVurdering og studentVurdering`() {
        val studentvurdering = StudentVurdering(
            begrunnelse = "begrunnelse",
            vurdertAv = "saksbehandler",
            harAvbruttStudie = true,
            godkjentStudieAvLånekassen = true,
            avbruttPgaSykdomEllerSkade = true,
            harBehovForBehandling = true,
            avbruttStudieDato = 1 januar 2025,
            avbruddMerEnn6Måneder = true,
        )

        val beregningVurdering = BeregningstidspunktVurdering(
            begrunnelse = "begrunnelse",
            nedsattArbeidsevneDato = 1 januar 2024,
            ytterligereNedsattBegrunnelse = null,
            ytterligereNedsattArbeidsevneDato = LocalDate.now(),
            vurdertAv = "saksbehandler",
        )

        val nedsettelsesdato = Inntektsbehov.utledNedsettelsesdato(beregningVurdering, studentvurdering)
        assertThat(nedsettelsesdato).isEqualTo(1 januar 2024)
    }
}
