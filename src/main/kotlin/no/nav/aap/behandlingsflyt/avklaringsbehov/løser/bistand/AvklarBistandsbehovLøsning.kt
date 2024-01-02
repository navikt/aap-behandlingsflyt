package no.nav.aap.behandlingsflyt.avklaringsbehov.løser.bistand

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.AvklaringsbehovLøsning
import no.nav.aap.behandlingsflyt.avklaringsbehov.AVKLAR_BISTANDSBEHOV_KODE

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(value = AVKLAR_BISTANDSBEHOV_KODE)
class AvklarBistandsbehovLøsning(
    @JsonProperty("bistandsVurdering", required = true) val bistandVurdering: BistandVurdering
) :
    AvklaringsbehovLøsning
