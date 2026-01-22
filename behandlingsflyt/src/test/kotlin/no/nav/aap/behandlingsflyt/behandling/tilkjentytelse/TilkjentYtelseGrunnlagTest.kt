package no.nav.aap.behandlingsflyt.behandling.tilkjentytelse

import no.nav.aap.behandlingsflyt.behandling.underveis.UnderveisService
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.FastsettGrenseverdiArbeidRegel
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.GraderingArbeidRegel
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.Hverdager.Companion.antallHverdager
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.Kvote
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.MeldepliktVurdering
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.Oppfylt
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.Vurdering
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.tomUnderveisInput
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.barnetillegg.BarnetilleggGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.SamordningGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.arbeidsgiver.SamordningArbeidsgiverGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.arbeidsgiver.SamordningArbeidsgiverVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.uførevurdering.SamordningUføreGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.uførevurdering.SamordningUføreVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.ArbeidIPeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.Meldekort
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Fødselsdato
import no.nav.aap.behandlingsflyt.help.assertTidslinje
import no.nav.aap.behandlingsflyt.test.august
import no.nav.aap.behandlingsflyt.test.desember
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.tidslinjeOf
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.komponenter.verdityper.GUnit
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.komponenter.verdityper.Tid
import no.nav.aap.komponenter.verdityper.TimerArbeid
import no.nav.aap.verdityper.dokument.JournalpostId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal

/**
 * Eksemplene er hentet fra https://navno.sharepoint.com/:w:/r/sites/POAAP/Shared%20Documents/Regelverk/Regelverksavklaringer/Juridiske%20avklaringer/Beregningscaser.docx?web=1
 */
class TilkjentYtelseGrunnlagTest {
    val `1000 kroner i G` = GUnit(BigDecimal("1.99755"))
    val `1100 kroner i G` = GUnit(BigDecimal("2.1973"))

    val førsteUke = Periode(1 desember 2025, 7 desember 2025)
    val andreUke = Periode(8 desember 2025, 14 desember 2025)
    val meldeperioden = Periode(1 desember 2025, 14 desember 2025)

    val minsteÅrligeYtelse = tidslinjeOf(førsteUke to `1000 kroner i G`, andreUke to `1100 kroner i G`)

    @Test
    fun `eksempel 1, endring i dagsats og tap av rett til AAP`() {
        val førsteTilkjentYtelse = tilkjentYtelse(
            minsteÅrligeYtelse = minsteÅrligeYtelse
        )
        assertThat(sumRettTilAAP(førsteTilkjentYtelse)).isEqualTo(BigDecimal("10500.00"))

        val andreTilkjentYtelse = tilkjentYtelse(
            rettTilYtelse = tidslinjeOf(
                Periode(1 desember 2025, 2 desember 2025) to false,
                Periode(3 desember 2025, 14 desember 2025) to true
            ),
            minsteÅrligeYtelse = minsteÅrligeYtelse
        )
        assertThat(sumRettTilAAP(andreTilkjentYtelse)).isEqualTo(BigDecimal("8500.00"))

        assertTidslinje(
            førsteTilkjentYtelse,
            førsteUke to { it: Tilkjent ->
                assertThat(it.redusertDagsats()).isEqualTo(Beløp(1000))
            },
            andreUke to { it: Tilkjent ->
                assertThat(it.redusertDagsats()).isEqualTo(Beløp(1100))
            }
        )

        assertTidslinje(
            andreTilkjentYtelse,
            Periode(1 desember 2025, 2 desember 2025) to { it: Tilkjent ->
                assertThat(it.redusertDagsats()).isEqualTo(Beløp(0))
            },
            Periode(3 desember 2025, 7 desember 2025) to { it: Tilkjent ->
                assertThat(it.redusertDagsats()).isEqualTo(Beløp(1000))
            },
            andreUke to { it: Tilkjent ->
                assertThat(it.redusertDagsats()).isEqualTo(Beløp(1100))
            }
        )
        assertThat(sumFeilutbetaling(førsteTilkjentYtelse, andreTilkjentYtelse))
            .isEqualTo(BigDecimal("2000.00"))
    }

