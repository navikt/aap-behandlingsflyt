package no.nav.aap.flate.behandling.avklaringsbehov

import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.aap.avklaringsbehov.AvklaringsbehovLøsning
import java.util.*

data class LøsAvklaringsbehovPåBehandling(
    @JsonProperty(value = "referanse", required = true) val referanse: UUID,
    @JsonProperty(value = "behandlingVersjon", required = true, defaultValue = "0") val behandlingVersjon: Long,
    @JsonProperty(value = "behov", required = true) val behov: AvklaringsbehovLøsning
)