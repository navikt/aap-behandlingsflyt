package no.nav.aap.behandlingsflyt.behandling.underveis.regler

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType.BISTANDSBEHOV
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.Fritaksvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.MeldepliktOverstyringStatus
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.MeldepliktGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.OverstyringMeldepliktGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.OverstyringMeldepliktVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.OverstyringMeldepliktVurderingPeriode
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.test.FakeUnleash
import no.nav.aap.behandlingsflyt.test.april
import no.nav.aap.behandlingsflyt.test.desember
import no.nav.aap.behandlingsflyt.test.februar
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.behandlingsflyt.test.juni
import no.nav.aap.behandlingsflyt.test.mai
import no.nav.aap.behandlingsflyt.test.mars
import no.nav.aap.behandlingsflyt.test.november
import no.nav.aap.komponenter.tidslinje.JoinStyle
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.tidslinjeOf
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.verdityper.dokument.JournalpostId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId


class MeldepliktRegelTest {
    private data class Forventer(
        val fom: LocalDate,
        val tom: LocalDate,
        val vurdering: MeldepliktVurdering,
    )

    @Test
    fun `Sent virkningstidspunkt ny adferdi`() {
        val rettighetsperiode = Periode(
            31 mars 2025,
            30 mars 2026
        )

        val input = tomUnderveisInput(
            rettighetsperiode = rettighetsperiode,
            innsendingsTidspunkt = mapOf(
                26 mai 2025 to JournalpostId("1"),
            ),
        )

        val vurdertTidslinje = vurder(
            input,
            nå = 26 mai 2025,
            vurderinger = tidslinjeOf(
                Periode(31 mars 2025, 11 mai 2025) to Vurdering(fårAapEtter = null),
                Periode(12 mai 2025, 30 mars 2026) to Vurdering(fårAapEtter = BISTANDSBEHOV),
            )
        )

        assertVurdering(
            vurdertTidslinje, rettighetsperiode,
            Forventer(
                fom = 31 mars 2025,
                tom = 13 april 2025,
                vurdering = MeldepliktVurdering.FørVedtak,
            ),
            Forventer(
                fom = 14 april 2025,
                tom = 11 mai 2025,
                vurdering = MeldepliktVurdering.UtenRett,
            ),
            Forventer(
                fom = 12 mai 2025,
                tom = 25 mai 2025,
                vurdering = MeldepliktVurdering.FørsteMeldeperiodeMedRett,
            ),
            Forventer(
                fom = 26 mai 2025,
                tom = 8 juni 2025,
                vurdering = MeldepliktVurdering.MeldtSeg(JournalpostId("1")),
            ),
            Forventer(
                fom = 9 juni 2025,
                tom = 30 mars 2026,
                vurdering = MeldepliktVurdering.FremtidigOppfylt,
            ),
        )
    }

    /*   2020
     *           January                   February                   March
     *   Mo  Tu We Th Fr Sa Su      Mo  Tu We Th Fr Sa  Su      Mo  Tu We Th Fr  Sa  Su
     *                                               1   2                           [1]
     *   [6]  7  8  9 10 11 12      [3]  4  5  6  7  8   9
     *   13  14 15 16 17 18 19      10  11 12 13 14 15 [16]
     *  [20] 21 22 23 24 25 26     [17] 18 19 20 21 22  23
     *   27  28 29 30 31            24  25 26 27 28 29
     *
     */
    @Test
    fun `Meldeplikt overholdt ved innsendt på fastsatt dag`() {
        val rettighetsperiode = Periode(
            6 januar 2020,
            1 mars 2020
        )
        val input = tomUnderveisInput(
            rettighetsperiode = rettighetsperiode,
            innsendingsTidspunkt = mapOf(
                20 januar 2020 to JournalpostId("1"),
                3 februar 2020 to JournalpostId("2"),
                17 februar 2020 to JournalpostId("3"),
            ),
        )

        val vurdertTidslinje = vurder(input, nå = rettighetsperiode.tom.plusDays(1))

        assertVurdering(
            vurdertTidslinje, rettighetsperiode,
            Forventer(
                fom = 6 januar 2020,
                tom = 19 januar 2020,
                vurdering = MeldepliktVurdering.FørVedtak,
            ),
            Forventer(
                fom = 20 januar 2020,
                tom = 2 februar 2020,
                vurdering = MeldepliktVurdering.MeldtSeg(JournalpostId("1")),
            ),
            Forventer(
                fom = 3 februar 2020,
                tom = 16 februar 2020,
                vurdering = MeldepliktVurdering.MeldtSeg(JournalpostId("2")),
            ),
            Forventer(
                fom = 17 februar 2020,
                tom = 1 mars 2020,
                vurdering = MeldepliktVurdering.MeldtSeg(JournalpostId("3")),
            ),
        )
    }

    /**
     * ```
     *     December 2025              January 2026
     *  Su Mo Tu We Th Fr Sa     Su Mo Tu We Th Fr Sa
     *      1  2  3  4  5  6                  1  2  3
     *   7  8  9 10 11 12 13      4  5  6  7  8  9 10
     *  14 15 16 17 18 19 20     11 12 13 14 15 16 17
     *  21 22 23 24 25 26 27     18 19 20 21 22 23 24
     *  28 29 30 31              25 26 27 28 29 30 31
     *  ```
     */
    @Test
    fun `skal kunne levere tidlig meldekort 17 desember, og oppfylle meldeplikt i perioden etter`() {
        /// få oppfylt også neste meldeperiode (3 ukers)
        val rettighetsperiode = Periode(29 november 2025, 19 januar 2026)
        val input = tomUnderveisInput(
            rettighetsperiode = rettighetsperiode,
            innsendingsTidspunkt = mapOf(
                8 desember 2025 to JournalpostId("2"),
                17 desember 2025 to JournalpostId("3"), // meldekort blir sendt inn
                5 januar 2026 to JournalpostId("4"),
                19 januar 2026 to JournalpostId("5"),
            ),
        )
        // behandling kjøres på `nå`
        val vurdertTidslinje = vurder(input, nå = 19 januar 2026)

        assertVurdering(
            vurdertTidslinje, rettighetsperiode,
            Forventer(
                fom = 29 november 2025,
                tom = 7 desember 2025,
                vurdering = MeldepliktVurdering.FørVedtak,
            ),
            Forventer(
                fom = 8 desember 2025,
                tom = 21 desember 2025,
                vurdering = MeldepliktVurdering.MeldtSeg(JournalpostId("2")),
            ),
            Forventer(
                fom = 22 desember 2025,
                tom = 4 januar 2026,
                vurdering = MeldepliktVurdering.MeldtSeg(JournalpostId("3")), // MeldepliktVurdering.IkkeMeldtSeg //
            ),
            Forventer(
                fom = 5 januar 2026,
                tom = 18 januar 2026,
                vurdering = MeldepliktVurdering.MeldtSeg(JournalpostId("4")), //MeldepliktVurdering.MeldtSeg(JournalpostId("4")),
            ),
            Forventer(
                fom = 19 januar 2026,
                tom = 19 januar 2026,
                vurdering = MeldepliktVurdering.MeldtSeg(JournalpostId("5")),
            ),
        )
    }

