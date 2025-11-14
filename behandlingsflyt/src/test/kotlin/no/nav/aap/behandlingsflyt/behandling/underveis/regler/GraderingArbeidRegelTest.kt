package no.nav.aap.behandlingsflyt.behandling.underveis.regler

import no.nav.aap.behandlingsflyt.behandling.underveis.regler.GraderingArbeidRegel.OpplysningerOmArbeid
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.ArbeidIPeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.Meldekort
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsevne.ArbeidsevneGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsevne.ArbeidsevneVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.Fritaksvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.MeldepliktGrunnlag
import no.nav.aap.behandlingsflyt.help.assertTidslinje
import no.nav.aap.behandlingsflyt.test.mai
import no.nav.aap.behandlingsflyt.test.mars
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.komponenter.verdityper.TimerArbeid
import no.nav.aap.verdityper.dokument.JournalpostId
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

class GraderingArbeidRegelTest {
    private val regel = GraderingArbeidRegel()

    @Test
    fun `Arbeidsevnevurdering fører ikke til vurdering utenfor rettighetsperioden`() {
        val fom = LocalDate.parse("2024-10-31")
        val rettighetsperiode = Periode(fom, LocalDate.parse("2025-10-31"))
        val input = tomUnderveisInput(
            rettighetsperiode = rettighetsperiode,
            arbeidsevneGrunnlag = ArbeidsevneGrunnlag(
                listOf(
                    ArbeidsevneVurdering(
                        begrunnelse = "",
                        arbeidsevne = Prosent.`50_PROSENT`,
                        fraDato = fom.minusDays(1), /* viktig at vi tester vurderinger fra før rettighetsperioden */
                        opprettetTid = LocalDateTime.now(),
                        "vurdertAv"
                    )
                )
            )
        )
        val vurdering = vurder(input)

        assertTrue(rettighetsperiode.inneholder(vurdering.helePerioden()))
    }

    @Test
    fun `skal anta timer hvis frist ikke er passert selv om opplysninger mangler`() {
        assertTrue(
            regel.skalAntaTimerArbeidet(
                underveisVurderinger = Tidslinje(
                    listOf(
                        Segment(
                            Periode(3 mars 2025, 16 mars 2025),
                            Vurdering(
                                fårAapEtter = RettighetsType.BISTANDSBEHOV,
                                meldeperiode = Periode(3 mars 2025, 16 mars 2025),
                            )
                        )
                    )
                ),
                opplysningerTidslinje = Tidslinje(emptyList()),
                dagensDato = 24 mars 2025
            ),
        )
    }

    @Test
    fun `skal ikke anta timer hvis opplysninger mangler`() {
        assertFalse(
            regel.skalAntaTimerArbeidet(
                underveisVurderinger = Tidslinje(
                    listOf(
                        Segment(
                            Periode(3 mars 2025, 16 mars 2025),
                            Vurdering(
                                fårAapEtter = RettighetsType.BISTANDSBEHOV,
                                meldeperiode = Periode(3 mars 2025, 16 mars 2025),
                            )
                        )
                    )
                ),
                opplysningerTidslinje = Tidslinje(emptyList()),
                dagensDato = 25 mars 2025
            ),
        )
    }

    @Test
    fun `skal anta timer hvis opplysninger er gitt`() {
        assertTrue(
            regel.skalAntaTimerArbeidet(
                underveisVurderinger = Tidslinje(
                    listOf(
                        Segment(
                            Periode(3 mars 2025, 16 mars 2025),
                            Vurdering(
                                fårAapEtter = RettighetsType.BISTANDSBEHOV,
                                meldeperiode = Periode(3 mars 2025, 16 mars 2025),
                            )
                        )
                    )
                ),
                opplysningerTidslinje = Tidslinje(
                    listOf(
                        Segment(
                            Periode(3 mars 2025, 16 mars 2025),
                            OpplysningerOmArbeid(
                                timerArbeid = TimerArbeid(BigDecimal.ZERO),
                                arbeidsevne = null,
                                opplysningerFørstMottatt = 17 mars 2025
                            )
                        )
                    ),
                ),
                dagensDato = 25 mars 2025
            )
        )
    }

