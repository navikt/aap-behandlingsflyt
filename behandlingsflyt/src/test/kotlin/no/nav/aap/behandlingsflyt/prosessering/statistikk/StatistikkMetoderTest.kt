package no.nav.aap.behandlingsflyt.prosessering.statistikk

import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.påklagetbehandling.PåklagetBehandlingVurderingMedReferanse
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.påklagetbehandling.PåklagetVedtakType
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.verdityper.Bruker
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class StatistikkMetoderTest {

    private val vurdertAv = Bruker("ident")
    private val opprettet = Instant.now()

    @Test
    fun `klage på kelvinvedtak gir referanse til intern kelvinbehandling`() {
        val behandlingReferanse = BehandlingReferanse()
        val vurdering = PåklagetBehandlingVurderingMedReferanse(
            påklagetVedtakType = PåklagetVedtakType.KELVIN_BEHANDLING,
            påklagetBehandling = BehandlingId(1L),
            påklagetTilbakekrevingsbehandling = null,
            referanse = behandlingReferanse,
            vurdertAv = vurdertAv,
            opprettet = opprettet
        )

        assertThat(relatertBehandlingReferanseForKlage(vurdering)).isEqualTo(behandlingReferanse.referanse)
    }

    @Test
    fun `klage på tilbakekrevingsvedtak gir referanse til ekstern tilbakekrevingsbehandling`() {
        val tilbakekrevingsReferanse = UUID.randomUUID()
        val vurdering = PåklagetBehandlingVurderingMedReferanse(
            påklagetVedtakType = PåklagetVedtakType.TILBAKEKREVING,
            påklagetBehandling = null,
            påklagetTilbakekrevingsbehandling = tilbakekrevingsReferanse,
            referanse = null,
            vurdertAv = vurdertAv,
            opprettet = opprettet
        )

        assertThat(relatertBehandlingReferanseForKlage(vurdering)).isEqualTo(tilbakekrevingsReferanse)
    }

    @Test
    fun `klage på arenavedtak gir ingen referanse - støttes ikke per idag`() {
        val vurdering = PåklagetBehandlingVurderingMedReferanse(
            påklagetVedtakType = PåklagetVedtakType.ARENA_VEDTAK,
            påklagetBehandling = null,
            påklagetTilbakekrevingsbehandling = null,
            referanse = null,
            vurdertAv = vurdertAv,
            opprettet = opprettet
        )

        assertThat(relatertBehandlingReferanseForKlage(vurdering)).isNull()
    }

    @Test
    fun `Ingen vurdering gir ingen referanse`() {
        assertThat(relatertBehandlingReferanseForKlage(null)).isNull()
    }
}