    @Test
    fun `eksempel 2, endring i dagsats og tap av rett til AAP`() {
        val førsteTilkjentYtelse = tilkjentYtelse(
            minsteÅrligeYtelse = minsteÅrligeYtelse
        )
        assertThat(sumRettTilAAP(førsteTilkjentYtelse))
            .isEqualTo(BigDecimal("10500.00"))

        val andreTilkjentYtelse = tilkjentYtelse(
            rettTilYtelse = tidslinjeOf(
                førsteUke to true,
                andreUke to false,
            ),
            minsteÅrligeYtelse = minsteÅrligeYtelse
        )
        assertThat(sumRettTilAAP(andreTilkjentYtelse)).isEqualTo(BigDecimal("5000.00"))

        assertTidslinje(
            førsteTilkjentYtelse,
            førsteUke to { it: Tilkjent ->
                assertThat(it.redusertDagsats()).isEqualTo(Beløp(1000))
            },
            andreUke to { it: Tilkjent ->
                assertThat(it.redusertDagsats()).isEqualTo(Beløp(1100))
            }
        )

        assertTidslinje(
            andreTilkjentYtelse,
            førsteUke to { it: Tilkjent ->
                assertThat(it.redusertDagsats()).isEqualTo(Beløp(1000))
            },
            andreUke to { it: Tilkjent ->
                assertThat(it.redusertDagsats()).isEqualTo(Beløp(0))
            }
        )
        assertThat(sumFeilutbetaling(førsteTilkjentYtelse, andreTilkjentYtelse))
            .isEqualTo(BigDecimal("5500.00"))
    }

    @Test
    fun `eksempel 3, arbeidet andre uke`() {
        val førsteTilkjentYtelse = tilkjentYtelse(
            minsteÅrligeYtelse = minsteÅrligeYtelse,
            timerArbeidet = List(7) { 0.0 } + List(5) { 7.5 } + List(2) { 0.0 },
        )

        assertTidslinje(
            førsteTilkjentYtelse,
            Periode(1 desember 2025, 14 desember 2025) to {
                assertThat(it.gradering).isEqualTo(Prosent(50))
            })

        assertTidslinje(
            førsteTilkjentYtelse,
            førsteUke to { it: Tilkjent ->
                assertThat(it.redusertDagsats()).isEqualTo(Beløp(500))
            },
            andreUke to { it: Tilkjent ->
                assertThat(it.redusertDagsats()).isEqualTo(Beløp(550))
            }
        )

        assertThat(sumRettTilAAP(førsteTilkjentYtelse))
            .isEqualTo(BigDecimal("5250.00"))
    }

    @Test
    fun `eksempel 4a`() {
        val timerArbeidet = listOf(
            0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
            0.0, 0.0, 7.5, 7.5, 7.5, 0.0, 0.0,
        )

        val førsteTilkjentYtelse = tilkjentYtelse(
            minsteÅrligeYtelse = minsteÅrligeYtelse,
            timerArbeidet = timerArbeidet,
        )

        val andreTilkjentYtelse = tilkjentYtelse(
            rettTilYtelse = tidslinjeOf(
                Periode(1 desember 2025, 9 desember 2025) to true,
                Periode(10 desember 2025, 14 desember 2025) to false,
            ),
            minsteÅrligeYtelse = minsteÅrligeYtelse,
            timerArbeidet = timerArbeidet,
        )

        assertTidslinje(
            førsteTilkjentYtelse,
            førsteUke to {
                assertThat(it.gradering).isEqualTo(Prosent(70))
                assertThat(it.redusertDagsats()).isEqualTo(Beløp(700))
            },
            andreUke to {
                assertThat(it.gradering).isEqualTo(Prosent(70))
                assertThat(it.redusertDagsats()).isEqualTo(Beløp(770))
            },
        )

        assertTidslinje(
            andreTilkjentYtelse,
            førsteUke to {
                assertThat(it.gradering).isEqualTo(Prosent(100))
                assertThat(it.redusertDagsats()).isEqualTo(Beløp(1000))
            },
            Periode(8 desember 2025, 9 desember 2025) to {
                assertThat(it.gradering).isEqualTo(Prosent(100))
                assertThat(it.redusertDagsats()).isEqualTo(Beløp(1100))
            },
            Periode(10 desember 2025, 14 desember 2025) to {
                assertThat(it.gradering).isEqualTo(Prosent(0))
                assertThat(it.redusertDagsats()).isEqualTo(Beløp(0))
            },
        )

        assertThat(sumRettTilAAP(førsteTilkjentYtelse)).isEqualTo(BigDecimal("7350.00"))
        assertThat(sumRettTilAAP(andreTilkjentYtelse)).isEqualTo(BigDecimal("7200.00"))
        assertThat(sumFeilutbetaling(førsteTilkjentYtelse, andreTilkjentYtelse)).isEqualTo(BigDecimal("150.00"))
    }

