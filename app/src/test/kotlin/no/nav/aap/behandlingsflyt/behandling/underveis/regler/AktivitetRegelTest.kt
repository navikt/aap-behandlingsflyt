package no.nav.aap.behandlingsflyt.behandling.underveis.regler

import no.nav.aap.behandlingsflyt.behandling.underveis.Kvote
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisÅrsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.tidslinje.Tidslinje
import no.nav.aap.verdityper.dokument.JournalpostId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.Period


class AktivitetRegelTest {
    private val regel = AktivitetRegel()
    private val kvote = Kvote(Period.ofYears(3))

    @Test
    fun `Meldeplikt overholdt ved innsendt på fastsatt dag`() {
        val fom = LocalDate.now().minusMonths(3)
        val rettighetsperiode = Periode(fom, fom.plusWeeks(6).minusDays(1))
        val input = UnderveisInput(
            rettighetsperiode = rettighetsperiode,
            relevanteVilkår = listOf(),
            opptrappingPerioder = listOf(),
            pliktkort = listOf(),
            innsendingsTidspunkt = mapOf(
                Pair(fom.plusDays(13), JournalpostId("1")),
                Pair(fom.plusDays(27), JournalpostId("2")),
                Pair(fom.plusDays(41), JournalpostId("3"))
            ),
            kvote = kvote,
            bruddAktivitetsplikt = setOf(),
            etAnnetSted = listOf(),
        )

        val vurdertTidslinje = regel.vurder(input, Tidslinje())

        assertThat(vurdertTidslinje.segmenter()).allMatch { it.verdi.meldeplikUtfall() == Utfall.OPPFYLT }
    }

    @Test
    fun `Skal starte med full utbetalingsplan`() {
        val fom = LocalDate.now()
        val rettighetsperiode = Periode(fom, fom.plusYears(1))
        val input = UnderveisInput(
            rettighetsperiode = rettighetsperiode,
            relevanteVilkår = listOf(),
            opptrappingPerioder = listOf(),
            pliktkort = listOf(),
            innsendingsTidspunkt = mapOf(),
            kvote = kvote,
            bruddAktivitetsplikt = setOf(),
            etAnnetSted = listOf(),
        )

        val vurdertTidslinje = regel.vurder(input, Tidslinje())

        assertThat(vurdertTidslinje.segmenter()).allMatch { it.verdi.meldeplikUtfall() == Utfall.OPPFYLT }
    }

    @Test
    fun `Meldeplikt skal være stanset etter at man ikke har meldt seg og fristen utløpt`() {
        val fom = LocalDate.now().minusMonths(6)
        val rettighetsperiode = Periode(fom, fom.plusWeeks(12).minusDays(1))
        val input = UnderveisInput(
            rettighetsperiode = rettighetsperiode,
            relevanteVilkår = listOf(),
            opptrappingPerioder = listOf(),
            pliktkort = listOf(),
            innsendingsTidspunkt = mapOf(
                Pair(fom.plusDays(27), JournalpostId("1")),
                Pair(fom.plusDays(60), JournalpostId("2"))
            ),
            kvote = kvote,
            bruddAktivitetsplikt = setOf(),
            etAnnetSted = listOf(),
        )

        val vurdertTidslinje = regel.vurder(input, Tidslinje())

        val avslåttesegmenter = vurdertTidslinje.segmenter().filter { it.verdi.meldeplikUtfall() != Utfall.OPPFYLT }
        val oppfyltesegmenter = vurdertTidslinje.segmenter().filter { it.verdi.meldeplikUtfall() == Utfall.OPPFYLT }
        assertThat(avslåttesegmenter).hasSize(5)
        assertThat(avslåttesegmenter).allMatch { it.verdi.meldeplikAvslagsårsak() == UnderveisÅrsak.IKKE_OVERHOLDT_MELDEPLIKT_SANKSJON }
        assertThat(oppfyltesegmenter).hasSize(3)
    }

    @Test
    fun `Meldeplikt ikke overholdt ved innsendt på fastsatt dag`() {
        val fom = LocalDate.now().minusMonths(3)
        val rettighetsperiode = Periode(fom, fom.plusWeeks(6).minusDays(1))
        val input = UnderveisInput(
            rettighetsperiode = rettighetsperiode,
            relevanteVilkår = listOf(),
            opptrappingPerioder = listOf(),
            pliktkort = listOf(),
            innsendingsTidspunkt = mapOf(
                Pair(fom.plusDays(13), JournalpostId("1")),
                Pair(fom.plusDays(33), JournalpostId("2"))
            ),
            kvote = kvote,
            bruddAktivitetsplikt = setOf(),
            etAnnetSted = listOf(),
        )

        val vurdertTidslinje = regel.vurder(input, Tidslinje())

        val avslåttesegmenter = vurdertTidslinje.segmenter().filter { it.verdi.meldeplikUtfall() != Utfall.OPPFYLT }
        assertThat(avslåttesegmenter).hasSize(2)
        assertThat(avslåttesegmenter).allMatch { it.verdi.meldeplikAvslagsårsak() == UnderveisÅrsak.IKKE_OVERHOLDT_MELDEPLIKT_SANKSJON }
    }
}