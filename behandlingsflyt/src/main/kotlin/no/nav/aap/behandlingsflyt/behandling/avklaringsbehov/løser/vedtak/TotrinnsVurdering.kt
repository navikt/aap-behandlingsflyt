package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.vedtak

import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.ÅrsakTilRetur
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AvklaringsbehovKode
import no.nav.aap.oppgave.markering.MarkeringDto

data class TotrinnsVurdering(
    @param:JsonProperty(required = true, value = "definisjon") val definisjon: AvklaringsbehovKode,
    @param:JsonProperty(required = true, value = "godkjent") val godkjent: Boolean?,
    @param:JsonProperty(value = "begrunnelse") val begrunnelse: String?,
    @param:JsonProperty(value = "grunner") val grunner: List<ÅrsakTilRetur>?,
    @param:JsonProperty(value = "markeringer") val markeringer: List<MarkeringDto>?,
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
