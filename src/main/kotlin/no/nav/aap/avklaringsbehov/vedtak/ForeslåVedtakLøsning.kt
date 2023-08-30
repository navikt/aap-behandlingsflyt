package no.nav.aap.avklaringsbehov.vedtak

import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.avklaringsbehov.AvklaringsbehovLøsning
import no.nav.aap.domene.behandling.avklaringsbehov.FORESLÅ_VEDTAK_KODE

@JsonTypeName(value = FORESLÅ_VEDTAK_KODE)
class ForeslåVedtakLøsning(begrunnelse: String, endretAv: String) :
    AvklaringsbehovLøsning(begrunnelse, endretAv) {

}
