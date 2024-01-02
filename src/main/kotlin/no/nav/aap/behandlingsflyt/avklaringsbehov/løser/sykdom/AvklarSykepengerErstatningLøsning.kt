package no.nav.aap.behandlingsflyt.avklaringsbehov.løser.sykdom

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.AvklaringsbehovLøsning
import no.nav.aap.behandlingsflyt.avklaringsbehov.VURDER_SYKEPENGEERSTATNING_KODE

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(value = VURDER_SYKEPENGEERSTATNING_KODE)
class AvklarSykepengerErstatningLøsning(
    @JsonProperty("vurdering", required = true) val vurdering: SykepengerVurdering
) :
    AvklaringsbehovLøsning