    @Test
    fun `eksempel 4b`() {
        val timerArbeidet = listOf(
            0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
            0.0, 0.0, 7.5, 7.5, 7.5, 0.0, 0.0,
        )

        val førsteTilkjentYtelse = tilkjentYtelse(
            minsteÅrligeYtelse = tidslinjeOf(meldeperioden to `1000 kroner i G`),
            timerArbeidet = timerArbeidet,
        )

        val andreTilkjentYtelse = tilkjentYtelse(
            rettTilYtelse = tidslinjeOf(
                Periode(1 desember 2025, 9 desember 2025) to true,
                Periode(10 desember 2025, 14 desember 2025) to false,
            ),
            minsteÅrligeYtelse = tidslinjeOf(meldeperioden to `1000 kroner i G`),
            timerArbeidet = timerArbeidet,
        )

        assertTidslinje(
            førsteTilkjentYtelse,
            meldeperioden to {
                assertThat(it.gradering).isEqualTo(Prosent(70))
                assertThat(it.redusertDagsats()).isEqualTo(Beløp(700))
            },
        )

        assertTidslinje(
            andreTilkjentYtelse,
            førsteUke to {
                assertThat(it.gradering).isEqualTo(Prosent(100))
                assertThat(it.redusertDagsats()).isEqualTo(Beløp(1000))
            },
            Periode(8 desember 2025, 9 desember 2025) to {
                assertThat(it.gradering).isEqualTo(Prosent(100))
                assertThat(it.redusertDagsats()).isEqualTo(Beløp(1000))
            },
            Periode(10 desember 2025, 14 desember 2025) to {
                assertThat(it.gradering).isEqualTo(Prosent(0))
                assertThat(it.redusertDagsats()).isEqualTo(Beløp(0))
            },
        )

        assertThat(sumRettTilAAP(førsteTilkjentYtelse)).isEqualTo(BigDecimal("7000.00"))
        assertThat(sumRettTilAAP(andreTilkjentYtelse)).isEqualTo(BigDecimal("7000.00"))
        assertThat(sumFeilutbetaling(førsteTilkjentYtelse, andreTilkjentYtelse)).isEqualTo(BigDecimal("0.00"))
    }

    @Test
    fun `eksempel 4c`() {
        val timerArbeidet = listOf(
            0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
            0.0, 0.0, 0.0, 0.0, 0.0, 10.0, 10.0,
        )

        val førsteTilkjentYtelse = tilkjentYtelse(
            minsteÅrligeYtelse = minsteÅrligeYtelse,
            timerArbeidet = timerArbeidet,
        )

        val andreTilkjentYtelse = tilkjentYtelse(
            rettTilYtelse = tidslinjeOf(
                Periode(1 desember 2025, 9 desember 2025) to true,
                Periode(10 desember 2025, 14 desember 2025) to false,
            ),
            minsteÅrligeYtelse = minsteÅrligeYtelse,
            timerArbeidet = timerArbeidet,
        )

        assertTidslinje(
            førsteTilkjentYtelse,
            førsteUke to {
                assertThat(it.gradering).isEqualTo(Prosent(73))
                assertThat(it.redusertDagsats()).isEqualTo(Beløp(730))
            },
            andreUke to {
                assertThat(it.gradering).isEqualTo(Prosent(73))
                assertThat(it.redusertDagsats()).isEqualTo(Beløp(803))
            },
        )

        assertTidslinje(
            andreTilkjentYtelse,
            førsteUke to {
                assertThat(it.gradering).isEqualTo(Prosent(100))
                assertThat(it.redusertDagsats()).isEqualTo(Beløp(1000))
            },
            Periode(8 desember 2025, 9 desember 2025) to {
                assertThat(it.gradering).isEqualTo(Prosent(100))
                assertThat(it.redusertDagsats()).isEqualTo(Beløp(1100))
            },
            Periode(10 desember 2025, 14 desember 2025) to {
                assertThat(it.gradering).isEqualTo(Prosent(0))
                assertThat(it.redusertDagsats()).isEqualTo(Beløp(0))
            },
        )

        assertThat(sumRettTilAAP(førsteTilkjentYtelse)).isEqualTo(BigDecimal("7665.00"))
        assertThat(sumRettTilAAP(andreTilkjentYtelse)).isEqualTo(BigDecimal("7200.00"))
        assertThat(sumFeilutbetaling(førsteTilkjentYtelse, andreTilkjentYtelse)).isEqualTo(BigDecimal("465.00"))
    }