    /**
     * ```
     *    November 2025           December 2025              January 2026
     * Su Mo Tu We Th Fr Sa    Su Mo Tu We Th Fr Sa     Su Mo Tu We Th Fr Sa
     *                    1        1  2  3  4  5  6                  1  2  3
     *  2  3  4  5  6  7  8     7  8  9 10 11 12 13      4  5  6  7  8  9 10
     *  9 10 11 12 13 14 15    14 15 16 17 18 19 20     11 12 13 14 15 16 17
     * 16 17 18 19 20 21 22    21 22 23 24 25 26 27     18 19 20 21 22 23 24
     * 23 24 25 26 27 28 29    28 29 30 31              25 26 27 28 29 30 31
     * 30
     * ```
     */
    @Test
    fun `sender inn 17de, men er ikke ajour med meldeplikt`() {
        val rettighetsperiode = Periode(24 november 2025, 19 januar 2026)
        val input = tomUnderveisInput(
            rettighetsperiode = rettighetsperiode,
            innsendingsTidspunkt = mapOf(
                17 desember 2025 to JournalpostId("3"),
                5 januar 2026 to JournalpostId("5"),
                19 januar 2026 to JournalpostId("6"),
            ),
        )
        // behandling kjøres på `nå`
        val vurdertTidslinje = vurder(input, nå = 19 januar 2026)

        assertVurdering(
            vurdertTidslinje, rettighetsperiode,
            // oppfylt pga søknad
            Forventer(
                fom = 24 november 2025,
                tom = 7 desember 2025,
                vurdering = MeldepliktVurdering.FørVedtak,
            ),
            // ikke meldt seg for en uke
            Forventer(
                fom = 8 desember 2025,
                tom = 16 desember 2025,
                vurdering = MeldepliktVurdering.IkkeMeldtSeg,
            ),
            // ok for resten av perioden
            Forventer(
                fom = 17 desember 2025,
                tom = 21 desember 2025,
                vurdering = MeldepliktVurdering.MeldtSeg(JournalpostId("3")),
            ),
            Forventer(
                fom = 22 desember 2025,
                tom = 4 januar 2026,
                vurdering = MeldepliktVurdering.MeldtSeg(JournalpostId("3")),
            ),
            Forventer(
                fom = 5 januar 2026,
                tom = 18 januar 2026,
                vurdering = MeldepliktVurdering.MeldtSeg(JournalpostId("5")),
            ),
            Forventer(
                fom = 19 januar 2026,
                tom = 19 januar 2026,
                vurdering = MeldepliktVurdering.MeldtSeg(JournalpostId("6")),
            ),
        )
    }

    /**
     * ```
     *    November 2025           December 2025              January 2026
     * Su Mo Tu We Th Fr Sa    Su Mo Tu We Th Fr Sa     Su Mo Tu We Th Fr Sa
     *                    1        1  2  3  4  5  6                  1  2  3
     *  2  3  4  5  6  7  8     7  8  9 10 11 12 13      4  5  6  7  8  9 10
     *  9 10 11 12 13 14 15    14 15 16 17 18 19 20     11 12 13 14 15 16 17
     * 16 17 18 19 20 21 22    21 22 23 24 25 26 27     18 19 20 21 22 23 24
     * 23 24 25 26 27 28 29    28 29 30 31              25 26 27 28 29 30 31
     * 30
     * ```
     */
    @ParameterizedTest
    @ValueSource(ints = [15, 16, 17])
    fun `samme tester som over, men for ikke samme fase`(dag: Int) {
        val rettighetsperiode = Periode(23 november 2025, 19 januar 2026)
        val input = tomUnderveisInput(
            rettighetsperiode = rettighetsperiode,
            innsendingsTidspunkt = mapOf(
                1 desember 2025 to JournalpostId("2"),
                dag desember 2025 to JournalpostId("3"),
                29 desember 2025 to JournalpostId("4"),
                12 januar 2026 to JournalpostId("5"),
            ),
        )
        // behandling kjøres på `nå`
        val vurdertTidslinje = vurder(input, nå = 19 januar 2026)

        assertVurdering(
            vurdertTidslinje, rettighetsperiode,
            Forventer(
                fom = 23 november 2025,
                tom = 30 november 2025,
                vurdering = MeldepliktVurdering.FørVedtak,
            ),
            Forventer(
                fom = 1 desember 2025,
                tom = 7 desember 2025,
                vurdering = MeldepliktVurdering.MeldtSeg(JournalpostId("2")),
            ),
            Forventer(
                fom = 15 desember 2025,
                tom = 28 desember 2025,
                vurdering = MeldepliktVurdering.MeldtSeg(JournalpostId("3")),
            ),
            Forventer(
                fom = 29 desember 2025,
                tom = 11 januar 2026,
                vurdering = MeldepliktVurdering.MeldtSeg(JournalpostId("4")),
            ),
            Forventer(
                fom = 12 januar 2026,
                tom = 19 januar 2026,
                vurdering = MeldepliktVurdering.MeldtSeg(JournalpostId("5")),
            ),
        )
    }

    /**
    ```
    April                      May                       June
    Mo Tu We Th Fr Sa Su      Mo Tu We Th Fr Sa Su      Mo Tu We Th Fr Sa Su
    1  2  3  4  5                   1  2  3       1  2  3  4  5  6  7
    6  7  8  9 10 11 12       4  5  6  7  8  9 10       8  9 10 11 12 13 14
    13 14 15 16 17 18 19      11 12 13 14 15 16 17      15 16 17 18 19 20 21
    20 21 22 23 24 25 26      18 19 20 21 22 23 24      22 23 24 25 26 27 28
    27 28 29 30               25 26 27 28 29 30 31      29 30
    ```
     */
    @Test
    fun `Skal starte med full utbetalingsplan`() {
        val rettighetsperiode = Periode(20 april 2020, 19 april 2021)
        val input = tomUnderveisInput(rettighetsperiode = rettighetsperiode)
        val vurdertTidslinje = vurder(input, nå = 20 april 2020)

        assertVurdering(
            vurdertTidslinje, rettighetsperiode,
            Forventer(
                fom = 20 april 2020,
                tom = 3 mai 2020,
                vurdering = MeldepliktVurdering.FørVedtak,
            ),
            Forventer(
                fom = 4 mai 2020,
                tom = 19 april 2021,
                vurdering = MeldepliktVurdering.FremtidigOppfylt,
            ),
        )
    }

