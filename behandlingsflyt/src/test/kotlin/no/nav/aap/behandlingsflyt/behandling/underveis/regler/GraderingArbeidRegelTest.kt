package no.nav.aap.behandlingsflyt.behandling.underveis.regler

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.ArbeidIPeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.Meldekort
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsevne.ArbeidsevneGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsevne.ArbeidsevneVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.Fritaksvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.MeldepliktGrunnlag
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.komponenter.verdityper.Prosent.Companion.`0_PROSENT`
import no.nav.aap.komponenter.verdityper.Prosent.Companion.`50_PROSENT`
import no.nav.aap.komponenter.verdityper.TimerArbeid
import no.nav.aap.verdityper.dokument.JournalpostId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

class GraderingArbeidRegelTest {
    @Test
    fun `Hvis timer arbeidet er lavere enn fastsatt arbeidsevne, blir fastsatt arbeidsevne brukt i utregning av gradering`() {
        assertMeldekortutregning(
            fastsattArbeidsevne = `50_PROSENT`,
            opptrapping = false,
            timerArbeidet = List(9) { 1.0 } + List(5) { 0.0 }, // 9 timer

            forventetAndelArbeid = Prosent(12),
            forventetGradering = `50_PROSENT`,
        )
    }

    @Test
    fun `Hvis fastsatt arbeidsevne er lavere enn timer arbeidet, blir timer arbeidet brukt i utregning av gradering`() {
        assertMeldekortutregning(
            fastsattArbeidsevne = Prosent.`30_PROSENT`,
            opptrapping = false,
            timerArbeidet = listOf(10.0, 10.0, 10.0, 7.5) + List(10) { 0.0 }, // 37.5 timer

            forventetGradering = `50_PROSENT`,
            forventetAndelArbeid = `50_PROSENT`,
        )
    }

    @Test
    fun `Kan jobbe opp til 60 prosent uten arbeidsopptrapping`() {
        assertMeldekortutregning(
            fastsattArbeidsevne = null,
            opptrapping = false,
            timerArbeidet = List(4) { 10.0 } + listOf(5.0) + List(9) { 0.0 }, /* 45 timer */

            forventetGradering = Prosent(40),
            forventetAndelArbeid = Prosent(60),
        )
    }

    @Test
    fun `Kan jobbe opp til 80 prosent med arbeidsopptrapping`() {
        assertMeldekortutregning(
            fastsattArbeidsevne = null,
            opptrapping = true,
            timerArbeidet = List(6) { 10.0 } + List(8) { 0.0 }, /* 60 timer */
            forventetGradering = Prosent(20),
            forventetAndelArbeid = Prosent(80),
        )
    }

    @Test
    @Disabled /* Vi har ikke nok presisjon i Prosent for at denne testen blir grønn. Må den være grønn? */
    fun `Kan ikke jobbe minste inkrement (0,5 timer) over 60 prosent uten arbeidsopptrapping`() {
        assertMeldekortutregning(
            fastsattArbeidsevne = null,
            opptrapping = false,
            timerArbeidet = List(4) { 10.0 } + listOf(5.5) + List(9) { 0.0 }, // 45.5 timer
            forventetGradering = Prosent(0),
            forventetAndelArbeid = Prosent.fraDesimal(BigDecimal("0.60666666666")), /* Hvor presist skal vi regne? */
        )
    }

    @Test
    @Disabled /* Vi har ikke nok presisjon i Prosent for at denne testen blir grønn. Må den være grønn? */
    fun `Kan ikke jobbe minste inkrement (0,5 timer) over 80 prosent med arbeidsopptrapping`() {
        assertMeldekortutregning(
            fastsattArbeidsevne = null,
            opptrapping = true,
            timerArbeidet = List(6) { 10.0 } + listOf(0.5) + List(7) { 0.0 }, // 60.5 timer
            forventetGradering = Prosent(0),
            forventetAndelArbeid = Prosent.fraDesimal(BigDecimal("0.80666666666")), /* Hvor presist skal vi regne? */
        )
    }

    @Test
    fun `Kan ikke jobbe  over 60 prosent uten arbeidsopptrapping`() {
        assertMeldekortutregning(
            fastsattArbeidsevne = null,
            opptrapping = false,
            timerArbeidet = List(4) { 10.0 } + listOf(6.5) + List(9) { 0.0 }, // 46.5 timer
            forventetGradering = Prosent(0),
            forventetAndelArbeid = Prosent(62),
        )
    }

