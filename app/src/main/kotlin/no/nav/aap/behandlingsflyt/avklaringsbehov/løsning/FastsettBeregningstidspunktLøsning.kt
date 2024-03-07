package no.nav.aap.behandlingsflyt.avklaringsbehov.løsning

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.behandlingsflyt.avklaringsbehov.FASTSETT_BEREGNINGSTIDSPUNKT_KODE
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningVurdering

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(value = FASTSETT_BEREGNINGSTIDSPUNKT_KODE)
class FastsettBeregningstidspunktLøsning(
    @JsonProperty("vurdering", required = true) val vurdering: BeregningVurdering
) : AvklaringsbehovLøsning
