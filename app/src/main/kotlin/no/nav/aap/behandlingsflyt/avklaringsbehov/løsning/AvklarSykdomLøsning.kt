package no.nav.aap.behandlingsflyt.avklaringsbehov.løsning

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.behandlingsflyt.avklaringsbehov.AVKLAR_SYKDOM_KODE
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.flate.SykdomsvurderingDto

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(value = AVKLAR_SYKDOM_KODE)
class AvklarSykdomLøsning(
    @JsonProperty("sykdomsvurdering", required = true) val sykdomsvurdering: SykdomsvurderingDto
) : AvklaringsbehovLøsning
