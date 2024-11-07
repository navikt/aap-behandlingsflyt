package no.nav.aap.behandlingsflyt.behandling.underveis.regler

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisÅrsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisÅrsak.IKKE_OVERHOLDT_MELDEPLIKT_SANKSJON
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall.IKKE_OPPFYLT
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall.OPPFYLT
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.Fritaksvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.MeldepliktGrunnlag
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.tidslinje.JoinStyle
import no.nav.aap.tidslinje.Segment
import no.nav.aap.tidslinje.Tidslinje
import no.nav.aap.verdityper.dokument.JournalpostId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.LocalDate
import java.time.Month.APRIL
import java.time.Month.FEBRUARY
import java.time.Month.JANUARY
import java.time.Month.MARCH
import java.time.ZoneId


class MeldepliktRegelTest {
    private data class Forventer(
        val fom: LocalDate,
        val tom: LocalDate,
        val utfall: Utfall,
        val årsak: UnderveisÅrsak? = null,
        val journalpostId: JournalpostId? = null,
        val fritak: Boolean = false,
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
        val input = tomUnderveisInput.copy(
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
                journalpostId = JournalpostId("1"),
                utfall = OPPFYLT,
            ),
            Forventer(
                fom = LocalDate.of(2020, FEBRUARY, 3),
                tom = LocalDate.of(2020, FEBRUARY, 16),
                journalpostId = JournalpostId("2"),
                utfall = OPPFYLT,
            ),
            Forventer(
                fom = LocalDate.of(2020, FEBRUARY, 17),
                tom = LocalDate.of(2020, MARCH, 1),
                journalpostId = JournalpostId("3"),
                utfall = OPPFYLT,
            ),
        )
    }

    @Test
    fun `Skal starte med full utbetalingsplan`() {
        val rettighetsperiode = Periode(
            LocalDate.of(2020, APRIL, 20),
            LocalDate.of(2021, APRIL, 19),
        )
        val input = tomUnderveisInput.copy(
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

        val input = tomUnderveisInput.copy(
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
                journalpostId = JournalpostId("1"), // TODO: burde kanskje være null?
                utfall = IKKE_OPPFYLT,
                årsak = IKKE_OVERHOLDT_MELDEPLIKT_SANKSJON,
            ),
            Forventer(
                fom = LocalDate.of(2020, FEBRUARY, 2), tom = LocalDate.of(2020, FEBRUARY, 2),
                journalpostId = JournalpostId("1"),
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
                journalpostId = JournalpostId("2"),
                utfall = OPPFYLT,
            ),
            Forventer(
                fom = LocalDate.of(2020, MARCH, 16), tom = LocalDate.of(2020, MARCH, 29),
                utfall = IKKE_OPPFYLT,
                årsak = IKKE_OVERHOLDT_MELDEPLIKT_SANKSJON,
            ),
        )
    }

    @Test
    fun `Meldeplikt overholdt ved fritak hele meldeperioden`() {
        val rettighetsperiode = Periode(
            LocalDate.of(2020, APRIL, 20),
            LocalDate.of(2021, APRIL, 19),
        )
        val input = tomUnderveisInput.copy(
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
                fritak = true,
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
                assertThat(vurdering?.verdi?.meldeplikUtfall())
                    .`as`("for periode $periode")
                    .isEqualTo(forventet.verdi.utfall)
                assertThat(vurdering?.verdi?.meldepliktVurdering?.journalpostId)
                    .`as`("for periode $periode")
                    .isEqualTo(forventet.verdi.journalpostId)
                assertThat(vurdering?.verdi?.meldepliktVurdering?.fritak)
                    .`as`("for periode $periode")
                    .isEqualTo(forventet.verdi.fritak)
                assertThat(vurdering?.verdi?.meldepliktVurdering?.årsak)
                    .`as`("for periode $periode")
                    .isEqualTo(forventet.verdi.årsak)
                null
            })
    }
}