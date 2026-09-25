package no.nav.aap.behandlingsflyt.faktagrunnlag.klage.påklagetbehandling

import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.verdityper.Bruker
import java.time.Instant
import java.util.UUID

data class PåklagetBehandlingVurdering(
    val påklagetVedtakType: PåklagetVedtakType,
    val påklagetBehandling: BehandlingId?,
    val påklagetTilbakekrevingsbehandling: UUID?,
    val vurdertAv: Bruker,
    val opprettet: Instant
) {
    init {
        validerReferanserForType(påklagetVedtakType, påklagetBehandling, påklagetTilbakekrevingsbehandling)
    }
}

data class PåklagetBehandlingVurderingMedReferanse(
    val påklagetVedtakType: PåklagetVedtakType,
    val påklagetBehandling: BehandlingId?,
    val påklagetTilbakekrevingsbehandling: UUID?,
    val referanse: BehandlingReferanse?,
    val vurdertAv: Bruker,
    val opprettet: Instant
) {
    init {
        validerReferanserForType(påklagetVedtakType, påklagetBehandling, påklagetTilbakekrevingsbehandling)
    }
}

/**
 * Kun én av referansene skal være satt, avhengig av hva slags vedtak det klages på:
 * - KELVIN_BEHANDLING refererer til en intern [BehandlingId].
 * - TILBAKEKREVING refererer til en ekstern tilbakekrevingsbehandling-UUID
 *   (Tilbakekrevingsbehandling har ingen intern BehandlingId).
 * - ARENA_VEDTAK har ingen av delene, siden det ikke finnes en behandling å referere til i Kelvin.
 */
private fun validerReferanserForType(
    påklagetVedtakType: PåklagetVedtakType,
    påklagetBehandling: BehandlingId?,
    påklagetTilbakekrevingsbehandling: UUID?
) {
    when (påklagetVedtakType) {
        PåklagetVedtakType.KELVIN_BEHANDLING -> {
            require(påklagetBehandling != null) {
                "Påklaget behandling må være utfylt dersom det klages på et Kelvin-vedtak"
            }
            require(påklagetTilbakekrevingsbehandling == null) {
                "Påklaget tilbakekrevingsbehandling skal ikke være utfylt for Kelvin-vedtak"
            }
        }

        PåklagetVedtakType.TILBAKEKREVING -> {
            require(påklagetTilbakekrevingsbehandling != null) {
                "Påklaget tilbakekrevingsbehandling må være utfylt dersom det klages på et tilbakekrevingsvedtak"
            }
            require(påklagetBehandling == null) {
                "Påklaget behandling skal ikke være utfylt for tilbakekrevingsvedtak"
            }
        }

        PåklagetVedtakType.ARENA_VEDTAK -> {
            require(påklagetBehandling == null && påklagetTilbakekrevingsbehandling == null) {
                "Verken påklaget behandling eller påklaget tilbakekrevingsbehandling skal være utfylt for Arena-vedtak"
            }
        }
    }
}

enum class PåklagetVedtakType {
    KELVIN_BEHANDLING,
    ARENA_VEDTAK,
    TILBAKEKREVING
}