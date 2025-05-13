package no.nav.aap.behandlingsflyt.faktagrunnlag.klage.påklagetbehandling

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import java.time.Instant

data class PåklagetBehandlingVurdering(
    val påklagetVedtakType: PåklagetVedtakType,
    val påklagetBehandling: BehandlingId?,
    val vurdertAv: String,
    val opprettet: Instant? = null
) {
    init {
        require(påklagetVedtakType == PåklagetVedtakType.KELVIN_BEHANDLING || påklagetBehandling != null) {
            "Påklaget behandling må være utfylt dersom det klages på et Kelvin-vedtak"
        }
    }
}

enum class PåklagetVedtakType {
    KELVIN_BEHANDLING,
    ARENA_VEDTAK
}