    @Test
    fun `Kan ikke jobbe over 80 prosent hvis det foreligger arbeidsopptrapping i perioden`() {
        assertMeldekortutregning(
            fastsattArbeidsevne = null,
            opptrapping = true,
            timerArbeidet = List(6) { 10.0 } + listOf(1.5) + List(7) { 0.0 }, // 61.5 timer
            forventetGradering = Prosent(0),
            forventetAndelArbeid = Prosent(82),
        )
    }

    @Test
    fun `Hvis det ikke er registrert arbeidsevne brukes timer arbeidet i utregning av gradering`() {
        assertMeldekortutregning(
            fastsattArbeidsevne = null,
            opptrapping = false,
            timerArbeidet = listOf(10.0, 10.0, 10.0, 7.5) + List(10) { 0.0 }, // 37.5 timer
            forventetGradering = Prosent(50),
            forventetAndelArbeid = Prosent(50),
        )
    }

    @Test
    fun `Jobber over 100 prosent`() {
        assertMeldekortutregning(
            fastsattArbeidsevne = null,
            opptrapping = false,
            timerArbeidet = List(14) { 8.0 }, // 112 timer
            forventetGradering = Prosent(0),
            forventetAndelArbeid = Prosent(100), /* skulle prosenten gått > 100? */
        )
    }

    @Test
    fun `Hvis det ikke er registrert timer arbeidet blir graderingen 0 prosent`() {
        assertMeldekortutregning(
            fastsattArbeidsevne = Prosent.`30_PROSENT`,
            opptrapping = false,
            godtaTimerMangler = true,
            timerArbeidet = listOf(),
            forventetGradering = Prosent(0),
            forventetAndelArbeid = Prosent(0), /* litt tilfeldig om man bruker 0, null eller 100 egentlig. */
        )
    }

    @Test
    fun `Setter grenseverdi når det er opptrappingsperiode`() {
        val rettighetsperiode = Periode(LocalDate.parse("2024-10-31"), LocalDate.parse("2025-10-31"))
        val periode = Periode(rettighetsperiode.fom, rettighetsperiode.fom.plusDays(13))
        val input = underveisInput(
            rettighetsperiode = rettighetsperiode,
            fastsattArbeidsevne = null,
            opptrappingPerioder = listOf(periode),
            meldekort = meldekort(periode.fom to List(6) { 10.0 } + List(8) { 0.0 })
        )
        val vurdering = vurder(input)

        assertEquals(
            Prosent(80),
            vurdering.segment(rettighetsperiode.fom)?.verdi?.grenseverdi()
        )
    }

