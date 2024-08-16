package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.flate

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.papsign.ktor.openapigen.annotations.Response
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklaringsbehovLøsning
import no.nav.aap.tilgang.Behandlingsreferanse
import java.util.*

@Response(statusCode = 202)
@JsonIgnoreProperties(ignoreUnknown = true)
data class LøsAvklaringsbehovPåBehandling(
    @JsonProperty(value = "referanse", required = true) val referanse: UUID,
    @JsonProperty(value = "behandlingVersjon", required = true, defaultValue = "0") val behandlingVersjon: Long,
    @JsonProperty(value = "behov", required = true) val behov: AvklaringsbehovLøsning,
    @JsonProperty(value = "ingenEndringIGruppe") val ingenEndringIGruppe: Boolean?,
): Behandlingsreferanse {
    override fun hentBehandlingsreferanse(): String {
        return referanse.toString()
    }
    override fun hentAvklaringsbehovKode(): String {
        return behov.toString()
    }


}
