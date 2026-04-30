package no.nav.aap.behandlingsflyt.behandling.beregning

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre.Uføre
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre.tilTidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.komponenter.verdityper.Tid
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class UføreTidslinjeTest {

    @Test
    fun `Lager riktig tidslinje ved redusert men sammenhengende uføre`() {
        val redusertUføre = setOf(
            Uføre(
                virkningstidspunkt = LocalDate.of(2021, 2, 1),
                uføregradFom = LocalDate.of(2019, 2, 1),
                uføregradTom = LocalDate.of(2021, 9, 30),
                uføregrad = Prosent(80)
            ),
            Uføre(
                virkningstidspunkt = LocalDate.of(2022, 7, 1),
                uføregrad = Prosent(50),
                uføregradFom = LocalDate.of(2021, 10, 1),
                uføregradTom = null,
            )
        )

        val tidslinje = redusertUføre.tilTidslinje()
        assertThat(tidslinje.erSammenhengende()).isTrue()
        assertThat(tidslinje.helePerioden().fom).isEqualTo(LocalDate.of(2021, 2, 1))
        assertThat(tidslinje.helePerioden().tom).isEqualTo(Tid.MAKS)
    }

    @Test
    fun `Lager riktig tidslinje for to perioder med stans mellom`() {
        val redusertUføreMedFaktiskStans = setOf(
            Uføre(
                virkningstidspunkt = LocalDate.of(2021, 2, 1),
                uføregradFom = LocalDate.of(2019, 2, 1),
                uføregradTom = LocalDate.of(2021, 6, 30),
                uføregrad = Prosent(80)
            ),
            Uføre(
                virkningstidspunkt = LocalDate.of(2022, 7, 1),
                uføregrad = Prosent(50),
                uføregradFom = LocalDate.of(2021, 10, 1),
                uføregradTom = null,
            )
        )

        val tidslinje = redusertUføreMedFaktiskStans.tilTidslinje()
        assertThat(tidslinje.erSammenhengende()).isFalse()
        assertThat(tidslinje.segmenter().map { it.periode }).containsExactly(
            Periode(LocalDate.of(2021, 2, 1), LocalDate.of(2021, 6, 30)),
            Periode(LocalDate.of(2022, 7, 1), Tid.MAKS),
        )
    }

    @Test
    fun `Tidslinje for reduksjon med sammenhengende perioder og så stans`() {
        val førstSammenhengendeReduksjonOgSåStans = setOf(
            Uføre(
                virkningstidspunkt = LocalDate.of(2021, 2, 1),
                uføregradFom = LocalDate.of(2019, 2, 1),
                uføregradTom = LocalDate.of(2021, 9, 30),
                uføregrad = Prosent(80)
            ),
            Uføre(
                virkningstidspunkt = LocalDate.of(2022, 7, 1),
                uføregrad = Prosent(50),
                uføregradFom = LocalDate.of(2021, 10, 1),
                uføregradTom = LocalDate.of(2023, 12, 31),
            )
        )

        val tidslinje = førstSammenhengendeReduksjonOgSåStans.tilTidslinje()
        assertThat(tidslinje.erSammenhengende()).isTrue()
        assertThat(tidslinje.helePerioden().tom).isEqualTo(LocalDate.of(2023, 12, 31))
        assertThat(tidslinje.segmenter().map { it.periode }).containsExactly(
            Periode(LocalDate.of(2021, 2, 1), LocalDate.of(2022, 6, 30)),
            Periode(LocalDate.of(2022, 7, 1), LocalDate.of(2023, 12, 31)),
        )
    }

    @Test
    fun `Når uføregradFom ikke finnes ennå skal sluttdato bare settes for siste segment`() {
        val sisteSegmentHarSluttdato = setOf(
            Uføre(
                virkningstidspunkt = LocalDate.of(2021, 2, 1),
                uføregradTom = LocalDate.of(2021, 9, 30),
                uføregrad = Prosent(80)
            ),
            Uføre(
                virkningstidspunkt = LocalDate.of(2022, 7, 1),
                uføregrad = Prosent(50),
                uføregradTom = LocalDate.of(2023, 12, 31),
            )
        )

        val tidslinje = sisteSegmentHarSluttdato.tilTidslinje()
        assertThat(tidslinje.erSammenhengende()).isTrue()
        assertThat(tidslinje.helePerioden().tom).isEqualTo(LocalDate.of(2023, 12, 31))
        assertThat(tidslinje.segmenter().map { it.periode }).containsExactly(
            Periode(LocalDate.of(2021, 2, 1), LocalDate.of(2022, 6, 30)),
            Periode(LocalDate.of(2022, 7, 1), LocalDate.of(2023, 12, 31)),
        )
    }
}