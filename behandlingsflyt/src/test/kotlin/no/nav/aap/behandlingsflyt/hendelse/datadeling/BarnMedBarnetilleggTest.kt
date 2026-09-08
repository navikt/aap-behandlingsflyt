package no.nav.aap.behandlingsflyt.hendelse.datadeling

import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.GraderingGrunnlag
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.Minstesats
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.Tilkjent
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.TilkjentYtelsePeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.barnetillegg.BarnetilleggGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.barnetillegg.BarnetilleggPeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Fødselsdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn.BarnIdentifikator
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.behandlingsflyt.test.februar
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.komponenter.verdityper.GUnit
import no.nav.aap.komponenter.verdityper.Prosent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class BarnMedBarnetilleggTest {

    private val barnMedIdent = BarnIdentifikator.BarnIdent(Ident("12345678901"))

    private val barnUtenIdent = BarnIdentifikator.NavnOgFødselsdato("Ole", Fødselsdato(1 januar 2010))


    @Test
    fun `barn med ident får riktig gradert beløp for hele perioden når periodene er like`() {
        val periode = Periode(1 januar 2024, 31 januar 2024)
        val tilkjentYtelse = listOf(
            TilkjentYtelsePeriode(periode, tilkjent(gradering = Prosent.`50_PROSENT`, barnetilleggsats = Beløp(38)))
        )
        val grunnlag = BarnetilleggGrunnlag(listOf(BarnetilleggPeriode(periode, setOf(barnMedIdent))))

        val resultat = utledBarnMedBarnetillegg(tilkjentYtelse, grunnlag)

        assertThat(resultat)
            .usingRecursiveComparison()
            .isEqualTo(
                listOf(
                    BarnMedBarnetillegg(
                        ident = "12345678901",
                        perioderMedBarnetillegg = listOf(
                            PeriodeMedBeløp(periode, Beløp(38).multiplisert(Prosent.`50_PROSENT`))
                        )
                    )
                )
            )
    }

    @Test
    fun `barnetilleggperiode som spenner over flere tilkjentperioder med ulik gradering splittes riktig`() {
        val heleRettighetsperioden = Periode(1 januar 2024, 29 februar 2024)
        val januarPeriode = Periode(1 januar 2024, 31 januar 2024)
        val februarPeriode = Periode(1 februar 2024, 29 februar 2024)

        val tilkjentYtelse = listOf(
            TilkjentYtelsePeriode(januarPeriode, tilkjent(gradering = Prosent.`100_PROSENT`)),
            TilkjentYtelsePeriode(februarPeriode, tilkjent(gradering = Prosent.`50_PROSENT`)),
        )
        val grunnlag = BarnetilleggGrunnlag(
            listOf(BarnetilleggPeriode(heleRettighetsperioden, setOf(barnMedIdent)))
        )

        val resultat = utledBarnMedBarnetillegg(tilkjentYtelse, grunnlag)

        assertThat(resultat)
            .usingRecursiveComparison()
            .ignoringCollectionOrder()
            .isEqualTo(
                listOf(
                    BarnMedBarnetillegg(
                        ident = "12345678901",
                        perioderMedBarnetillegg = listOf(
                            PeriodeMedBeløp(januarPeriode, Beløp(38).multiplisert(Prosent.`100_PROSENT`)),
                            PeriodeMedBeløp(februarPeriode, Beløp(38).multiplisert(Prosent.`50_PROSENT`)),
                        )
                    )
                )
            )
    }

    @Test
    fun `tilkjentperiode som spenner over flere barnetilleggperioder splittes riktig når barn kommer til`() {
        val heleTilkjentPerioden = Periode(1 januar 2024, 29 februar 2024)
        val januarPeriode = Periode(1 januar 2024, 31 januar 2024)
        val februarPeriode = Periode(1 februar 2024, 29 februar 2024)

        val tilkjentYtelse = listOf(
            TilkjentYtelsePeriode(heleTilkjentPerioden, tilkjent())
        )
        val grunnlag = BarnetilleggGrunnlag(
            listOf(
                BarnetilleggPeriode(januarPeriode, setOf(barnMedIdent)),
                BarnetilleggPeriode(februarPeriode, setOf(barnMedIdent, barnUtenIdent)),
            )
        )

        val resultat = utledBarnMedBarnetillegg(tilkjentYtelse, grunnlag)

        val medIdent = resultat.single { it.ident == "12345678901" }
        assertThat(medIdent.perioderMedBarnetillegg)
            .usingRecursiveComparison()
            .ignoringCollectionOrder()
            .isEqualTo(
                listOf(
                    PeriodeMedBeløp(januarPeriode, Beløp(38).multiplisert(Prosent.`100_PROSENT`)),
                    PeriodeMedBeløp(februarPeriode, Beløp(38).multiplisert(Prosent.`100_PROSENT`)),
                )
            )

        val utenIdent = resultat.single { it.ident == null }
        assertThat(utenIdent.perioderMedBarnetillegg)
            .usingRecursiveComparison()
            .isEqualTo(listOf(PeriodeMedBeløp(februarPeriode, Beløp(38).multiplisert(Prosent.`100_PROSENT`))))
    }

    @Test
    fun `periode uten utbetaling fra tilkjent ytelse gir ikke barnetilleggperiode`() {
        val januarPeriode = Periode(1 januar 2024, 31 januar 2024)
        val februarPeriode = Periode(1 februar 2024, 29 februar 2024)
        val heleRettighetsperioden = Periode(1 januar 2024, 29 februar 2024)

        val tilkjentYtelse = listOf(
            TilkjentYtelsePeriode(januarPeriode, tilkjent(redusertDagsats = Beløp(0))),
            TilkjentYtelsePeriode(februarPeriode, tilkjent(redusertDagsats = Beløp(100))),
        )
        val grunnlag = BarnetilleggGrunnlag(listOf(BarnetilleggPeriode(heleRettighetsperioden, setOf(barnMedIdent))))

        val resultat = utledBarnMedBarnetillegg(tilkjentYtelse, grunnlag)

        assertThat(resultat)
            .usingRecursiveComparison()
            .isEqualTo(
                listOf(
                    BarnMedBarnetillegg(
                        ident = "12345678901",
                        perioderMedBarnetillegg = listOf(
                            PeriodeMedBeløp(februarPeriode, Beløp(38).multiplisert(Prosent.`100_PROSENT`))
                        )
                    )
                )
            )
    }

    private fun tilkjent(
        gradering: Prosent = Prosent.`100_PROSENT`,
        barnetilleggsats: Beløp = Beløp(38),
        redusertDagsats: Beløp = Beløp(100),
    ) = Tilkjent(
        dagsats = Beløp(100),
        gradering = gradering,
        graderingGrunnlag = GraderingGrunnlag(
            samordningGradering = Prosent.`0_PROSENT`,
            institusjonGradering = Prosent.`0_PROSENT`,
            arbeidGradering = Prosent.`0_PROSENT`,
            samordningUføregradering = Prosent.`0_PROSENT`,
            samordningArbeidsgiverGradering = Prosent.`0_PROSENT`,
            meldepliktGradering = Prosent.`0_PROSENT`,
        ),
        grunnlagsfaktor = GUnit(1),
        grunnbeløp = Beløp(100),
        barnepensjonDagsats = Beløp(0),
        antallBarn = 1,
        barnetilleggsats = barnetilleggsats,
        barnetillegg = barnetilleggsats,
        utbetalingsdato = 1 januar 2024,
        minsteSats = Minstesats.IKKE_MINSTESATS,
        redusertDagsats = redusertDagsats
    )
}