    @Test
    fun `Hvis fastsatt arbeidsevne er lavere enn timer arbeidet, blir timer arbeidet brukt i utregning av gradering`() {
        val rettighetsperiode = Periode(LocalDate.parse("2024-10-31"), LocalDate.parse("2025-10-31"))
        val periode = Periode(rettighetsperiode.fom, rettighetsperiode.fom.plusDays(13))
        val input = underveisInput(
            rettighetsperiode = rettighetsperiode,
            fastsattArbeidsevne = Prosent.`50_PROSENT`,
            meldekort = meldekort(periode to BigDecimal(10))
        )
        val vurdering = vurder(input)

        assertEquals(
            Prosent.`50_PROSENT`,
            vurdering.segment(rettighetsperiode.fom)?.verdi?.arbeidsgradering()?.gradering
        )
    }

    @Test
    fun `Hvis timer arbeidet er lavere enn fastsatt arbeidsevne, blir fastsatt arbeidsevne brukt i utregning av gradering`() {
        val rettighetsperiode = Periode(LocalDate.parse("2024-10-31"), LocalDate.parse("2025-10-31"))
        val periode = Periode(rettighetsperiode.fom, rettighetsperiode.fom.plusDays(13))
        val input = underveisInput(
            rettighetsperiode = rettighetsperiode,
            fastsattArbeidsevne = Prosent.`30_PROSENT`,
            meldekort = meldekort(periode to BigDecimal(37.5))
        )
        val vurdering = vurder(input)

        assertEquals(Prosent(44), vurdering.segment(rettighetsperiode.fom)?.verdi?.arbeidsgradering()?.gradering)
    }

    @Test
    fun `Hvis det ikke er registrert timer arbeidet blir graderingen 0 prosent`() {
        val rettighetsperiode = Periode(LocalDate.parse("2024-10-31"), LocalDate.parse("2025-10-31"))
        val input = underveisInput(
            rettighetsperiode = rettighetsperiode,
            fastsattArbeidsevne = Prosent.`30_PROSENT`,
            meldekort = emptyList()
        )
        val vurdering = vurder(input)

        assertEquals(
            Prosent.`0_PROSENT`,
            vurdering.segment(rettighetsperiode.fom)?.verdi?.arbeidsgradering()?.gradering
        )
    }

    @Test
    fun `Kan jobbe opp til 80 prosent hvis det foreligger arbeidsopptrapping i perioden`() {
        val rettighetsperiode = Periode(LocalDate.parse("2024-10-31"), LocalDate.parse("2025-10-31"))
        val periode = Periode(rettighetsperiode.fom, rettighetsperiode.fom.plusDays(13))
        val input = underveisInput(
            rettighetsperiode = rettighetsperiode,
            fastsattArbeidsevne = null,
            opptrappingPerioder = listOf(periode),
            meldekort = meldekort(periode to BigDecimal(80))
        )
        val vurdering = vurder(input)

        assertEquals(
            Prosent(80),
            vurdering.segment(rettighetsperiode.fom)?.verdi?.grenseverdi()
        )
    }

    @Test
    fun `Hvis det ikke er registrert arbeidsevne brukes timer arbeidet i utregning av gradering`() {
        val rettighetsperiode = Periode(LocalDate.parse("2024-10-31"), LocalDate.parse("2025-10-31"))
        val periode = Periode(rettighetsperiode.fom, rettighetsperiode.fom.plusDays(13))
        val input = underveisInput(
            rettighetsperiode = rettighetsperiode,
            fastsattArbeidsevne = null,
            meldekort = meldekort(periode to BigDecimal(37.5))
        )
        val vurdering = vurder(input)

        assertEquals(Prosent(44), vurdering.segment(rettighetsperiode.fom)?.verdi?.arbeidsgradering()?.gradering)
    }

