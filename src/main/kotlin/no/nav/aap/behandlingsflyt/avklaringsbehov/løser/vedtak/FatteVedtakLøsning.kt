package no.nav.aap.behandlingsflyt.avklaringsbehov.løser.vedtak

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.AvklaringsbehovLøsning
import no.nav.aap.behandlingsflyt.avklaringsbehov.FATTE_VEDTAK_KODE

@JsonTypeName(value = FATTE_VEDTAK_KODE)
class FatteVedtakLøsning(@JsonProperty("vurderinger", required = true) val vurderinger: List<TotrinnsVurdering>) :
    AvklaringsbehovLøsning
