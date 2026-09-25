package no.nav.aap.behandlingsflyt.faktagrunnlag.klage.påklagetbehandling

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.verdityper.Bruker
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant
import java.util.UUID

class PåklagetBehandlingVurderingTest {

    private val vurdertAv = Bruker("12345678901")
    private val opprettet = Instant.now()

    @Test
    fun `KELVIN_BEHANDLING krever påklagetBehandling og tillater ikke påklagetTilbakekrevingsbehandling`() {
        assertThrows<IllegalArgumentException> {
            PåklagetBehandlingVurdering(
                påklagetVedtakType = PåklagetVedtakType.KELVIN_BEHANDLING,
                påklagetBehandling = null,
                påklagetTilbakekrevingsbehandling = null,
                vurdertAv = vurdertAv,
                opprettet = opprettet
            )
        }

        assertThrows<IllegalArgumentException> {
            PåklagetBehandlingVurdering(
                påklagetVedtakType = PåklagetVedtakType.KELVIN_BEHANDLING,
                påklagetBehandling = BehandlingId(1L),
                påklagetTilbakekrevingsbehandling = UUID.randomUUID(),
                vurdertAv = vurdertAv,
                opprettet = opprettet
            )
        }

        val vurdering = PåklagetBehandlingVurdering(
            påklagetVedtakType = PåklagetVedtakType.KELVIN_BEHANDLING,
            påklagetBehandling = BehandlingId(1L),
            påklagetTilbakekrevingsbehandling = null,
            vurdertAv = vurdertAv,
            opprettet = opprettet
        )
        assertThat(vurdering.påklagetBehandling).isEqualTo(BehandlingId(1L))
    }

    @Test
    fun `TILBAKEKREVING krever påklagetTilbakekrevingsbehandling og tillater ikke påklagetBehandling`() {
        val tilbakekrevingsReferanse = UUID.randomUUID()

        assertThrows<IllegalArgumentException> {
            PåklagetBehandlingVurdering(
                påklagetVedtakType = PåklagetVedtakType.TILBAKEKREVING,
                påklagetBehandling = null,
                påklagetTilbakekrevingsbehandling = null,
                vurdertAv = vurdertAv,
                opprettet = opprettet
            )
        }

        assertThrows<IllegalArgumentException> {
            PåklagetBehandlingVurdering(
                påklagetVedtakType = PåklagetVedtakType.TILBAKEKREVING,
                påklagetBehandling = BehandlingId(1L),
                påklagetTilbakekrevingsbehandling = tilbakekrevingsReferanse,
                vurdertAv = vurdertAv,
                opprettet = opprettet
            )
        }

        val vurdering = PåklagetBehandlingVurdering(
            påklagetVedtakType = PåklagetVedtakType.TILBAKEKREVING,
            påklagetBehandling = null,
            påklagetTilbakekrevingsbehandling = tilbakekrevingsReferanse,
            vurdertAv = vurdertAv,
            opprettet = opprettet
        )
        assertThat(vurdering.påklagetTilbakekrevingsbehandling).isEqualTo(tilbakekrevingsReferanse)
    }

    @Test
    fun `ARENA_VEDTAK tillater verken påklagetBehandling eller påklagetTilbakekrevingsbehandling`() {
        assertThrows<IllegalArgumentException> {
            PåklagetBehandlingVurdering(
                påklagetVedtakType = PåklagetVedtakType.ARENA_VEDTAK,
                påklagetBehandling = BehandlingId(1L),
                påklagetTilbakekrevingsbehandling = null,
                vurdertAv = vurdertAv,
                opprettet = opprettet
            )
        }

        assertThrows<IllegalArgumentException> {
            PåklagetBehandlingVurdering(
                påklagetVedtakType = PåklagetVedtakType.ARENA_VEDTAK,
                påklagetBehandling = null,
                påklagetTilbakekrevingsbehandling = UUID.randomUUID(),
                vurdertAv = vurdertAv,
                opprettet = opprettet
            )
        }

        val vurdering = PåklagetBehandlingVurdering(
            påklagetVedtakType = PåklagetVedtakType.ARENA_VEDTAK,
            påklagetBehandling = null,
            påklagetTilbakekrevingsbehandling = null,
            vurdertAv = vurdertAv,
            opprettet = opprettet
        )
        assertThat(vurdering.påklagetVedtakType).isEqualTo(PåklagetVedtakType.ARENA_VEDTAK)
    }
}
