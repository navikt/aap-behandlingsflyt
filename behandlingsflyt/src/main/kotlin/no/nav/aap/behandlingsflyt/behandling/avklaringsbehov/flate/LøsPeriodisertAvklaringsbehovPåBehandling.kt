package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.flate

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.papsign.ktor.openapigen.annotations.Response
import java.util.UUID
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.PeriodisertAvklaringsbehovLøsning
import no.nav.aap.tilgang.plugin.kontrakt.Behandlingsreferanse

@Response(statusCode = 202)
@JsonIgnoreProperties(ignoreUnknown = true)
data class LøsPeriodisertAvklaringsbehovPåBehandling(
    @param:JsonProperty(value = "referanse", required = true) val referanse: UUID,
    @param:JsonProperty(value = "behandlingVersjon", required = true, defaultValue = "0") val behandlingVersjon: Long,
    @param:JsonProperty(value = "behov", required = true) val behov: PeriodisertAvklaringsbehovLøsning<*>,
    @param:JsonProperty(value = "ingenEndringIGruppe") val ingenEndringIGruppe: Boolean?,
) : Behandlingsreferanse {
    override fun hentAvklaringsbehovKode(): String {
        return behov.definisjon().kode.toString()
    }

    override fun behandlingsreferanseResolverInput(): String {
        return referanse.toString()
    }
}