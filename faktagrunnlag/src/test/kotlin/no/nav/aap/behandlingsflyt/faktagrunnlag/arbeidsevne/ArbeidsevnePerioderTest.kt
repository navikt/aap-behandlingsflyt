package no.nav.aap.behandlingsflyt.faktagrunnlag.arbeidsevne

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsevne.ArbeidsevnePerioder
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsevne.Arbeidsevnevurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.Fritaksvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.MeldepliktFritaksperioder
import no.nav.aap.verdityper.Prosent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class ArbeidsevnePerioderTest {
    @Test
    fun `rekkefølge som Arbeidsevnevurdering er i lista har ingenting å si`() {
        val arbeidsevnevurderinger = listOf(
            arbeidsevnevurdering(Prosent.`100_PROSENT`, LocalDate.now().minusDays(1)),
            arbeidsevnevurdering(Prosent.`50_PROSENT`, LocalDate.now().plusDays(1))
        )
        val arbeidsevnevurderingerReversed = arbeidsevnevurderinger.reversed()
        val arbeidsevnePerioder = ArbeidsevnePerioder(arbeidsevnevurderinger)

        assertThat(arbeidsevnePerioder.gjeldendeArbeidsevner()).isEqualTo(ArbeidsevnePerioder(arbeidsevnevurderingerReversed).gjeldendeArbeidsevner())
    }

    @Test
    fun `Arbeidsevner sorteres kronologisk og den ene slutter når den andre starter`() {
        val arbeidsevnevurderinger = listOf(
            arbeidsevnevurdering(Prosent.`100_PROSENT`, LocalDate.now().minusDays(1)),
            arbeidsevnevurdering(Prosent.`50_PROSENT`, LocalDate.now().plusDays(1))
        )
        val arbeidsevnePerioder = ArbeidsevnePerioder(arbeidsevnevurderinger)
        assertThat(arbeidsevnePerioder.gjeldendeArbeidsevner()).isEqualTo(arbeidsevnevurderinger)
    }

    @Test
    fun `nye arbeidsevner har prioritet over de gamle`() {
        val eksisterendeArbeidsevnevurderinger = listOf(
            arbeidsevnevurdering(Prosent.`100_PROSENT`, LocalDate.now().minusDays(1)),
            arbeidsevnevurdering(Prosent.`50_PROSENT`, LocalDate.now().plusDays(1))
        )
        val nyArbeidsevnevurdering = listOf(
            arbeidsevnevurdering(Prosent.`30_PROSENT`, LocalDate.now().minusDays(2))
        )

        val arbeidsevnePerioder = ArbeidsevnePerioder(eksisterendeArbeidsevnevurderinger).leggTil(
            ArbeidsevnePerioder(nyArbeidsevnevurdering)
        )

        assertThat(arbeidsevnePerioder.gjeldendeArbeidsevner()).isEqualTo(nyArbeidsevnevurdering)
    }

    @Test
    fun `to like arbeidsevner med annen fraDato som overlapper blir til én arbeidsevne`() {
        val arbeidsevnevurdering = arbeidsevnevurdering(Prosent.`30_PROSENT`, LocalDate.now())
        val arbeidsevnevurderinger = listOf(
            arbeidsevnevurdering,
            arbeidsevnevurdering.copy(fraDato = arbeidsevnevurdering.fraDato.plusDays(1))
        )

        val arbeidsevnePerioder = ArbeidsevnePerioder(arbeidsevnevurderinger)
        assertThat(arbeidsevnePerioder.gjeldendeArbeidsevner()).containsExactly(arbeidsevnevurdering)
    }

    private fun arbeidsevnevurdering(arbeidsevne: Prosent, fraDato: LocalDate) = Arbeidsevnevurdering(
        UUID.randomUUID().toString(), arbeidsevne, fraDato, LocalDateTime.now()
    )
}