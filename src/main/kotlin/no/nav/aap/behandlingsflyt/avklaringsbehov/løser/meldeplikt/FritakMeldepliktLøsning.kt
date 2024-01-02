package no.nav.aap.behandlingsflyt.avklaringsbehov.løser.meldeplikt

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.AvklaringsbehovLøsning
import no.nav.aap.behandlingsflyt.avklaringsbehov.FRITAK_MELDEPLIKT_KODE

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(value = FRITAK_MELDEPLIKT_KODE)
class FritakMeldepliktLøsning(
    @JsonProperty("vurdering", required = true) val vurdering: Fritaksvurdering?
) : AvklaringsbehovLøsning
