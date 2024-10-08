package no.nav.aap.behandlingsflyt.faktagrunnlag.meldeplikt

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.Fritaksperioder
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.Fritaksvurdering
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class FritaksperioderTest {
    @Test
    fun `rekkefølge som fritaksvurderinger er i lista har ingenting å si`() {
        val fritaksvurderinger = listOf(
            fritaksvurdering(true, LocalDate.now().minusDays(1)),
            fritaksvurdering(false, LocalDate.now().plusDays(1)),
        )

        val fritaksvurderingerReversed = fritaksvurderinger.reversed()
        val fritaksperioder = Fritaksperioder(fritaksvurderinger)
        assertThat(fritaksperioder.gjeldendeFritaksvurderinger()).isEqualTo(Fritaksperioder(fritaksvurderingerReversed).gjeldendeFritaksvurderinger())
    }

    @Test
    fun `fritaksvurdering-ja, som starter før et fritaksvurdering-nei, har sluttid dagen før fritaksvurdering-nei slutter`() {
        val fritaksvurderinger = listOf(
            fritaksvurdering(true, LocalDate.now().minusDays(1)),
            fritaksvurdering(false, LocalDate.now().plusDays(1)),
        )
        val fritaksperioder = Fritaksperioder(fritaksvurderinger)
        assertThat(fritaksperioder.gjeldendeFritaksvurderinger()).isEqualTo(fritaksvurderinger)
    }

    @Test
    fun `fritaksvurdering-nei, som starter før et fritaksvurdering-ja, resulterer i kun en gjeldende vurdering`() {
        val gjeldendeFritaksvurdering = fritaksvurdering(true, LocalDate.now().plusDays(1))
        val fritaksvurderinger = listOf(
            fritaksvurdering(false, LocalDate.now().minusDays(1)),
            gjeldendeFritaksvurdering,
        )
        val fritaksperioder = Fritaksperioder(fritaksvurderinger)
        assertThat(fritaksperioder.gjeldendeFritaksvurderinger()).containsExactly(gjeldendeFritaksvurdering)
    }

    @Test
    fun `nye fritaksvurderinger har prioritet over de gamle`() {
        val eksisterendeFritaksvurderinger = listOf(
            fritaksvurdering(true, LocalDate.now().minusDays(1)),
            fritaksvurdering(false, LocalDate.now().plusDays(1)),
        )
        val nyFritaksvurdering = listOf(
            fritaksvurdering(true, LocalDate.now().minusDays(2)),
        )

        val fritaksperioder = Fritaksperioder(eksisterendeFritaksvurderinger) leggTil nyFritaksvurdering

        assertThat(fritaksperioder.gjeldendeFritaksvurderinger()).isEqualTo(nyFritaksvurdering)
    }

    @Test
    fun `to like fritaksvurderinger med annen fraDato som overlapper blir til en fritaksvurdering`() {
        val fritaksvurdering = fritaksvurdering(true, LocalDate.now().minusDays(1))
        val fritaksvurderinger = listOf(fritaksvurdering, fritaksvurdering.copy(fraDato = LocalDate.now()))

        val fritaksperioder = Fritaksperioder(fritaksvurderinger)
        assertThat(fritaksperioder.gjeldendeFritaksvurderinger()).containsExactly(fritaksvurdering)
    }

    private fun fritaksvurdering(harFritak: Boolean, fraDato: LocalDate) = Fritaksvurdering(
        harFritak, fraDato, UUID.randomUUID().toString(), LocalDateTime.now()
    )
}