    @Test
    fun `Jobber over 100 prosent`() {
        val rettighetsperiode = Periode(LocalDate.parse("2024-10-31"), LocalDate.parse("2025-10-31"))
        val periode = Periode(rettighetsperiode.fom, rettighetsperiode.fom.plusDays(13))
        val input = underveisInput(
            rettighetsperiode = rettighetsperiode,
            fastsattArbeidsevne = null,
            meldekort = meldekort(periode to BigDecimal(100))
        )
        val vurdering = vurder(input)

        assertEquals(
            Prosent.`0_PROSENT`,
            vurdering.segment(rettighetsperiode.fom)?.verdi?.arbeidsgradering()?.gradering
        )
    }

    @Test
    fun `Jobber over flere meldeperioder`() {
        val rettighetsperiode = Periode(LocalDate.parse("2022-10-31"), LocalDate.parse("2023-10-31"))
        val meldeperiode1 = Periode(rettighetsperiode.fom, rettighetsperiode.fom.plusDays(13))
        val meldeperiode2 = Periode(meldeperiode1.tom.plusDays(1), meldeperiode1.tom.plusDays(14))
        val input = underveisInput(
            rettighetsperiode = rettighetsperiode,
            fastsattArbeidsevne = Prosent.`30_PROSENT`,
            meldekort = meldekort(meldeperiode1 to BigDecimal.ZERO, meldeperiode2 to BigDecimal(37.5))
        )
        val vurdering = vurder(input)

        assertEquals(Prosent.`70_PROSENT`, vurdering.segment(meldeperiode1.fom)?.verdi?.arbeidsgradering()?.gradering)
        assertEquals(Prosent.`50_PROSENT`, vurdering.segment(meldeperiode2.fom)?.verdi?.arbeidsgradering()?.gradering)
    }

    @Test
    fun `meldekort som overlapper blir prioritert fra tidligst til nyligst innsendt`() {
        val rettighetsperiode = Periode(LocalDate.parse("2022-10-31"), LocalDate.parse("2023-10-31"))
        val meldeperiode1 = Periode(rettighetsperiode.fom, rettighetsperiode.fom.plusDays(13))
        val meldeperiode2 = Periode(meldeperiode1.tom.plusDays(1), meldeperiode1.tom.plusDays(14))
        val input = underveisInput(
            rettighetsperiode = rettighetsperiode,
            fastsattArbeidsevne = Prosent.`30_PROSENT`,
            meldekort = meldekort(
                meldeperiode1.utvid(Periode(meldeperiode2.fom, meldeperiode2.fom.plusDays(3))) to BigDecimal.ZERO,
                meldeperiode2 to BigDecimal(37.5)
            )
        )
        val vurdering = vurder(input)

        assertEquals(Prosent.`70_PROSENT`, vurdering.segment(meldeperiode1.fom)?.verdi?.arbeidsgradering()?.gradering)
        assertEquals(Prosent.`50_PROSENT`, vurdering.segment(meldeperiode2.fom)?.verdi?.arbeidsgradering()?.gradering)
    }

    @Test
    fun `korrigering av meldekort på samme dag`() {
        val rettighetsperiode = Periode(LocalDate.parse("2022-10-31"), LocalDate.parse("2023-10-31"))
        val meldeperiode1 = Periode(rettighetsperiode.fom, rettighetsperiode.fom.plusDays(13))
        val meldeperiode2 = Periode(meldeperiode1.tom.plusDays(1), meldeperiode1.tom.plusDays(14))
        val meldekortPeriode1 = meldeperiode1 to BigDecimal.ZERO
        val meldekortPeriode2 = meldeperiode2 to BigDecimal(3)
        val korrigertMeldekortPeriode1 = meldeperiode1 to BigDecimal(3)
        val korrigertMeldekortPeriode2 = meldeperiode2 to BigDecimal(5)
        val allemeldekort = meldekort(
            meldekortPeriode1,
            meldekortPeriode2,
            korrigertMeldekortPeriode1,
            korrigertMeldekortPeriode2,
        )
        val input = underveisInput(
            rettighetsperiode = rettighetsperiode,
            fastsattArbeidsevne = Prosent.`30_PROSENT`,
            meldekort = allemeldekort
        ).copy(innsendingsTidspunkt = allemeldekort.associate { it.mottattTidspunkt.toLocalDate() to it.journalpostId })
        val vurdering = vurder(input)

        assertEquals(Prosent.`70_PROSENT`, vurdering.segment(meldeperiode1.fom)?.verdi?.arbeidsgradering()?.gradering)
        assertEquals(Prosent.`70_PROSENT`, vurdering.segment(meldeperiode2.fom)?.verdi?.arbeidsgradering()?.gradering)
    }

