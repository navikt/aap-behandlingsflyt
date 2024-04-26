package no.nav.aap.behandlingsflyt.avklaringsbehov.løser.vedtak

import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.aap.behandlingsflyt.avklaringsbehov.ÅrsakTilRetur

data class TotrinnsVurdering(
    @JsonProperty(required = true, value = "definisjon") val definisjon: String,
    @JsonProperty(required = true, value = "godkjent") val godkjent: Boolean?,
    @JsonProperty(value = "begrunnelse") val begrunnelse: String?,
    @JsonProperty(value = "grunner") val grunner: List<ÅrsakTilRetur>?
) {
    fun valider(): Boolean {
        if (godkjent == false) {
            requireNotNull(begrunnelse)
        }
        return true
    }

    fun begrunnelse(): String {
        if (godkjent == true) {
            return begrunnelse ?: ""
        }
        return requireNotNull(begrunnelse)
    }
}