    val minstesatsEksempel5 = tidslinjeOf(Periode(Tid.MIN, Tid.MAKS) to GUnit("2.041"))
    val `fyller 25 den 13 desember` = Fødselsdato(13 desember 2000)
        .also {
            check(it.`25årsDagen`() == 13 desember 2025)
        }

    @Test
    fun `eksempel 5a`() {
        val førsteTilkjentYtelse = tilkjentYtelse(
            fødselsdato = `fyller 25 den 13 desember`,
            minsteÅrligeYtelse = minstesatsEksempel5,
        )

        assertTidslinje(
            førsteTilkjentYtelse,
            Periode(1 desember 2025, 12 desember 2025) to {
                assertThat(it.dagsats).isEqualTo(Beløp("681.17"))
                assertThat(it.gradering).isEqualTo(Prosent(100))
                assertThat(it.redusertDagsats()).isEqualTo(Beløp(681))
            },
            Periode(13 desember 2025, 14 desember 2025) to {
                assertThat(it.dagsats).isEqualTo(Beløp("1021.76"))
                assertThat(it.gradering).isEqualTo(Prosent(100))
                assertThat(it.redusertDagsats()).isEqualTo(Beløp(1022))
            },
        )

        assertThat(sumRettTilAAP(førsteTilkjentYtelse)).isEqualTo(BigDecimal("6810.00"))
    }

    @Test
    fun `eksempel 5b`() {
        val timerArbeidet = listOf(
            0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
            0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 7.5,
        )

        val førsteTilkjentYtelse = tilkjentYtelse(
            fødselsdato = `fyller 25 den 13 desember`,
            minsteÅrligeYtelse = minstesatsEksempel5,
            timerArbeidet = timerArbeidet,
        )

        assertTidslinje(
            førsteTilkjentYtelse,
            Periode(1 desember 2025, 12 desember 2025) to {
                assertThat(it.dagsats).isEqualTo(Beløp("681.17"))
                assertThat(it.gradering).isEqualTo(Prosent(90))
                assertThat(it.redusertDagsats()).isEqualTo(Beløp(613))
            },
            Periode(13 desember 2025, 14 desember 2025) to {
                assertThat(it.dagsats).isEqualTo(Beløp("1021.76"))
                assertThat(it.gradering).isEqualTo(Prosent(90))
                assertThat(it.redusertDagsats()).isEqualTo(Beløp(920))
            },
        )

        assertThat(sumRettTilAAP(førsteTilkjentYtelse)).isEqualTo(BigDecimal("6130.00"))
    }

    @Test
    fun `eksempel 5c`() {
        val timerArbeidet = listOf(
            7.5, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
            0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
        )

        val førsteTilkjentYtelse = tilkjentYtelse(
            fødselsdato = `fyller 25 den 13 desember`,
            minsteÅrligeYtelse = minstesatsEksempel5,
            timerArbeidet = timerArbeidet,
        )

        assertTidslinje(
            førsteTilkjentYtelse,
            Periode(1 desember 2025, 12 desember 2025) to {
                assertThat(it.dagsats).isEqualTo(Beløp("681.17"))
                assertThat(it.gradering).isEqualTo(Prosent(90))
                assertThat(it.redusertDagsats()).isEqualTo(Beløp(613))
            },
            Periode(13 desember 2025, 14 desember 2025) to {
                assertThat(it.dagsats).isEqualTo(Beløp("1021.76"))
                assertThat(it.gradering).isEqualTo(Prosent(90))
                assertThat(it.redusertDagsats()).isEqualTo(Beløp(920))
            },
        )

        assertThat(sumRettTilAAP(førsteTilkjentYtelse)).isEqualTo(BigDecimal("6130.00"))
    }

    @Test
    fun `eksempel 6a`() {
        val førsteTilkjentYtelse = tilkjentYtelse(
            beregningsgrunnlag = GUnit("5.3520147269"),
        )

        assertTidslinje(
            førsteTilkjentYtelse,
            meldeperioden to {
                assertThat(it.dagsats).isEqualTo(Beløp("1768.34"))
            }
        )

        assertThat(sumRettTilAAP(førsteTilkjentYtelse)).isEqualTo(BigDecimal("17680.00"))
    }

    @Test
    fun `eksempel 6b`() {
        val timerArbeidet = listOf(
            6.5, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
            0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
        )

        val førsteTilkjentYtelse = tilkjentYtelse(
            beregningsgrunnlag = GUnit("5.3520147269"),
            timerArbeidet = timerArbeidet,
        )

        assertTidslinje(
            førsteTilkjentYtelse,
            meldeperioden to {
                assertThat(it.dagsats).isEqualTo(Beløp("1768.34"))
                assertThat(it.redusertDagsats()).isEqualTo(Beløp("1609.00"))
            }
        )

        assertThat(sumRettTilAAP(førsteTilkjentYtelse)).isEqualTo(BigDecimal("16090.00"))
    }



