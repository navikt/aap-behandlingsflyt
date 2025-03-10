package no.nav.aap.behandlingsflyt.behandling.underveis.regler

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisÅrsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisÅrsak.IKKE_OVERHOLDT_MELDEPLIKT_SANKSJON
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisÅrsak.MELDEPLIKT_FRIST_IKKE_PASSERT
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall.IKKE_OPPFYLT
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall.OPPFYLT
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.Fritaksvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.MeldepliktGrunnlag
import no.nav.aap.komponenter.tidslinje.JoinStyle
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.verdityper.dokument.JournalpostId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Month.APRIL
import java.time.Month.FEBRUARY
import java.time.Month.JANUARY
import java.time.Month.MARCH
import java.time.Month.MAY
import java.time.ZoneId


class MeldepliktRegelTest {
    private data class Forventer(
        val fom: LocalDate,
        val tom: LocalDate,
        val utfall: Utfall,
        val årsak: UnderveisÅrsak? = null,
        val dokument: MeldepliktRegel.Dokument? = null,
    )

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
            LocalDate.of(2020, JANUARY, 6),
            LocalDate.of(2020, MARCH, 1)
        )
        val input = tomUnderveisInput(
            rettighetsperiode = rettighetsperiode,
            innsendingsTidspunkt = mapOf(
                LocalDate.of(2020, JANUARY, 20) to JournalpostId("1"),
                LocalDate.of(2020, FEBRUARY, 3) to JournalpostId("2"),
                LocalDate.of(2020, FEBRUARY, 17) to JournalpostId("3"),
            ),
        )

        val vurdertTidslinje = vurder(input, nå = rettighetsperiode.tom.plusDays(1))

        assertVurdering(
            vurdertTidslinje, rettighetsperiode,
            Forventer(
                fom = LocalDate.of(2020, JANUARY, 6),
                tom = LocalDate.of(2020, JANUARY, 19),
                utfall = OPPFYLT,
            ),
            Forventer(
                fom = LocalDate.of(2020, JANUARY, 20),
                tom = LocalDate.of(2020, FEBRUARY, 2),
                dokument = MeldepliktRegel.Meldt(JournalpostId("1")),
                utfall = OPPFYLT,
            ),
            Forventer(
                fom = LocalDate.of(2020, FEBRUARY, 3),
                tom = LocalDate.of(2020, FEBRUARY, 16),
                dokument = MeldepliktRegel.Meldt(JournalpostId("2")),
                utfall = OPPFYLT,
            ),
            Forventer(
                fom = LocalDate.of(2020, FEBRUARY, 17),
                tom = LocalDate.of(2020, MARCH, 1),
                dokument = MeldepliktRegel.Meldt(JournalpostId("3")),
                utfall = OPPFYLT,
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
    fun `Skal starte med full utbetalingsplan`() {
        val rettighetsperiode = Periode(
            LocalDate.of(2020, APRIL, 20),
            LocalDate.of(2021, APRIL, 19),
        )
        val input = tomUnderveisInput(
            rettighetsperiode = rettighetsperiode,
        )

        val vurdertTidslinje = vurder(input, nå = rettighetsperiode.fom)

        assertVurdering(
            vurdertTidslinje, rettighetsperiode,
            Forventer(
                fom = rettighetsperiode.fom,
                tom = rettighetsperiode.tom,
                utfall = OPPFYLT,
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
    fun `Meldeplikt skal være stanset etter at man ikke har meldt seg og fristen utløpt`() {
        val rettighetsperiode = Periode(
            LocalDate.of(2020, JANUARY, 6),
            LocalDate.of(2020, MARCH, 29),
        )

        val input = tomUnderveisInput(
            rettighetsperiode = rettighetsperiode,
            innsendingsTidspunkt = mapOf(
                LocalDate.of(2020, FEBRUARY, 2) to JournalpostId("1"),
                LocalDate.of(2020, MARCH, 6) to JournalpostId("2")
            ),
        )

        val vurdertTidslinje = vurder(input, nå = rettighetsperiode.tom.plusDays(1))

        assertVurdering(
            vurdertTidslinje, rettighetsperiode,
            Forventer(
                fom = LocalDate.of(2020, JANUARY, 6), tom = LocalDate.of(2020, JANUARY, 19),
                utfall = OPPFYLT,
            ),
            Forventer(
                fom = LocalDate.of(2020, JANUARY, 20), tom = LocalDate.of(2020, FEBRUARY, 1),
                utfall = IKKE_OPPFYLT,
                årsak = IKKE_OVERHOLDT_MELDEPLIKT_SANKSJON,
            ),
            Forventer(
                fom = LocalDate.of(2020, FEBRUARY, 2), tom = LocalDate.of(2020, FEBRUARY, 2),
                dokument = MeldepliktRegel.Meldt(JournalpostId("1")),
                utfall = OPPFYLT,
            ),
            Forventer(
                fom = LocalDate.of(2020, FEBRUARY, 3), tom = LocalDate.of(2020, FEBRUARY, 16),
                utfall = IKKE_OPPFYLT,
                årsak = IKKE_OVERHOLDT_MELDEPLIKT_SANKSJON,
            ),
            Forventer(
                fom = LocalDate.of(2020, FEBRUARY, 17), tom = LocalDate.of(2020, MARCH, 1),
                utfall = IKKE_OPPFYLT,
                årsak = IKKE_OVERHOLDT_MELDEPLIKT_SANKSJON,
            ),
            Forventer(
                fom = LocalDate.of(2020, MARCH, 2), tom = LocalDate.of(2020, MARCH, 15),
                dokument = MeldepliktRegel.Meldt(JournalpostId("2")),
                utfall = OPPFYLT,
            ),
            Forventer(
                fom = LocalDate.of(2020, MARCH, 16), tom = LocalDate.of(2020, MARCH, 29),
                utfall = IKKE_OPPFYLT,
                årsak = IKKE_OVERHOLDT_MELDEPLIKT_SANKSJON,
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
            LocalDate.of(2020, APRIL, 20),
            LocalDate.of(2021, APRIL, 19),
        )
        val input = tomUnderveisInput(
            rettighetsperiode = rettighetsperiode,
            meldepliktGrunnlag = MeldepliktGrunnlag(listOf(
                Fritaksvurdering(
                    harFritak = true,
                    fraDato = rettighetsperiode.fom,
                    begrunnelse = "kan ikke",
                    opprettetTid = rettighetsperiode.fom.atStartOfDay(),
                )
            ))
        )

        val vurdertTidslinje = vurder(input, nå = rettighetsperiode.tom.plusDays(1))

        assertVurdering(
            vurdertTidslinje, rettighetsperiode,
            Forventer(
                fom = rettighetsperiode.fom,
                tom = rettighetsperiode.fom.plusDays(13),
                utfall = OPPFYLT,
            ),
            Forventer(
                fom = rettighetsperiode.fom.plusDays(14),
                tom = rettighetsperiode.tom,
                utfall = OPPFYLT,
                dokument = MeldepliktRegel.Fritak
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
        val rettighetsperiode = Periode(
            LocalDate.of(2020, APRIL, 20),
            LocalDate.of(2021, APRIL, 19),
        )
        val input = tomUnderveisInput(
            rettighetsperiode = rettighetsperiode,
            meldepliktGrunnlag = MeldepliktGrunnlag(listOf(
                Fritaksvurdering(
                    harFritak = true,
                    fraDato = rettighetsperiode.fom,
                    begrunnelse = "kan ikke",
                    opprettetTid = rettighetsperiode.fom.atStartOfDay(),
                )
            )),
            innsendingsTidspunkt = mapOf(
                LocalDate.of(2020, APRIL, 28) to JournalpostId("1"),
            ),
        )

        val vurdertTidslinje = vurder(input, nå = rettighetsperiode.tom.plusDays(1))

        assertVurdering(
            vurdertTidslinje, rettighetsperiode,
            Forventer(
                fom = rettighetsperiode.fom,
                tom = rettighetsperiode.fom.plusDays(13),
                utfall = OPPFYLT,
            ),
            Forventer(
                fom = rettighetsperiode.fom.plusDays(14),
                tom = rettighetsperiode.tom,
                utfall = OPPFYLT,
                dokument = MeldepliktRegel.Fritak
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
            LocalDate.of(2020, JANUARY, 6),
            LocalDate.of(2020, MARCH, 1),
        )

        val input = tomUnderveisInput(
            rettighetsperiode = rettighetsperiode,
        )

        val vurdertTidslinje = vurder(input, nå = LocalDate.of(2020, FEBRUARY, 2))

        assertVurdering(
            vurdertTidslinje, rettighetsperiode,
            Forventer(
                fom = LocalDate.of(2020, JANUARY, 6), tom = LocalDate.of(2020, JANUARY, 19),
                utfall = OPPFYLT,
            ),
            Forventer(
                fom = LocalDate.of(2020, JANUARY, 20), tom = LocalDate.of(2020, FEBRUARY, 1),
                utfall = IKKE_OPPFYLT,
                årsak = IKKE_OVERHOLDT_MELDEPLIKT_SANKSJON,
            ),
            Forventer(
                fom = LocalDate.of(2020, FEBRUARY, 2), tom = LocalDate.of(2020, FEBRUARY, 2),
                utfall = IKKE_OPPFYLT,
                årsak = MELDEPLIKT_FRIST_IKKE_PASSERT,
            ),
            Forventer(
                fom = LocalDate.of(2020, FEBRUARY, 3), tom = LocalDate.of(2020, FEBRUARY, 16),
                utfall = IKKE_OPPFYLT,
                årsak = MELDEPLIKT_FRIST_IKKE_PASSERT
            ),
            Forventer(
                fom = LocalDate.of(2020, FEBRUARY, 17), tom = LocalDate.of(2020, MARCH, 1),
                utfall = IKKE_OPPFYLT,
                årsak = MELDEPLIKT_FRIST_IKKE_PASSERT,
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
            LocalDate.of(2020, APRIL, 20),
            LocalDate.of(2021, APRIL, 19),
        )
        val input = tomUnderveisInput(
            rettighetsperiode = rettighetsperiode,
            innsendingsTidspunkt = mapOf(
                LocalDate.of(2020, MAY, 11) to JournalpostId("1"),
            ),
        )

        val vurdertTidslinje = vurder(input, nå = LocalDate.of(2020, MAY, 16))

        assertVurdering(
            vurdertTidslinje, rettighetsperiode,
            Forventer(
                fom = rettighetsperiode.fom,
                tom = rettighetsperiode.fom.plusDays(13),
                utfall = OPPFYLT,
            ),
            Forventer(
                dokument = MeldepliktRegel.Meldt(JournalpostId("1")),
                fom = rettighetsperiode.fom.plusDays(14),
                tom = rettighetsperiode.fom.plusDays(27),
                utfall = OPPFYLT,
            ),
            Forventer(
                fom = rettighetsperiode.fom.plusDays(28),
                tom = rettighetsperiode.tom,
                utfall = OPPFYLT,
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
            LocalDate.of(2020, APRIL, 20),
            LocalDate.of(2021, APRIL, 19),
        )
        val input = tomUnderveisInput(
            rettighetsperiode = rettighetsperiode,
            innsendingsTidspunkt = mapOf(
                LocalDate.of(2020, MAY, 11) to JournalpostId("1"),
            ),
        )

        val vurdertTidslinje = vurder(input, nå = LocalDate.of(2020, MAY, 18))

        assertVurdering(
            vurdertTidslinje, rettighetsperiode,
            Forventer(
                fom = rettighetsperiode.fom,
                tom = rettighetsperiode.fom.plusDays(13),
                utfall = OPPFYLT,
            ),
            Forventer(
                dokument = MeldepliktRegel.Meldt(JournalpostId("1")),
                fom = rettighetsperiode.fom.plusDays(14),
                tom = rettighetsperiode.fom.plusDays(27),
                utfall = OPPFYLT,
            ),
            Forventer(
                fom = rettighetsperiode.fom.plusDays(28),
                tom = rettighetsperiode.tom,
                utfall = OPPFYLT,
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
            LocalDate.of(2020, JANUARY, 6),
            LocalDate.of(2020, FEBRUARY, 16),
        )

        val input = tomUnderveisInput(
            rettighetsperiode = rettighetsperiode,
            innsendingsTidspunkt = mapOf(
                LocalDate.of(2020, JANUARY, 28) to JournalpostId("1"),
            ),
        )

        val vurdertTidslinje = vurder(input, nå = LocalDate.of(2020, FEBRUARY, 17))

        assertVurdering(
            vurdertTidslinje, rettighetsperiode,
            Forventer(
                fom = LocalDate.of(2020, JANUARY, 6), tom = LocalDate.of(2020, JANUARY, 19),
                utfall = OPPFYLT,
            ),
            Forventer(
                fom = LocalDate.of(2020, JANUARY, 20), tom = LocalDate.of(2020, JANUARY, 27),
                utfall = IKKE_OPPFYLT,
                årsak = IKKE_OVERHOLDT_MELDEPLIKT_SANKSJON,
            ),
            Forventer(
                fom = LocalDate.of(2020, JANUARY, 28), tom = LocalDate.of(2020, FEBRUARY, 2),
                dokument = MeldepliktRegel.Meldt(JournalpostId("1")),
                utfall = OPPFYLT,
            ),
            Forventer(
                fom = LocalDate.of(2020, FEBRUARY, 3), tom = LocalDate.of(2020, FEBRUARY, 16),
                utfall = IKKE_OPPFYLT,
                årsak = IKKE_OVERHOLDT_MELDEPLIKT_SANKSJON,
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
            LocalDate.of(2020, JANUARY, 6),
            LocalDate.of(2020, FEBRUARY, 16),
        )

        val input = tomUnderveisInput(
            rettighetsperiode = rettighetsperiode,
            innsendingsTidspunkt = mapOf(
                LocalDate.of(2020, JANUARY, 30) to JournalpostId("1"),
            ),
        )

        val vurdertTidslinje = vurder(input, nå = LocalDate.of(2020, FEBRUARY, 17))

        assertVurdering(
            vurdertTidslinje, rettighetsperiode,
            Forventer(
                fom = LocalDate.of(2020, JANUARY, 6), tom = LocalDate.of(2020, JANUARY, 19),
                utfall = OPPFYLT,
            ),
            Forventer(
                fom = LocalDate.of(2020, JANUARY, 20), tom = LocalDate.of(2020, JANUARY, 29),
                utfall = IKKE_OPPFYLT,
                årsak = IKKE_OVERHOLDT_MELDEPLIKT_SANKSJON,
            ),
            Forventer(
                fom = LocalDate.of(2020, JANUARY, 30), tom = LocalDate.of(2020, FEBRUARY, 2),
                dokument = MeldepliktRegel.Meldt(JournalpostId("1")),
                utfall = OPPFYLT,
            ),
            Forventer(
                fom = LocalDate.of(2020, FEBRUARY, 3), tom = LocalDate.of(2020, FEBRUARY, 16),
                utfall = IKKE_OPPFYLT,
                årsak = IKKE_OVERHOLDT_MELDEPLIKT_SANKSJON,
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
            LocalDate.of(2020, APRIL, 20),
            LocalDate.of(2020, MAY, 31),
        )
        val input = tomUnderveisInput(
            rettighetsperiode = rettighetsperiode,
            meldepliktGrunnlag = MeldepliktGrunnlag(listOf(
                Fritaksvurdering(
                    harFritak = true,
                    fraDato = LocalDate.of(2020, MAY, 4),
                    begrunnelse = "kan ikke",
                    opprettetTid = rettighetsperiode.fom.atStartOfDay(),
                ),
                Fritaksvurdering(
                    harFritak = false,
                    fraDato = LocalDate.of(2020, MAY, 18),
                    begrunnelse = "kan",
                    opprettetTid = rettighetsperiode.fom.atStartOfDay(),
                )
            )),
            innsendingsTidspunkt = mapOf(LocalDate.of(2020, MAY, 19) to JournalpostId("1"))
        )

        val vurdertTidslinje = vurder(input, nå = rettighetsperiode.tom.plusDays(1))

        assertVurdering(
            vurdertTidslinje, rettighetsperiode,
            Forventer(
                fom = rettighetsperiode.fom,
                tom = rettighetsperiode.fom.plusDays(13),
                utfall = OPPFYLT,
            ),
            Forventer(
                fom = LocalDate.of(2020, MAY, 4),
                tom = LocalDate.of(2020, MAY, 17),
                utfall = OPPFYLT,
                dokument = MeldepliktRegel.Fritak
            ),
            Forventer(
                dokument = MeldepliktRegel.Meldt(JournalpostId("1")),
                fom = LocalDate.of(2020, MAY, 18),
                tom = LocalDate.of(2020, MAY, 31),
                utfall = OPPFYLT,
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
            LocalDate.of(2020, APRIL, 20),
            LocalDate.of(2020, MAY, 31),
        )
        val input = tomUnderveisInput(
            rettighetsperiode = rettighetsperiode,
            meldepliktGrunnlag = MeldepliktGrunnlag(listOf(
                Fritaksvurdering(
                    harFritak = true,
                    fraDato = LocalDate.of(2020, MAY, 4),
                    begrunnelse = "kan ikke",
                    opprettetTid = rettighetsperiode.fom.atStartOfDay(),
                ),
                Fritaksvurdering(
                    harFritak = false,
                    fraDato = LocalDate.of(2020, MAY, 18),
                    begrunnelse = "kan",
                    opprettetTid = rettighetsperiode.fom.atStartOfDay(),
                )
            ))
        )

        val vurdertTidslinje = vurder(input, nå = rettighetsperiode.tom.plusDays(1))

        assertVurdering(
            vurdertTidslinje, rettighetsperiode,
            Forventer(
                fom = rettighetsperiode.fom,
                tom = rettighetsperiode.fom.plusDays(13),
                utfall = OPPFYLT,
            ),
            Forventer(
                fom = LocalDate.of(2020, MAY, 4),
                tom = LocalDate.of(2020, MAY, 17),
                utfall = OPPFYLT,
                dokument = MeldepliktRegel.Fritak
            ),
            Forventer(
                fom = LocalDate.of(2020, MAY, 18),
                tom = rettighetsperiode.tom,
                utfall = IKKE_OPPFYLT,
                årsak = IKKE_OVERHOLDT_MELDEPLIKT_SANKSJON,
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
            LocalDate.of(2020, APRIL, 20),
            LocalDate.of(2020, MAY, 20),
        )
        val input = tomUnderveisInput(
            rettighetsperiode = rettighetsperiode,
            innsendingsTidspunkt = mapOf(LocalDate.of(2020, MAY, 22) to JournalpostId("1"))
        )

        val vurdertTidslinje = vurder(input, nå = LocalDate.of(2020, MAY, 23))

        assertVurdering(
            vurdertTidslinje, rettighetsperiode,
            Forventer(
                fom = rettighetsperiode.fom,
                tom = rettighetsperiode.fom.plusDays(13),
                utfall = OPPFYLT,
            ),
            Forventer(
                fom = LocalDate.of(2020, MAY, 4),
                tom = LocalDate.of(2020, MAY, 17),
                utfall = IKKE_OPPFYLT,
                årsak = IKKE_OVERHOLDT_MELDEPLIKT_SANKSJON
            ),
            Forventer(
                fom = LocalDate.of(2020, MAY, 18),
                tom = LocalDate.of(2020, MAY, 20),
                utfall = OPPFYLT,
                dokument = MeldepliktRegel.Meldt(JournalpostId("1"))
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
            LocalDate.of(2020, APRIL, 20),
            LocalDate.of(2020, MAY, 17),
        )
        val input = tomUnderveisInput(
            rettighetsperiode = rettighetsperiode,
            meldepliktGrunnlag = MeldepliktGrunnlag(listOf(
                Fritaksvurdering(
                    harFritak = true,
                    fraDato = LocalDate.of(2020, MAY, 14),
                    begrunnelse = "kan ikke",
                    opprettetTid = rettighetsperiode.fom.atStartOfDay(),
                ),
                Fritaksvurdering(
                    harFritak = false,
                    fraDato = LocalDate.of(2020, MAY, 15),
                    begrunnelse = "kan",
                    opprettetTid = rettighetsperiode.fom.atStartOfDay(),
                )
            ))
        )

        val vurdertTidslinje = vurder(input, nå = rettighetsperiode.tom.plusDays(1))

        assertVurdering(
            vurdertTidslinje, rettighetsperiode,
            Forventer(
                fom = rettighetsperiode.fom,
                tom = rettighetsperiode.fom.plusDays(13),
                utfall = OPPFYLT,
            ),
            Forventer(
                fom = LocalDate.of(2020, MAY, 4),
                tom = LocalDate.of(2020, MAY, 13),
                utfall = IKKE_OPPFYLT,
                årsak = IKKE_OVERHOLDT_MELDEPLIKT_SANKSJON,
            ),
            Forventer(
                dokument = MeldepliktRegel.Fritak,
                fom = LocalDate.of(2020, MAY, 14),
                tom = LocalDate.of(2020, MAY, 17),
                utfall = OPPFYLT,
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
            LocalDate.of(2020, APRIL, 20),
            LocalDate.of(2020, MAY, 17),
        )
        val input = tomUnderveisInput(
            rettighetsperiode = rettighetsperiode,
        )

        val vurdertTidslinje = vurder(input, nå = LocalDate.of(2020, MAY, 15))

        assertVurdering(
            vurdertTidslinje, rettighetsperiode,
            Forventer(
                fom = rettighetsperiode.fom,
                tom = rettighetsperiode.fom.plusDays(13),
                utfall = OPPFYLT,
            ),
            Forventer(
                fom = LocalDate.of(2020, MAY, 4),
                tom = LocalDate.of(2020, MAY, 14),
                utfall = IKKE_OPPFYLT,
                årsak = IKKE_OVERHOLDT_MELDEPLIKT_SANKSJON,
            ),
            Forventer(
                fom = LocalDate.of(2020, MAY, 15),
                tom = LocalDate.of(2020, MAY, 17),
                utfall = IKKE_OPPFYLT,
                årsak = MELDEPLIKT_FRIST_IKKE_PASSERT
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
        val rettighetsperiode = Periode(
            LocalDate.of(2020, APRIL, 20),
            LocalDate.of(2020, MAY, 17),
        )
        val input = tomUnderveisInput(
            rettighetsperiode = rettighetsperiode,
        )

        val vurdertTidslinje = vurder(input, nå = LocalDate.of(2020, MAY, 7))

        assertVurdering(
            vurdertTidslinje, rettighetsperiode,
            Forventer(
                fom = rettighetsperiode.fom,
                tom = rettighetsperiode.fom.plusDays(13),
                utfall = OPPFYLT,
            ),
            Forventer(
                fom = LocalDate.of(2020, MAY, 4),
                tom = LocalDate.of(2020, MAY, 17),
                utfall = OPPFYLT
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
            LocalDate.of(2020, APRIL, 20),
            LocalDate.of(2020, MAY, 31),
        )
        val input = tomUnderveisInput(
            rettighetsperiode = rettighetsperiode,
        )

        val vurdertTidslinje = vurder(input, nå = LocalDate.of(2020, MAY, 20))

        assertVurdering(
            vurdertTidslinje, rettighetsperiode,
            Forventer(
                fom = rettighetsperiode.fom,
                tom = rettighetsperiode.fom.plusDays(13),
                utfall = OPPFYLT,
            ),
            Forventer(
                fom = LocalDate.of(2020, MAY, 4),
                tom = LocalDate.of(2020, MAY, 17),
                utfall = IKKE_OPPFYLT,
                årsak = IKKE_OVERHOLDT_MELDEPLIKT_SANKSJON,
            ),
            Forventer(
                fom = LocalDate.of(2020, MAY, 18),
                tom = LocalDate.of(2020, MAY, 31),
                utfall = IKKE_OPPFYLT,
                årsak = MELDEPLIKT_FRIST_IKKE_PASSERT,
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
            LocalDate.of(2020, APRIL, 20),
            LocalDate.of(2020, MAY, 31),
        )
        val input = tomUnderveisInput(
            rettighetsperiode = rettighetsperiode,
            meldepliktGrunnlag = MeldepliktGrunnlag(
                listOf(Fritaksvurdering(
                    harFritak = true,
                    fraDato = LocalDate.of(2020, MAY, 28),
                    begrunnelse = "kan ikke",
                    opprettetTid = LocalDateTime.now()
                ))
            )
        )

        val vurdertTidslinje = vurder(input, nå = LocalDate.of(2020, MAY, 20))

        assertVurdering(
            vurdertTidslinje, rettighetsperiode,
            Forventer(
                fom = rettighetsperiode.fom,
                tom = rettighetsperiode.fom.plusDays(13),
                utfall = OPPFYLT,
            ),
            Forventer(
                fom = LocalDate.of(2020, MAY, 4),
                tom = LocalDate.of(2020, MAY, 17),
                utfall = IKKE_OPPFYLT,
                årsak = IKKE_OVERHOLDT_MELDEPLIKT_SANKSJON,
            ),
            Forventer(
                fom = LocalDate.of(2020, MAY, 18),
                tom = LocalDate.of(2020, MAY, 27),
                utfall = IKKE_OPPFYLT,
                årsak = MELDEPLIKT_FRIST_IKKE_PASSERT,
            ),
            Forventer(
                fom = LocalDate.of(2020, MAY, 28),
                tom = LocalDate.of(2020, MAY, 31),
                utfall = OPPFYLT,
                dokument = MeldepliktRegel.Fritak
            )
        )
    }


    private fun vurder(input: UnderveisInput, nå: LocalDate): Tidslinje<Vurdering> {
        val zone = ZoneId.systemDefault()
        val now = nå.atStartOfDay(zone).toInstant()
        return MeldepliktRegel(clock = Clock.fixed(now, zone))
            .vurder(input, UtledMeldeperiodeRegel().vurder(input, Tidslinje()))
    }

    private fun assertVurdering(
        vurdertTidslinje: Tidslinje<Vurdering>,
        rettighetsperiode: Periode,
        vararg forventedeVurderinger: Forventer,
    ) {
        return assertVurdering(vurdertTidslinje, rettighetsperiode, false, *forventedeVurderinger)
    }

    private fun assertVurdering(
        vurdertTidslinje: Tidslinje<Vurdering>,
        rettighetsperiode: Periode,
        aksepterFlereVurderinger: Boolean,
        vararg forventedeVurderinger: Forventer,
    ) {
        val forventetTidslinje = forventedeVurderinger
            .map { Segment(Periode(it.fom, it.tom), it) }
            .let { Tidslinje(it) }

        assertThat(vurdertTidslinje.erSammenhengende()).isTrue()
        assertThat(vurdertTidslinje.helePerioden()).isEqualTo(rettighetsperiode)

        if (!aksepterFlereVurderinger) {
            assertThat(forventetTidslinje.helePerioden()).isEqualTo(rettighetsperiode)
        }

        vurdertTidslinje.kombiner<_, Nothing>(
            forventetTidslinje,
            JoinStyle.RIGHT_JOIN { periode, vurdering, forventet ->
                assertThat(vurdering?.verdi?.meldepliktUtfall())
                    .`as`("for periode $vurdering")
                    .isEqualTo(forventet.verdi.utfall)
                assertThat(vurdering?.verdi?.meldepliktVurdering?.dokument)
                    .`as`("for periode $vurdering")
                    .isEqualTo(forventet.verdi.dokument)
                assertThat(vurdering?.verdi?.meldepliktVurdering?.årsak)
                    .`as`("for periode $vurdering")
                    .isEqualTo(forventet.verdi.årsak)
                null
            })
    }
}