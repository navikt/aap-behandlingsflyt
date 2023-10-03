package no.nav.aap.behandlingsflyt.avklaringsbehov.vedtak

import com.fasterxml.jackson.annotation.JsonProperty

data class TotrinnsVurdering(
    @JsonProperty(required = true, value = "definisjon") val definisjon: String,
    @JsonProperty(required = true, value = "godkjent") val godkjent: Boolean,
    @JsonProperty(required = true, value = "begrunnelse") val begrunnelse: String
) {

}