    @Test
    fun `Arbeidsevnevurdering fører ikke til vurdering utenfor rettighetsperioden`() {
        val fom = LocalDate.parse("2025-11-24")
        val rettighetsperiode = Periode(fom, LocalDate.parse("2026-11-23"))
        val input = tomUnderveisInput(
            rettighetsperiode = rettighetsperiode,
            arbeidsevneGrunnlag = ArbeidsevneGrunnlag(
                listOf(
                    ArbeidsevneVurdering(
                        begrunnelse = "",
                        arbeidsevne = `50_PROSENT`,
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
    fun `Jobber over flere meldeperioder`() {
        val rettighetsperiode = Periode(LocalDate.parse("2022-10-31"), LocalDate.parse("2023-10-31"))
        val meldeperiode1 = Periode(rettighetsperiode.fom, rettighetsperiode.fom.plusDays(13))
        val meldeperiode2 = Periode(meldeperiode1.tom.plusDays(1), meldeperiode1.tom.plusDays(14))
        val input = underveisInput(
            rettighetsperiode = rettighetsperiode,
            fastsattArbeidsevne = Prosent.`30_PROSENT`,
            meldekort = meldekort(
                meldeperiode1.fom to List(14) { 0.0 },
                meldeperiode2.fom to listOf(10.0, 10.0, 10.0, 7.5) + List(10) { 0.0 }, // 37.5 timer
            )
        )
        val vurdering = vurder(input)

        assertEquals(Prosent.`70_PROSENT`, vurdering.segment(meldeperiode1.fom)?.verdi?.arbeidsgradering()?.gradering)
        assertEquals(`50_PROSENT`, vurdering.segment(meldeperiode2.fom)?.verdi?.arbeidsgradering()?.gradering)
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
                meldeperiode1.fom to List(17) { 0.0 },
                meldeperiode2.fom to listOf(10.0, 10.0, 10.0, 7.5) + List(10) { 0.0 }, // 37.5
            )
        )
        val vurdering = vurder(input)

        assertEquals(Prosent.`70_PROSENT`, vurdering.segment(meldeperiode1.fom)?.verdi?.arbeidsgradering()?.gradering)
        assertEquals(`50_PROSENT`, vurdering.segment(meldeperiode2.fom)?.verdi?.arbeidsgradering()?.gradering)
    }

    @Test
    fun `korrigering av meldekort på samme dag`() {
        val rettighetsperiode = Periode(LocalDate.parse("2022-10-31"), LocalDate.parse("2023-10-31"))
        val meldeperiode1 = Periode(rettighetsperiode.fom, rettighetsperiode.fom.plusDays(13))
        val meldeperiode2 = Periode(meldeperiode1.tom.plusDays(1), meldeperiode1.tom.plusDays(14))
        val allemeldekort = meldekort(
            meldeperiode1.fom to List(14) { 0.0 },
            meldeperiode2.fom to listOf(3.0) + List(13) { 0.0 },
            meldeperiode1.fom to List(13) { 0.0 } + listOf(3.0),
            meldeperiode2.fom to List(13) { 0.0 } + listOf(5.0),
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
            fastsattArbeidsevne = `0_PROSENT`,
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
                assertThat(it.verdi.arbeidsgradering().opplysningerMottatt).isNotNull
            } else {
                assertThat(it.verdi.arbeidsgradering().opplysningerMottatt).isNull()
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

    private fun meldekort(vararg datoOgTimer: Pair<LocalDate, List<Double>>): List<Meldekort> {
        return datoOgTimer.mapIndexed { i, (førsteDag, timerArbeidet) ->
            Meldekort(
                journalpostId = JournalpostId(i.toString()),
                timerArbeidPerPeriode =
                    timerArbeidet.mapIndexed { j, timer ->
                        val dato = førsteDag.plusDays(j.toLong())
                        ArbeidIPeriode(
                            periode = Periode(dato, dato),
                            timerArbeid = TimerArbeid(BigDecimal(timer))
                        )
                    }.toSet(),
                mottattTidspunkt = LocalDateTime.now().plusMinutes(i.toLong()),
            )
        }
    }

    private fun vurder(input: UnderveisInput): Tidslinje<Vurdering> {
        fun Tidslinje<Vurdering>.kjørRegel(regel: UnderveisRegel): Tidslinje<Vurdering> {
            return regel.vurder(input, this)
        }
        return Tidslinje(input.periodeForVurdering, Vurdering(fårAapEtter = RettighetsType.BISTANDSBEHOV))
            .kjørRegel(UtledMeldeperiodeRegel())
            .kjørRegel(FastsettGrenseverdiArbeidRegel())
            .kjørRegel(GraderingArbeidRegel())
    }

    fun assertMeldekortutregning(
        fastsattArbeidsevne: Prosent? = null,
        opptrapping: Boolean = false,
        timerArbeidet: List<Double>,
        forventetGradering: Prosent,
        forventetAndelArbeid: Prosent,
        godtaTimerMangler: Boolean = false,
    ) {
        if (!godtaTimerMangler) {
            check(timerArbeidet.size == 14) {
                "mangler timer i meldeperioden"
            }
        }
        val fom = LocalDate.parse("2025-11-24")
        val rettighetsperiode = Periode(fom, fom.plusDays(13))
        val input = underveisInput(
            rettighetsperiode = rettighetsperiode,
            fastsattArbeidsevne = fastsattArbeidsevne,
            opptrappingPerioder = if (opptrapping) listOf(rettighetsperiode) else emptyList(),
            meldekort = meldekort(fom to timerArbeidet)
        )
        val vurdering = vurder(input)

        assertEquals(
            forventetGradering,
            vurdering.segment(fom)?.verdi?.arbeidsgradering()?.gradering,
            "forventet gradering",
        )

        assertEquals(
            forventetAndelArbeid,
            vurdering.segment(fom)?.verdi?.arbeidsgradering()?.andelArbeid,
            "forventet andel arbeid",
        )
    }
}