    @Test
    fun `Fritak for meldeperiode som er passert skal gi null timer med opplysningstidspunkt satt, fritak som kommer senere skal ikke ha satt opplysninger mottatt tidspunkt`() {
        val rettighetsperiode = Periode(LocalDate.now().minusMonths(1), LocalDate.now().plusMonths(11).minusDays(1))
        val input = underveisInput(
            rettighetsperiode = rettighetsperiode,
            fastsattArbeidsevne = Prosent.`0_PROSENT`,
            meldekort = emptyList(),
            fritaksvurderinger =
                listOf(
                    Fritaksvurdering(
                        harFritak = true,
                        fraDato = rettighetsperiode.fom,
                        begrunnelse = "kan ikke",
                        vurdertAv = "saksbehandler",
                        opprettetTid = rettighetsperiode.fom.atStartOfDay(),
                    )
                )
        )
        val vurdering = vurder(input)

        vurdering.segmenter().forEach {
            if (LocalDate.now() >= it.tom().plusDays(1)) {
                Assertions.assertThat(it.verdi.arbeidsgradering().opplysningerMottatt).isNotNull
            } else {
                Assertions.assertThat(it.verdi.arbeidsgradering().opplysningerMottatt).isNull()
            }
        }
        assertEquals(
            Prosent.`100_PROSENT`,
            vurdering.segment(rettighetsperiode.fom)?.verdi?.arbeidsgradering()?.gradering
        )
    }


    private fun underveisInput(
        rettighetsperiode: Periode,
        fastsattArbeidsevne: Prosent?,
        meldekort: List<Meldekort>,
        fritaksvurderinger: List<Fritaksvurdering> = emptyList(),
        opptrappingPerioder: List<Periode> = emptyList()
    ) = tomUnderveisInput(
        innsendingsTidspunkt = meldekort.associate { it.mottattTidspunkt.toLocalDate() to it.journalpostId },
        rettighetsperiode = rettighetsperiode,
        meldekort = meldekort,
        meldepliktGrunnlag = MeldepliktGrunnlag(vurderinger = fritaksvurderinger),
        opptrappingPerioder = opptrappingPerioder,
        arbeidsevneGrunnlag = ArbeidsevneGrunnlag(
            listOfNotNull(fastsattArbeidsevne?.let {
                ArbeidsevneVurdering(
                    begrunnelse = "",
                    arbeidsevne = it,
                    fraDato = rettighetsperiode.fom,
                    opprettetTid = LocalDateTime.now(),
                    "vurdertAv"
                )
            })
        )
    )

    private fun meldekort(vararg periodeOgTimerArbeidet: Pair<Periode, BigDecimal>): List<Meldekort> {
        return periodeOgTimerArbeidet.mapIndexed { i, (periode, timerArbeidet) ->
            Meldekort(
                journalpostId = JournalpostId(i.toString()),
                timerArbeidPerPeriode = setOf(
                    ArbeidIPeriode(
                        periode = periode,
                        timerArbeid = TimerArbeid(timerArbeidet)
                    )
                ),
                mottattTidspunkt = LocalDateTime.now().plusMinutes(i.toLong()),
            )
        }
    }

    private fun vurder(input: UnderveisInput): Tidslinje<Vurdering> {
        return regel.vurder(
            input,
            UtledMeldeperiodeRegel().vurder(
                input,
                Tidslinje(input.rettighetsperiode, Vurdering(fårAapEtter = RettighetsType.BISTANDSBEHOV))
            )
        ).mapValue { it.copy(fårAapEtter = RettighetsType.BISTANDSBEHOV) }
    }
}