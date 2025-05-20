package no.nav.aap.behandlingsflyt.faktagrunnlag.meldeplikt

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.Fritaksvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.MeldepliktFritaksperioder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class MeldepliktFritaksperioderTest {
    @Test
    fun `rekkefølge som fritaksvurderinger er i lista har ingenting å si`() {
        val fritaksvurderinger = listOf(
            fritaksvurdering(true, LocalDate.now().minusDays(1)),
            fritaksvurdering(false, LocalDate.now().plusDays(1)),
        )

        val fritaksvurderingerReversed = fritaksvurderinger.reversed()
        val fritaksperioder = MeldepliktFritaksperioder(fritaksvurderinger)
        assertThat(fritaksperioder.gjeldendeFritaksvurderinger()).isEqualTo(MeldepliktFritaksperioder(fritaksvurderingerReversed).gjeldendeFritaksvurderinger())
    }

    @Test
    fun `fritaksvurdering-ja, som starter før et fritaksvurdering-nei, har sluttid dagen før fritaksvurdering-nei slutter`() {
        val fritaksvurderinger = listOf(
            fritaksvurdering(true, LocalDate.now().minusDays(1)),
            fritaksvurdering(false, LocalDate.now().plusDays(1)),
        )
        val fritaksperioder = MeldepliktFritaksperioder(fritaksvurderinger)
        assertThat(fritaksperioder.gjeldendeFritaksvurderinger()).isEqualTo(fritaksvurderinger)
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

        val fritaksperioder = MeldepliktFritaksperioder(eksisterendeFritaksvurderinger).leggTil(MeldepliktFritaksperioder(nyFritaksvurdering))

        assertThat(fritaksperioder.gjeldendeFritaksvurderinger()).isEqualTo(nyFritaksvurdering)
    }

    @Test
    fun `to like fritaksvurderinger med annen fraDato som overlapper blir til en fritaksvurdering`() {
        val fritaksvurdering = fritaksvurdering(true, LocalDate.now().minusDays(1))
        val fritaksvurderinger = listOf(fritaksvurdering, fritaksvurdering.copy(fraDato = LocalDate.now()))

        val fritaksperioder = MeldepliktFritaksperioder(fritaksvurderinger)
        assertThat(fritaksperioder.gjeldendeFritaksvurderinger()).containsExactly(fritaksvurdering)
    }

    private fun fritaksvurdering(harFritak: Boolean, fraDato: LocalDate) = Fritaksvurdering(
        harFritak = harFritak,
        fraDato = fraDato,
        begrunnelse = UUID.randomUUID().toString(),
        vurdertAv = "saksbehandler",
        opprettetTid = LocalDateTime.now()
    )
}