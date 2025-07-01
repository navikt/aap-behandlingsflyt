package no.nav.aap.behandlingsflyt.faktagrunnlag.arbeidsevne

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsevne.ArbeidsevnePerioder
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsevne.ArbeidsevneVurdering
import no.nav.aap.komponenter.verdityper.Prosent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class ArbeidsevnePerioderTest {
    @Test
    fun `rekkefølge som Arbeidsevnevurdering er i lista har ingenting å si`() {
        val arbeidsevneVurderinger = listOf(
            arbeidsevneVurdering(Prosent.`100_PROSENT`, LocalDate.now().minusDays(1)),
            arbeidsevneVurdering(Prosent.`50_PROSENT`, LocalDate.now().plusDays(1))
        )
        val arbeidsevneVurderingerReversed = arbeidsevneVurderinger.reversed()
        val arbeidsevnePerioder = ArbeidsevnePerioder(arbeidsevneVurderinger)

        assertThat(arbeidsevnePerioder.gjeldendeArbeidsevner()).isEqualTo(ArbeidsevnePerioder(arbeidsevneVurderingerReversed).gjeldendeArbeidsevner())
    }

    @Test
    fun `Arbeidsevner sorteres kronologisk og den ene slutter når den andre starter`() {
        val arbeidsevneVurderinger = listOf(
            arbeidsevneVurdering(Prosent.`100_PROSENT`, LocalDate.now().minusDays(1)),
            arbeidsevneVurdering(Prosent.`50_PROSENT`, LocalDate.now().plusDays(1))
        )
        val arbeidsevnePerioder = ArbeidsevnePerioder(arbeidsevneVurderinger)
        assertThat(arbeidsevnePerioder.gjeldendeArbeidsevner()).isEqualTo(arbeidsevneVurderinger)
    }

    @Test
    fun `nye arbeidsevner har prioritet over de gamle`() {
        val eksisterendeArbeidsevnevurderinger = listOf(
            arbeidsevneVurdering(Prosent.`100_PROSENT`, LocalDate.now().minusDays(1)),
            arbeidsevneVurdering(Prosent.`50_PROSENT`, LocalDate.now().plusDays(1))
        )
        val nyArbeidsevnevurdering = listOf(
            arbeidsevneVurdering(Prosent.`30_PROSENT`, LocalDate.now().minusDays(2))
        )

        val arbeidsevnePerioder = ArbeidsevnePerioder(eksisterendeArbeidsevnevurderinger).leggTil(
            ArbeidsevnePerioder(nyArbeidsevnevurdering)
        )

        assertThat(arbeidsevnePerioder.gjeldendeArbeidsevner()).isEqualTo(nyArbeidsevnevurdering)
    }

    @Test
    fun `to like arbeidsevner med annen fraDato som overlapper blir til én arbeidsevne`() {
        val arbeidsevneVurdering = arbeidsevneVurdering(Prosent.`30_PROSENT`, LocalDate.now())
        val arbeidsevneVurderinger = listOf(
            arbeidsevneVurdering,
            arbeidsevneVurdering.copy(fraDato = arbeidsevneVurdering.fraDato.plusDays(1))
        )

        val arbeidsevnePerioder = ArbeidsevnePerioder(arbeidsevneVurderinger)
        assertThat(arbeidsevnePerioder.gjeldendeArbeidsevner()).containsExactly(arbeidsevneVurdering)
    }

    private fun arbeidsevneVurdering(arbeidsevne: Prosent, fraDato: LocalDate) = ArbeidsevneVurdering(
        UUID.randomUUID().toString(), arbeidsevne, fraDato, LocalDateTime.now(), "vurdertAv"
    )
}