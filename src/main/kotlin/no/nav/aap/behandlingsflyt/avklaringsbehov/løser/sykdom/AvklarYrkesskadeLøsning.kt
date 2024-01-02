package no.nav.aap.behandlingsflyt.avklaringsbehov.løser.sykdom

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.AvklaringsbehovLøsning
import no.nav.aap.behandlingsflyt.avklaringsbehov.AVKLAR_YRKESSKADE_KODE

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(value = AVKLAR_YRKESSKADE_KODE)
class AvklarYrkesskadeLøsning(
    @JsonProperty("yrkesskadevurdering", required = true) val yrkesskadevurdering: Yrkesskadevurdering
) :
    AvklaringsbehovLøsning