    private fun sumRettTilAAP(tilkjentYtelse: Tidslinje<Tilkjent>): BigDecimal {
        return tilkjentYtelse.segmenter().sumOf {
            it.verdi.redusertDagsats().multiplisert(it.periode.antallHverdager().asInt).verdi
        }
    }


    private fun sumFeilutbetaling(
        førsteTilkjentYtelse: Tidslinje<Tilkjent>,
        andreTilkjentYtelse: Tidslinje<Tilkjent>
    ): BigDecimal {
        return BigDecimal(0).minus(
            førsteTilkjentYtelse.outerJoin(andreTilkjentYtelse) { første, andre ->
                val rettTilPenger = første?.redusertDagsats() ?: Beløp(0)
                val endretRettTilPenger = andre?.redusertDagsats() ?: Beløp(0)
                endretRettTilPenger.minus(rettTilPenger)
            }.segmenter()
                .sumOf { it.verdi.multiplisert(it.periode.antallHverdager().asInt).verdi }
        )
    }

    /* Funker kun for en meldeperiode, 1. til 14. desember 2025. */
    private fun tilkjentYtelse(
        minsteÅrligeYtelse: Tidslinje<GUnit> = MINSTE_ÅRLIG_YTELSE_TIDSLINJE,
        rettTilYtelse: Tidslinje<Boolean>? = null,
        timerArbeidet: List<Double> = List(14) { 0.0 },
        fødselsdato: Fødselsdato = Fødselsdato(14 august 1989),
        beregningsgrunnlag: GUnit = GUnit(0),
    ): Tidslinje<Tilkjent> {
        val periodeForVurdering = Periode(1 desember 2025, 14 desember 2025)
        val underveisInput = tomUnderveisInput(
            rettighetsperiode = periodeForVurdering,
            meldekort = listOf(
                Meldekort(
                    journalpostId = JournalpostId("0"),
                    timerArbeidPerPeriode = timerArbeidet.mapIndexed { i, timer ->
                        val dato = (1 + i) desember 2025
                        ArbeidIPeriode(
                            periode = Periode(dato, dato),
                            timerArbeid = TimerArbeid(BigDecimal(timer)),
                        )
                    }.toSet(),
                    mottattTidspunkt = (14 desember 2025).atTime(14, 0)
                )
            ),
            innsendingsTidspunkt = mapOf(15 desember 2025 to JournalpostId("0")),
        )


        val startvurderinger = (rettTilYtelse ?: tidslinjeOf(periodeForVurdering to true))
            .map { rettTilAAP ->
                Vurdering(
                    fårAapEtter = if (rettTilAAP) RettighetsType.BISTANDSBEHOV else null,
                    meldeperiode = periodeForVurdering,
                    meldepliktVurdering = MeldepliktVurdering.Fritak,
                    varighetVurdering = Oppfylt(setOf(Kvote.ORDINÆR))
                )
            }

        val underveisperioder = UnderveisService.tilUnderveisperioder(
            GraderingArbeidRegel().vurder(
                underveisInput,
                FastsettGrenseverdiArbeidRegel().vurder(underveisInput, startvurderinger)
            )
        )

        return BeregnTilkjentYtelseService(
            TilkjentYtelseGrunnlag(
                minsteÅrligeYtelse = minsteÅrligeYtelse,
                fødselsdato = fødselsdato,
                beregningsgrunnlag = beregningsgrunnlag,
                underveisgrunnlag = UnderveisGrunnlag(0L, underveisperioder),
                barnetilleggGrunnlag = BarnetilleggGrunnlag(listOf()),
                samordningGrunnlag = SamordningGrunnlag(setOf()),
                samordningUføre = SamordningUføreGrunnlag(
                    SamordningUføreVurdering(
                        begrunnelse = "",
                        vurderingPerioder = listOf(),
                        vurdertAv = "Z00000",
                    )
                ),
                samordningArbeidsgiver = SamordningArbeidsgiverGrunnlag(
                    vurdering = SamordningArbeidsgiverVurdering(
                        begrunnelse = "",
                        perioder = listOf(),
                        vurdertAv = "Z00000",
                    )
                ),
            ),
        ).beregnTilkjentYtelse()
    }
}