    /*   2020
     *           January                   February                   March
     *   Mo  Tu We Th Fr Sa Su      Mo Tu We Th Fr Sa Su      Mo  Tu We Th Fr  Sa  Su
     *                                              1 [2]                           1
     *   [6]  7  8  9 10 11 12       3  4  5  6  7  8  9       2   3  4  5 [6]  7   8
     *   13  14 15 16 17 18 19      10 11 12 13 14 15 16       9  10 11 12 13  14  15
     *   20  21 22 23 24 25 26      17 18 19 20 21 22 23      16  17 18 19 20  21  22
     *   27  28 29 30 31            24 25 26 27 28 29         23  24 25 26 27  28 [29]
     *                                                       [30] 31
     */
    @Test
    fun `Meldeplikt skal være stanset etter at man ikke har meldt seg og fristen utløpt`() {
        val rettighetsperiode = Periode(6 januar 2020, 29 mars 2020)

        val input = tomUnderveisInput(
            rettighetsperiode = rettighetsperiode,
            innsendingsTidspunkt = mapOf(
                2 februar 2020 to JournalpostId("1"),
                6 mars 2020 to JournalpostId("2"),
            ),
        )

        val vurdertTidslinje = vurder(input, nå = rettighetsperiode.tom.plusDays(1))

        assertVurdering(
            vurdertTidslinje, rettighetsperiode,
            Forventer(
                fom = 6 januar 2020, tom = 19 januar 2020,
                vurdering = MeldepliktVurdering.FørVedtak,
            ),
            Forventer(
                fom = 20 januar 2020, tom = 1 februar 2020,
                vurdering = MeldepliktVurdering.IkkeMeldtSeg,
            ),
            Forventer(
                fom = 2 februar 2020, tom = 2 februar 2020,
                vurdering = MeldepliktVurdering.MeldtSeg(JournalpostId("1")),
            ),
            Forventer(
                fom = 3 februar 2020, tom = 16 februar 2020,
                vurdering = MeldepliktVurdering.IkkeMeldtSeg,
            ),
            Forventer(
                fom = 17 februar 2020, tom = 1 mars 2020,
                vurdering = MeldepliktVurdering.IkkeMeldtSeg,
            ),
            Forventer(
                fom = 2 mars 2020, tom = 15 mars 2020,
                vurdering = MeldepliktVurdering.MeldtSeg(JournalpostId("2")),
            ),
            Forventer(
                fom = 16 mars 2020, tom = 29 mars 2020,
                vurdering = MeldepliktVurdering.IkkeMeldtSeg,
            ),
        )
    }

