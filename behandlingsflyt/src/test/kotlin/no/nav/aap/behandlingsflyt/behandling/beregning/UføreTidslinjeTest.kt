package no.nav.aap.behandlingsflyt.behandling.beregning

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre.Uføre
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre.tilTidslinje
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.komponenter.verdityper.Tid
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class UføreTidslinjeTest {

    @Test
    fun `Bygger tidslinje riktig basert på fra- og til-dato for uføregrader`() {
        // redusert, men sammenhengende uføre
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

        assertThat(redusertUføre.tilTidslinje().erSammenhengende()).isTrue()

        // redusert uføre, men med reell stans mellom de to uføregradene
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

        assertThat(redusertUføreMedFaktiskStans.tilTidslinje().erSammenhengende()).isFalse()

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
        assertThat(førstSammenhengendeReduksjonOgSåStans.tilTidslinje().erSammenhengende()).isTrue()
        assertThat(førstSammenhengendeReduksjonOgSåStans.tilTidslinje().helePerioden().tom).isEqualTo(LocalDate.of(2023, 12, 31))


        val midlertidigStansIMidtenAvTidslinje = setOf(
            Uføre(
                virkningstidspunkt = LocalDate.of(2021, 2, 1),
                uføregradFom = LocalDate.of(2019, 2, 1),
                uføregradTom = LocalDate.of(2021, 9, 30),
                uføregrad = Prosent(80)
            ),
            Uføre(
                virkningstidspunkt = LocalDate.of(2022, 7, 1),
                uføregrad = Prosent(50),
                uføregradFom = LocalDate.of(2022, 1, 1),
                uføregradTom = null
            )
        )

        assertThat(midlertidigStansIMidtenAvTidslinje.tilTidslinje().erSammenhengende()).isFalse()
        assertThat(midlertidigStansIMidtenAvTidslinje.tilTidslinje().helePerioden().tom).isEqualTo(Tid.MAKS)
    }
}