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
import no.nav.aap.komponenter.verdityper.TimerArbeid
import no.nav.aap.verdityper.dokument.JournalpostId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class TilkjentYtelseGrunnlagTest {
    val minsteÅrligeYtelse = tidslinjeOf(
        Periode(1 desember 2025, 7 desember 2025) to GUnit(BigDecimal("1.99755")), // 1000 kroner
        Periode(8 desember 2025, 14 desember 2025) to GUnit(BigDecimal("2.1973")), // 1100 kroner
    )

    @Test
    fun `eksempel 1, endring i dagsats og tap av rett til AAP`() {
        val førsteTilkjentYtelse = tilkjentYtelse(
            rettTilYtelse = tidslinjeOf(
                Periode(1 desember 2025, 14 desember 2025) to true
            ),
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
            Periode(1 desember 2025, 7 desember 2025) to { it: Tilkjent ->
                assertThat(it.redusertDagsats()).isEqualTo(Beløp(1000))
            },
            Periode(8 desember 2025, 14 desember 2025) to { it: Tilkjent ->
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
            Periode(8 desember 2025, 14 desember 2025) to { it: Tilkjent ->
                assertThat(it.redusertDagsats()).isEqualTo(Beløp(1100))
            }
        )
        assertThat(sumFeilutbetaling(førsteTilkjentYtelse, andreTilkjentYtelse))
            .isEqualTo(BigDecimal("2000.00"))
    }

    @Test
    fun `eksempel 2, endring i dagsats og tap av rett til AAP`() {
        val førsteTilkjentYtelse = tilkjentYtelse(
            rettTilYtelse = tidslinjeOf(
                Periode(1 desember 2025, 14 desember 2025) to true
            ),
            minsteÅrligeYtelse = minsteÅrligeYtelse
        )
        assertThat(sumRettTilAAP(førsteTilkjentYtelse))
            .isEqualTo(BigDecimal("10500.00"))

        val andreTilkjentYtelse = tilkjentYtelse(
            rettTilYtelse = tidslinjeOf(
                Periode(1 desember 2025, 7 desember 2025) to true,
                Periode(8 desember 2025, 14 desember 2025) to false,
            ),
            minsteÅrligeYtelse = minsteÅrligeYtelse
        )
        assertThat(sumRettTilAAP(andreTilkjentYtelse)).isEqualTo(BigDecimal("5000.00"))

        assertTidslinje(
            førsteTilkjentYtelse,
            Periode(1 desember 2025, 7 desember 2025) to { it: Tilkjent ->
                assertThat(it.redusertDagsats()).isEqualTo(Beløp(1000))
            },
            Periode(8 desember 2025, 14 desember 2025) to { it: Tilkjent ->
                assertThat(it.redusertDagsats()).isEqualTo(Beløp(1100))
            }
        )

        assertTidslinje(
            andreTilkjentYtelse,
            Periode(1 desember 2025, 7 desember 2025) to { it: Tilkjent ->
                assertThat(it.redusertDagsats()).isEqualTo(Beløp(1000))
            },
            Periode(8 desember 2025, 14 desember 2025) to { it: Tilkjent ->
                assertThat(it.redusertDagsats()).isEqualTo(Beløp(0))
            }
        )
        assertThat(sumFeilutbetaling(førsteTilkjentYtelse, andreTilkjentYtelse))
            .isEqualTo(BigDecimal("5500.00"))
    }

    @Test
    fun `eksempel 3, arbeidet andre uke`() {
        val førsteTilkjentYtelse = tilkjentYtelse(
            rettTilYtelse = tidslinjeOf(
                Periode(1 desember 2025, 14 desember 2025) to true
            ),
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
            Periode(1 desember 2025, 7 desember 2025) to { it: Tilkjent ->
                assertThat(it.redusertDagsats()).isEqualTo(Beløp(500))
            },
            Periode(8 desember 2025, 14 desember 2025) to { it: Tilkjent ->
                assertThat(it.redusertDagsats()).isEqualTo(Beløp(550))
            }
        )

        assertThat(sumRettTilAAP(førsteTilkjentYtelse))
            .isEqualTo(BigDecimal("5250.00"))
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
        minsteÅrligeYtelse: Tidslinje<GUnit>,
        rettTilYtelse: Tidslinje<Boolean>,
        timerArbeidet: List<Double> = List(14) { 0.0 },
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


        val startvurderinger = rettTilYtelse.map { rettTilAAP ->
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
                fødselsdato = Fødselsdato(14 august 1989),
                beregningsgrunnlag = GUnit(0),
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