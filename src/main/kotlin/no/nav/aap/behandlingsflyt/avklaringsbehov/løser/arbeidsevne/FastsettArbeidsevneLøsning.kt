package no.nav.aap.behandlingsflyt.avklaringsbehov.løser.arbeidsevne

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.AvklaringsbehovLøsning
import no.nav.aap.behandlingsflyt.avklaringsbehov.FASTSETT_ARBEIDSEVNE_KODE

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(value = FASTSETT_ARBEIDSEVNE_KODE)
class FastsettArbeidsevneLøsning(
    @JsonProperty("arbeidsevne", required = true) val arbeidsevne: Arbeidsevne
) : AvklaringsbehovLøsning