    /*
               April                      May                       June
         Mo Tu We Th Fr Sa Su      Mo Tu We Th Fr Sa Su      Mo Tu We Th Fr Sa Su
                1  2  3  4  5                   1  2  3       1  2  3  4  5  6  7
          6  7  8  9 10 11 12       4  5  6  7  8  9 10       8  9 10 11 12 13 14
         13 14 15 16 17 18 19      11 12 13 14 15 16 17      15 16 17 18 19 20 21
         20 21 22 23 24 25 26      18 19 20 21 22 23 24      22 23 24 25 26 27 28
         27 28 29 30               25 26 27 28 29 30 31      29 30
         */
    @Test
    fun `Meldeplikt overholdt ved fritak hele meldeperioden`() {
        val rettighetsperiode = Periode(
            20 april 2020,
            19 april 2021,
        )
        val input = tomUnderveisInput(
            rettighetsperiode = rettighetsperiode,
            meldepliktGrunnlag = MeldepliktGrunnlag(
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
        )

        val vurdertTidslinje = vurder(input, nå = rettighetsperiode.tom.plusDays(1))

        assertVurdering(
            vurdertTidslinje, rettighetsperiode,
            Forventer(
                fom = rettighetsperiode.fom,
                tom = rettighetsperiode.fom.plusDays(13),
                vurdering = MeldepliktVurdering.FørVedtak,
            ),
            Forventer(
                fom = rettighetsperiode.fom.plusDays(14),
                tom = rettighetsperiode.tom,
                vurdering = MeldepliktVurdering.Fritak
            )
        )
    }

    /*
                 April                      May                       June
          Mo  Tu We Th Fr Sa Su      Mo Tu We Th Fr Sa Su      Mo Tu We Th Fr Sa Su
                  1  2  3  4  5                   1  2  3       1  2  3  4  5  6  7
           6   7  8  9 10 11 12       4  5  6  7  8  9 10       8  9 10 11 12 13 14
          13  14 15 16 17 18 19      11 12 13 14 15 16 17      15 16 17 18 19 20 21
         [20] 21 22 23 24 25 26      18 19 20 21 22 23 24      22 23 24 25 26 27 28
          27  28 29 30               25 26 27 28 29 30 31      29 30
         */
    @Test
    fun `Innsending etter fristen, men har fritak hele meldeperioden`() {
        val rettighetsperiode = Periode(20 april 2020, 19 april 2021)
        val input = tomUnderveisInput(
            rettighetsperiode = rettighetsperiode,
            meldepliktGrunnlag = MeldepliktGrunnlag(
                listOf(
                    Fritaksvurdering(
                        harFritak = true,
                        fraDato = 20 april 2020,
                        begrunnelse = "kan ikke",
                        vurdertAv = "saksbehandler",
                        opprettetTid = rettighetsperiode.fom.atStartOfDay(),
                    )
                )
            ),
            innsendingsTidspunkt = mapOf(28 april 2020 to JournalpostId("1")),
        )

        val vurdertTidslinje = vurder(input, nå = 20 april 2021)

        assertVurdering(
            vurdertTidslinje, rettighetsperiode,
            Forventer(
                fom = 20 april 2020,
                tom = 3 mai 2020,
                vurdering = MeldepliktVurdering.FørVedtak,
            ),
            Forventer(
                fom = 4 mai 2020,
                tom = 19 april 2021,
                vurdering = MeldepliktVurdering.Fritak
            )
        )
    }

    /*
                 April                      May                       June
          Mo  Tu We Th Fr  Sa  Su      Mo  Tu  We Th Fr Sa Su      Mo Tu We Th Fr Sa Su
                  1  2  3   4   5                     1  2  3       1  2  3  4  5  6  7

           6   7  8  9 10  11  12       4   5   6  7  8  9 10       8  9 10 11 12 13 14
          13  14 15 16 17  18  19      11 [12] 13 14 15 16 17      15 16 17 18 19 20 21

         [20] 21 22 23 24 [25] 26      18  19  20 21 22 23 24      22 23 24 25 26 27 28
          27  28 29 30                 25  26  27 28 29 30 31      29 30

          søknadsdato 20. april
          vedtaksdato 25. april
          virkningstidspunkt fra 12. mai (meldeperiode 4. – 17. mai)
          ----
          hele meldeperioden (4. – 17. mai) regnes som oppfylt, selv uten meldekort
         */
    @Test
    fun `Meldeplikten inntrer først etter virkningstidspunktet`() {
        val rettighetsperiode = Periode(20 april 2020, 19 april 2021)
        val input = tomUnderveisInput(
            rettighetsperiode = rettighetsperiode,
        )

        val vurdertTidslinje = vurder(
            input, nå = 20 april 2022,
            tidslinjeOf(
                Periode(20 april 2020, 11 mai 2020) to Vurdering(fårAapEtter = null),
                Periode(12 mai 2020, 19 april 2021) to Vurdering(fårAapEtter = BISTANDSBEHOV),
            )
        )

        assertVurdering(
            vurdertTidslinje, rettighetsperiode,
            Forventer(
                fom = 20 april 2020,
                tom = 3 mai 2020,
                vurdering = MeldepliktVurdering.FørVedtak,
            ),
            Forventer(
                fom = 4 mai 2020,
                tom = 17 mai 2020,
                vurdering = MeldepliktVurdering.UtenRett,
            ),
            Forventer(
                fom = 18 mai 2020,
                tom = 19 april 2021,
                vurdering = MeldepliktVurdering.IkkeMeldtSeg,
            )
        )
    }

    /*
                 April                     May                       June
          Mo  Tu We Th Fr  Sa  Su      Mo Tu We Th Fr Sa Su      Mo Tu We Th Fr Sa Su
                  1  2  3   4   5                   1  2  3       1  2  3  4  5  6  7

           6   7  8  9 10  11  12       4  5  6  7  8  9 10       8  9 10 11 12 13 14
          13  14 15 16 17  18  19      11 12 13 14 15 16 17      15 16 17 18 19 20 21

         [20] 21 22 23 24 [25] 26      18 19 20 21 22 23 24      22 23 24 25 26 27 28
          27  28 29 30                 25 26 27 28 29 30 31      29 30

          søknadsdato og vedtaksdato 20. april
          virkningstidspunkt fra 25. april (meldeperiode 20. april – 3. mai)
          ikke rett i meldeperioden fra 4. mai til 17. mai
          rett igjen fra  17. mai
          ----
          første meldeperiode er oppfylt fordi medlemmet anses som meldt på vedtaksdato
          tredje meldeperiode er oppfylt fordi det er første meldeperiode i en ny periode
         */
    @Test
    fun `Anser som meldt seg i overgangen fra ingen rett til å ha rett`() {
        val rettighetsperiode = Periode(20 april 2020, 19 april 2021)
        val input = tomUnderveisInput(
            rettighetsperiode = rettighetsperiode,
        )

        val vurdertTidslinje = vurder(
            input, nå = 20 april 2022,
            tidslinjeOf(
                Periode(20 april 2020, 3 mai 2020) to Vurdering(fårAapEtter = BISTANDSBEHOV),
                Periode(4 mai 2020, 17 mai 2020) to Vurdering(fårAapEtter = null),
                Periode(18 mai 2020, 19 april 2021) to Vurdering(fårAapEtter = BISTANDSBEHOV),
            )
        )

        assertVurdering(
            vurdertTidslinje, rettighetsperiode,
            Forventer(
                fom = 20 april 2020,
                tom = 3 mai 2020,
                vurdering = MeldepliktVurdering.FørVedtak,
            ),
            Forventer(
                fom = 4 mai 2020,
                tom = 17 mai 2020,
                vurdering = MeldepliktVurdering.UtenRett,
            ),
            Forventer(
                fom = 18 mai 2020,
                tom = 31 mai 2020,
                vurdering = MeldepliktVurdering.FørsteMeldeperiodeMedRett,
            ),
            Forventer(
                fom = 1 juni 2020,
                tom = 19 april 2021,
                vurdering = MeldepliktVurdering.IkkeMeldtSeg,
            )
        )
    }

    //Meldeperiode 1: 6-19
    //Meldeperiode 2: 20-2
    //Meldeperiode 3: 3-16
    //Meldeperiode 4: 17-1
    @Test
    fun `Fremtidige meldeperioder er IKKE_OPPFYLT hvis forrige har ført til stans`() {
        val rettighetsperiode = Periode(
            6 januar 2020,
            1 mars 2020,
        )

        val input = tomUnderveisInput(
            rettighetsperiode = rettighetsperiode,
        )

        val vurdertTidslinje = vurder(input, nå = 2 februar 2020)

        assertVurdering(
            vurdertTidslinje, rettighetsperiode,
            Forventer(
                fom = 6 januar 2020, tom = 19 januar 2020,
                vurdering = MeldepliktVurdering.FørVedtak,
            ),
            Forventer(
                fom = 20 januar 2020, tom = 1 februar 2020,
                vurdering = MeldepliktVurdering.IkkeMeldtSeg,
            ),
            Forventer(
                fom = 2 februar 2020, tom = 2 februar 2020,
                vurdering = MeldepliktVurdering.FremtidigIkkeOppfylt,
            ),
            Forventer(
                fom = 3 februar 2020, tom = 16 februar 2020,
                vurdering = MeldepliktVurdering.FremtidigIkkeOppfylt,
            ),
            Forventer(
                fom = 17 februar 2020, tom = 1 mars 2020,
                vurdering = MeldepliktVurdering.FremtidigIkkeOppfylt,
            )
        )
    }

    /*
               April                      May                       June
         Mo Tu We Th Fr Sa Su      Mo Tu We Th Fr Sa Su      Mo Tu We Th Fr Sa Su
                1  2  3  4  5                   1  2  3       1  2  3  4  5  6  7
          6  7  8  9 10 11 12       4  5  6  7  8  9 10       8  9 10 11 12 13 14
         13 14 15 16 17 18 19      11 12 13 14 15 16 17      15 16 17 18 19 20 21
         20 21 22 23 24 25 26      18 19 20 21 22 23 24      22 23 24 25 26 27 28
         27 28 29 30               25 26 27 28 29 30 31      29 30
         */
    @Test
    fun `Innsending siste dagen av fristen, oppfylt for hele meldeperioden`() {
        val rettighetsperiode = Periode(
            20 april 2020,
            19 april 2021,
        )
        val input = tomUnderveisInput(
            rettighetsperiode = rettighetsperiode,
            innsendingsTidspunkt = mapOf(
                11 mai 2020 to JournalpostId("1"),
            ),
        )

        val vurdertTidslinje = vurder(input, nå = 16 mai 2020)

        assertVurdering(
            vurdertTidslinje, rettighetsperiode,
            Forventer(
                fom = rettighetsperiode.fom,
                tom = rettighetsperiode.fom.plusDays(13),
                vurdering = MeldepliktVurdering.FørVedtak,
            ),
            Forventer(
                fom = rettighetsperiode.fom.plusDays(14),
                tom = rettighetsperiode.fom.plusDays(27),
                vurdering = MeldepliktVurdering.MeldtSeg(JournalpostId("1")),
            ),
            Forventer(
                fom = rettighetsperiode.fom.plusDays(28),
                tom = rettighetsperiode.tom,
                vurdering = MeldepliktVurdering.FremtidigOppfylt,
            ),
        )
    }

    /*
               April                      May                       June
         Mo Tu We Th Fr Sa Su      Mo Tu We Th Fr Sa Su      Mo Tu We Th Fr Sa Su
                1  2  3  4  5                   1  2  3       1  2  3  4  5  6  7
          6  7  8  9 10 11 12       4  5  6  7  8  9 10       8  9 10 11 12 13 14
         13 14 15 16 17 18 19      11 12 13 14 15 16 17      15 16 17 18 19 20 21
         20 21 22 23 24 25 26      18 19 20 21 22 23 24      22 23 24 25 26 27 28
         27 28 29 30               25 26 27 28 29 30 31      29 30
         */
    @Test
    fun `Innsending siste dagen av fristen, oppfylt for hele meldeperioden, selv når meldeperioden har passert`() {
        val rettighetsperiode = Periode(
            20 april 2020,
            19 april 2021,
        )
        val input = tomUnderveisInput(
            rettighetsperiode = rettighetsperiode,
            innsendingsTidspunkt = mapOf(
                11 mai 2020 to JournalpostId("1"),
            ),
        )

        val vurdertTidslinje = vurder(input, nå = 18 mai 2020)

        assertVurdering(
            vurdertTidslinje, rettighetsperiode,
            Forventer(
                fom = rettighetsperiode.fom,
                tom = rettighetsperiode.fom.plusDays(13),
                vurdering = MeldepliktVurdering.FørVedtak,
            ),
            Forventer(
                fom = rettighetsperiode.fom.plusDays(14),
                tom = rettighetsperiode.fom.plusDays(27),
                vurdering = MeldepliktVurdering.MeldtSeg(JournalpostId("1")),
            ),
            Forventer(
                fom = rettighetsperiode.fom.plusDays(28),
                tom = rettighetsperiode.tom,
                vurdering = MeldepliktVurdering.FremtidigOppfylt,
            ),
        )
    }

    /*   2020
     *           January                   February                   March
     *   Mo  Tu We Th Fr Sa Su      Mo Tu We Th Fr Sa Su      Mo  Tu We Th Fr  Sa  Su
     *                                              1 [2]                           1
     *   [6]  7  8  9 10 11 12       3  4  5  6  7  8  9       2   3  4  5 [6]  7   8
     *   13  14 15 16 17 18 19      10 11 12 13 14 15 16       9  10 11 12 13  14  15
     *   20  21 22 23 24 25 26      17 18 19 20 21 22 23      16  17 18 19 20  21  22
     *   27  28 29 30 31            24 25 26 27 28 29         23  24 25 26 27  28 [29]
     *                                                       [30] 31
     */
    @Test
    fun `Meldeplikt er oppfylt fra dagen etter frist og ut meldeperioden ved innmelding dagen etter frist`() {
        val rettighetsperiode = Periode(
            6 januar 2020,
            16 februar 2020,
        )

        val input = tomUnderveisInput(
            rettighetsperiode = rettighetsperiode,
            innsendingsTidspunkt = mapOf(
                28 januar 2020 to JournalpostId("1"),
            ),
        )

        val vurdertTidslinje = vurder(input, nå = 17 februar 2020)

        assertVurdering(
            vurdertTidslinje, rettighetsperiode,
            Forventer(
                fom = 6 januar 2020, tom = 19 januar 2020,
                vurdering = MeldepliktVurdering.FørVedtak,
            ),
            Forventer(
                fom = 20 januar 2020, tom = 27 januar 2020,
                vurdering = MeldepliktVurdering.IkkeMeldtSeg,
            ),
            Forventer(
                fom = 28 januar 2020, tom = 2 februar 2020,
                vurdering = MeldepliktVurdering.MeldtSeg(JournalpostId("1")),
            ),
            Forventer(
                fom = 3 februar 2020, tom = 16 februar 2020,
                vurdering = MeldepliktVurdering.IkkeMeldtSeg,
            )
        )
    }

    /*   2020
     *           January                   February                   March
     *   Mo  Tu We Th Fr Sa Su      Mo Tu We Th Fr Sa Su      Mo  Tu We Th Fr  Sa  Su
     *                                              1 [2]                           1
     *   [6]  7  8  9 10 11 12       3  4  5  6  7  8  9       2   3  4  5 [6]  7   8
     *   13  14 15 16 17 18 19      10 11 12 13 14 15 16       9  10 11 12 13  14  15
     *   20  21 22 23 24 25 26      17 18 19 20 21 22 23      16  17 18 19 20  21  22
     *   27  28 29 30 31            24 25 26 27 28 29         23  24 25 26 27  28 [29]
     *                                                       [30] 31
     */
    @Test
    fun `Meldeplikt er oppfylt fra innsending og ut meldeperioden ved innmelding etter frist`() {
        val rettighetsperiode = Periode(
            6 januar 2020,
            16 februar 2020,
        )

        val input = tomUnderveisInput(
            rettighetsperiode = rettighetsperiode,
            innsendingsTidspunkt = mapOf(
                30 januar 2020 to JournalpostId("1"),
            ),
        )

        val vurdertTidslinje = vurder(input, nå = 17 februar 2020)

        assertVurdering(
            vurdertTidslinje, rettighetsperiode,
            Forventer(
                fom = 6 januar 2020, tom = 19 januar 2020,
                vurdering = MeldepliktVurdering.FørVedtak,
            ),
            Forventer(
                fom = 20 januar 2020, tom = 29 januar 2020,
                vurdering = MeldepliktVurdering.IkkeMeldtSeg,
            ),
            Forventer(
                fom = 30 januar 2020, tom = 2 februar 2020,
                vurdering = MeldepliktVurdering.MeldtSeg(JournalpostId("1")),
            ),
            Forventer(
                fom = 3 februar 2020, tom = 16 februar 2020,
                vurdering = MeldepliktVurdering.IkkeMeldtSeg,
            )
        )
    }

    /*
           April                      May                       June
     Mo Tu We Th Fr Sa Su      Mo Tu We Th Fr Sa Su      Mo Tu We Th Fr Sa Su
            1  2  3  4  5                   1  2  3       1  2  3  4  5  6  7
      6  7  8  9 10 11 12       4  5  6  7  8  9 10       8  9 10 11 12 13 14
     13 14 15 16 17 18 19      11 12 13 14 15 16 17      15 16 17 18 19 20 21
     20 21 22 23 24 25 26      18 19 20 21 22 23 24      22 23 24 25 26 27 28
     27 28 29 30               25 26 27 28 29 30 31      29 30
     */
    @Test
    fun `har fritak akkurat en meldeperiode, melder seg den neste`() {
        val rettighetsperiode = Periode(
            20 april 2020,
            31 mai 2020,
        )
        val input = tomUnderveisInput(
            rettighetsperiode = rettighetsperiode,
            meldepliktGrunnlag = MeldepliktGrunnlag(
                listOf(
                    Fritaksvurdering(
                        harFritak = true,
                        fraDato = 4 mai 2020,
                        begrunnelse = "kan ikke",
                        vurdertAv = "saksbehandler",
                        opprettetTid = rettighetsperiode.fom.atStartOfDay(),
                    ),
                    Fritaksvurdering(
                        harFritak = false,
                        fraDato = 18 mai 2020,
                        begrunnelse = "kan",
                        vurdertAv = "saksbehandler",
                        opprettetTid = rettighetsperiode.fom.atStartOfDay(),
                    )
                )
            ),
            innsendingsTidspunkt = mapOf(19 mai 2020 to JournalpostId("1"))
        )

        val vurdertTidslinje = vurder(input, nå = rettighetsperiode.tom.plusDays(1))

        assertVurdering(
            vurdertTidslinje, rettighetsperiode,
            Forventer(
                fom = rettighetsperiode.fom,
                tom = rettighetsperiode.fom.plusDays(13),
                vurdering = MeldepliktVurdering.FørVedtak,
            ),
            Forventer(
                fom = 4 mai 2020,
                tom = 17 mai 2020,
                vurdering = MeldepliktVurdering.Fritak,
            ),
            Forventer(
                fom = 18 mai 2020,
                tom = 31 mai 2020,
                vurdering = MeldepliktVurdering.MeldtSeg(JournalpostId("1")),
            )
        )
    }

    /*
           April                       May                       June
     Mo Tu We Th Fr Sa Su      Mo  Tu We Th Fr Sa  Su      Mo Tu We Th Fr Sa Su
            1  2  3  4  5                    1  2   3        1  2  3  4  5  6  7
      6  7  8  9 10 11 12      [4]  5  6  7  8  9  10        8  9 10 11 12 13 14
     13 14 15 16 17 18 19      11  12 13 14 15 16 [17]      15 16 17 18 19 20 21
     20 21 22 23 24 25 26      18  19 20 21 22 23  24       22 23 24 25 26 27 28
     27 28 29 30               25  26 27 28 29 30  31       29 30
     */
    @Test
    fun `Hvis fritaksperiode == meldeperiode, oppfylt for kun den meldeperioden`() {
        val rettighetsperiode = Periode(
            20 april 2020,
            31 mai 2020,
        )
        val input = tomUnderveisInput(
            rettighetsperiode = rettighetsperiode,
            meldepliktGrunnlag = MeldepliktGrunnlag(
                listOf(
                    Fritaksvurdering(
                        harFritak = true,
                        fraDato = 4 mai 2020,
                        begrunnelse = "kan ikke",
                        vurdertAv = "saksbehandler",
                        opprettetTid = rettighetsperiode.fom.atStartOfDay(),
                    ),
                    Fritaksvurdering(
                        harFritak = false,
                        fraDato = 18 mai 2020,
                        begrunnelse = "kan",
                        vurdertAv = "saksbehandler",
                        opprettetTid = rettighetsperiode.fom.atStartOfDay(),
                    )
                )
            )
        )

        val vurdertTidslinje = vurder(input, nå = rettighetsperiode.tom.plusDays(1))

        assertVurdering(
            vurdertTidslinje, rettighetsperiode,
            Forventer(
                fom = rettighetsperiode.fom,
                tom = rettighetsperiode.fom.plusDays(13),
                vurdering = MeldepliktVurdering.FørVedtak,
            ),
            Forventer(
                fom = 4 mai 2020,
                tom = 17 mai 2020,
                vurdering = MeldepliktVurdering.Fritak,
            ),
            Forventer(
                fom = 18 mai 2020,
                tom = rettighetsperiode.tom,
                vurdering = MeldepliktVurdering.IkkeMeldtSeg,
            ),
        )
    }

    /*
           April                       May                       June
     Mo Tu We Th Fr Sa Su      Mo  Tu We Th Fr Sa  Su      Mo Tu We Th Fr Sa Su
            1  2  3  4  5                    1  2   3        1  2  3  4  5  6  7
      6  7  8  9 10 11 12      [4]  5  6  7  8  9  10        8  9 10 11 12 13 14
     13 14 15 16 17 18 19      11  12 13 14 15 16 [17]      15 16 17 18 19 20 21
     20 21 22 23 24 25 26      18  19 20 21 22 23  24       22 23 24 25 26 27 28
     27 28 29 30               25  26 27 28 29 30  31       29 30
     */
    @Test
    fun `Meldefrist er etter rettighetsperioden`() {
        val rettighetsperiode = Periode(
            20 april 2020,
            20 mai 2020,
        )
        val input = tomUnderveisInput(
            rettighetsperiode = rettighetsperiode,
            innsendingsTidspunkt = mapOf(22 mai 2020 to JournalpostId("1"))
        )

        val vurdertTidslinje = vurder(input, nå = 23 mai 2020)

        assertVurdering(
            vurdertTidslinje, rettighetsperiode,
            Forventer(
                fom = rettighetsperiode.fom,
                tom = rettighetsperiode.fom.plusDays(13),
                vurdering = MeldepliktVurdering.FørVedtak,
            ),
            Forventer(
                fom = 4 mai 2020,
                tom = 17 mai 2020,
                vurdering = MeldepliktVurdering.IkkeMeldtSeg,
            ),
            Forventer(
                fom = 18 mai 2020,
                tom = 20 mai 2020,
                vurdering = MeldepliktVurdering.MeldtSeg(JournalpostId("1"))
            ),
        )
    }

    /*
          April                      May                       June
    Mo Tu We Th Fr Sa Su      Mo Tu We Th Fr Sa Su      Mo Tu We Th Fr Sa Su
           1  2  3  4  5                   1  2  3       1  2  3  4  5  6  7
     6  7  8  9 10 11 12       4  5  6  7  8  9 10       8  9 10 11 12 13 14
    13 14 15 16 17 18 19      11 12 13 14 15 16 17      15 16 17 18 19 20 21
    20 21 22 23 24 25 26      18 19 20 21 22 23 24      22 23 24 25 26 27 28
    27 28 29 30               25 26 27 28 29 30 31      29 30
    */
    @Test
    fun `får fritak etter meldefrist uten å ha meldt seg, får oppfylt fra fritaksdag og ut`() {
        val rettighetsperiode = Periode(
            20 april 2020,
            17 mai 2020,
        )
        val input = tomUnderveisInput(
            rettighetsperiode = rettighetsperiode,
            meldepliktGrunnlag = MeldepliktGrunnlag(
                listOf(
                    Fritaksvurdering(
                        harFritak = true,
                        fraDato = 14 mai 2020,
                        begrunnelse = "kan ikke",
                        vurdertAv = "saksbehandler",
                        opprettetTid = rettighetsperiode.fom.atStartOfDay(),
                    ),
                    Fritaksvurdering(
                        harFritak = false,
                        fraDato = 15 mai 2020,
                        begrunnelse = "kan",
                        vurdertAv = "saksbehandler",
                        opprettetTid = rettighetsperiode.fom.atStartOfDay(),
                    )
                )
            )
        )

        val vurdertTidslinje = vurder(input, nå = rettighetsperiode.tom.plusDays(1))

        assertVurdering(
            vurdertTidslinje, rettighetsperiode,
            Forventer(
                fom = rettighetsperiode.fom,
                tom = rettighetsperiode.fom.plusDays(13),
                vurdering = MeldepliktVurdering.FørVedtak,
            ),
            Forventer(
                fom = 4 mai 2020,
                tom = 13 mai 2020,
                vurdering = MeldepliktVurdering.IkkeMeldtSeg,
            ),
            Forventer(
                fom = 14 mai 2020,
                tom = 17 mai 2020,
                vurdering = MeldepliktVurdering.Fritak,
            )
        )
    }

    /*
           April                      May                       June
     Mo Tu We Th Fr Sa Su      Mo Tu We Th Fr Sa Su      Mo Tu We Th Fr Sa Su
            1  2  3  4  5                   1  2  3       1  2  3  4  5  6  7
      6  7  8  9 10 11 12       4  5  6  7  8  9 10       8  9 10 11 12 13 14
     13 14 15 16 17 18 19      11 12 13 14 15 16 17      15 16 17 18 19 20 21
     20 21 22 23 24 25 26      18 19 20 21 22 23 24      22 23 24 25 26 27 28
     27 28 29 30               25 26 27 28 29 30 31      29 30
     */
    @Test
    fun `frist har passert, men dags dato er fortsatt i meldeperioden`() {
        val rettighetsperiode = Periode(
            20 april 2020,
            17 mai 2020,
        )
        val input = tomUnderveisInput(
            rettighetsperiode = rettighetsperiode,
        )

        val vurdertTidslinje = vurder(input, nå = 15 mai 2020)

        assertVurdering(
            vurdertTidslinje, rettighetsperiode,
            Forventer(
                fom = rettighetsperiode.fom,
                tom = rettighetsperiode.fom.plusDays(13),
                vurdering = MeldepliktVurdering.FørVedtak,
            ),
            Forventer(
                fom = 4 mai 2020,
                tom = 14 mai 2020,
                vurdering = MeldepliktVurdering.IkkeMeldtSeg,
            ),
            Forventer(
                fom = 15 mai 2020,
                tom = 17 mai 2020,
                vurdering = MeldepliktVurdering.FremtidigIkkeOppfylt,
            )
        )
    }

    /*
           April                      May                       June
     Mo Tu We Th Fr Sa Su      Mo Tu We Th Fr Sa Su      Mo Tu We Th Fr Sa Su
            1  2  3  4  5                   1  2  3       1  2  3  4  5  6  7
      6  7  8  9 10 11 12       4  5  6  7  8  9 10       8  9 10 11 12 13 14
     13 14 15 16 17 18 19      11 12 13 14 15 16 17      15 16 17 18 19 20 21
     20 21 22 23 24 25 26      18 19 20 21 22 23 24      22 23 24 25 26 27 28
     27 28 29 30               25 26 27 28 29 30 31      29 30
     */
    @Test
    fun `frist har ikke passert, forrige meldeperiode er oppfylt`() {
        val rettighetsperiode = Periode(20 april 2020, 17 mai 2020)
        val input = tomUnderveisInput(
            rettighetsperiode = rettighetsperiode,
        )

        val vurdertTidslinje = vurder(input, nå = 7 mai 2020)

        assertVurdering(
            vurdertTidslinje, rettighetsperiode,
            Forventer(
                fom = 20 april 2020,
                tom = 3 mai 2020,
                vurdering = MeldepliktVurdering.FørVedtak,
            ),
            Forventer(
                fom = 4 mai 2020,
                tom = 17 mai 2020,
                vurdering = MeldepliktVurdering.FremtidigOppfylt,
            )
        )
    }

    /*
           April                      May                       June
     Mo Tu We Th Fr Sa Su      Mo Tu We Th Fr Sa Su      Mo Tu We Th Fr Sa Su
            1  2  3  4  5                   1  2  3       1  2  3  4  5  6  7
      6  7  8  9 10 11 12       4  5  6  7  8  9 10       8  9 10 11 12 13 14
     13 14 15 16 17 18 19      11 12 13 14 15 16 17      15 16 17 18 19 20 21
     20 21 22 23 24 25 26      18 19 20 21 22 23 24      22 23 24 25 26 27 28
     27 28 29 30               25 26 27 28 29 30 31      29 30
     */
    @Test
    fun `frist har ikke passert, forrige meldeperiode er ikke oppfylt`() {
        val rettighetsperiode = Periode(
            20 april 2020,
            31 mai 2020,
        )
        val input = tomUnderveisInput(
            rettighetsperiode = rettighetsperiode,
        )

        val vurdertTidslinje = vurder(input, nå = 20 mai 2020)

        assertVurdering(
            vurdertTidslinje, rettighetsperiode,
            Forventer(
                fom = rettighetsperiode.fom,
                tom = rettighetsperiode.fom.plusDays(13),
                vurdering = MeldepliktVurdering.FørVedtak,
            ),
            Forventer(
                fom = 4 mai 2020,
                tom = 17 mai 2020,
                vurdering = MeldepliktVurdering.IkkeMeldtSeg,
            ),
            Forventer(
                fom = 18 mai 2020,
                tom = 31 mai 2020,
                vurdering = MeldepliktVurdering.FremtidigIkkeOppfylt,
            )
        )
    }

    /*
          April                      May                       June
    Mo Tu We Th Fr Sa Su      Mo Tu We Th Fr Sa Su      Mo Tu We Th Fr Sa Su
           1  2  3  4  5                   1  2  3       1  2  3  4  5  6  7
     6  7  8  9 10 11 12       4  5  6  7  8  9 10       8  9 10 11 12 13 14
    13 14 15 16 17 18 19      11 12 13 14 15 16 17      15 16 17 18 19 20 21
    20 21 22 23 24 25 26      18 19 20 21 22 23 24      22 23 24 25 26 27 28
    27 28 29 30               25 26 27 28 29 30 31      29 30
    */
    @Test
    fun `frist har ikke passert, forrige meldeperiode er ikke oppfylt, fritak etter fristen`() {
        val rettighetsperiode = Periode(
            20 april 2020,
            31 mai 2020,
        )
        val input = tomUnderveisInput(
            rettighetsperiode = rettighetsperiode,
            meldepliktGrunnlag = MeldepliktGrunnlag(
                listOf(
                    Fritaksvurdering(
                        harFritak = true,
                        fraDato = 28 mai 2020,
                        begrunnelse = "kan ikke",
                        vurdertAv = "saksbehandler",
                        opprettetTid = LocalDateTime.now()
                    )
                )
            )
        )

        val vurdertTidslinje = vurder(input, nå = 20 mai 2020)

        assertVurdering(
            vurdertTidslinje, rettighetsperiode,
            Forventer(
                fom = rettighetsperiode.fom,
                tom = rettighetsperiode.fom.plusDays(13),
                vurdering = MeldepliktVurdering.FørVedtak,
            ),
            Forventer(
                fom = 4 mai 2020,
                tom = 17 mai 2020,
                vurdering = MeldepliktVurdering.IkkeMeldtSeg,
            ),
            Forventer(
                fom = 18 mai 2020,
                tom = 27 mai 2020,
                vurdering = MeldepliktVurdering.FremtidigIkkeOppfylt,
            ),
            Forventer(
                fom = 28 mai 2020,
                tom = 31 mai 2020,
                vurdering = MeldepliktVurdering.Fritak,
            )
        )
    }

    /*
      April                      May                       June
Mo Tu We Th Fr Sa Su      Mo Tu We Th Fr Sa Su      Mo Tu We Th Fr Sa Su
       1  2  3  4  5                   1  2  3       1  2  3  4  5  6  7
 6  7  8  9 10 11 12       4  5  6  7  8  9 10       8  9 10 11 12 13 14
13 14 15 16 17 18 19      11 12 13 14 15 16 17      15 16 17 18 19 20 21
20 21 22 23 24 25 26      18 19 20 21 22 23 24      22 23 24 25 26 27 28
27 28 29 30               25 26 27 28 29 30 31      29 30
*/
    @Test
    fun `får oppfylt på rimelig grunn etter meldefrist uten å ha meldt seg`() {
        val rettighetsperiode = Periode(
            20 april 2020,
            14 juni 2020,
        )
        val input = tomUnderveisInput(
            rettighetsperiode = rettighetsperiode,
            overstyringMeldepliktGrunnlag = OverstyringMeldepliktGrunnlag(
                listOf(
                    OverstyringMeldepliktVurdering(
                        perioder = listOf(
                            OverstyringMeldepliktVurderingPeriode(
                                fom = 4 mai 2020,
                                tom = 17 mai 2020,
                                begrunnelse = "kan ikke",
                                meldepliktOverstyringStatus = MeldepliktOverstyringStatus.RIMELIG_GRUNN
                            )
                        ),
                        vurdertAv = "saksbehandler",
                        vurdertIBehandling = BehandlingReferanse(),
                        opprettetTid = rettighetsperiode.fom.atStartOfDay(),
                    )
                )
            )
        )

        val vurdertTidslinje = vurder(input, nå = 2 juni 2020)

        assertVurdering(
            vurdertTidslinje, rettighetsperiode,
            Forventer(
                fom = rettighetsperiode.fom,
                tom = rettighetsperiode.fom.plusDays(13),
                vurdering = MeldepliktVurdering.FørVedtak,
            ),
            Forventer(
                fom = 4 mai 2020,
                tom = 17 mai 2020,
                vurdering = MeldepliktVurdering.RimeligGrunnOverstyring,
            ),
            Forventer(
                fom = 18 mai 2020,
                tom = 31 mai 2020,
                vurdering = MeldepliktVurdering.IkkeMeldtSeg,
            ),
            Forventer(
                fom = 1 juni 2020,
                tom = 14 juni 2020,
                vurdering = MeldepliktVurdering.FremtidigIkkeOppfylt,
            )
        )
    }

    private fun vurder(
        input: UnderveisInput,
        nå: LocalDate,
        vurderinger: Tidslinje<Vurdering> = Tidslinje(
            input.periodeForVurdering,
            Vurdering(fårAapEtter = BISTANDSBEHOV)
        ),
    ): Tidslinje<Vurdering> {
        val zone = ZoneId.systemDefault()
        val now = nå.atStartOfDay(zone).toInstant()
        return MeldepliktRegel(clock = Clock.fixed(now, zone))
            .vurder(input, UtledMeldeperiodeRegel().vurder(input, vurderinger))
    }

    private fun assertVurdering(
        vurdertTidslinje: Tidslinje<Vurdering>,
        rettighetsperiode: Periode,
        vararg forventedeVurderinger: Forventer,
    ) {
        val forventetTidslinje = forventedeVurderinger
            .map { Segment(Periode(it.fom, it.tom), it) }
            .let { Tidslinje(it) }

        assertThat(vurdertTidslinje.erSammenhengende()).isTrue()
        assertThat(vurdertTidslinje.helePerioden()).isEqualTo(rettighetsperiode)
        assertThat(forventetTidslinje.helePerioden()).isEqualTo(rettighetsperiode)
            .`as`("Skal asserte på hele perioden.")

        vurdertTidslinje.kombiner<_, Nothing>(
            forventetTidslinje,
            JoinStyle.RIGHT_JOIN { periode, vurdering, forventet ->
                assertThat(vurdering?.verdi?.meldepliktVurdering)
                    .`as`("for periode $periode")
                    .isEqualTo(forventet.verdi.vurdering)
                null
            })
    }
}