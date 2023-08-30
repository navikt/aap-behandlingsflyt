package no.nav.aap.avklaringsbehov.vedtak

import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.avklaringsbehov.AvklaringsbehovLøsning
import no.nav.aap.domene.behandling.avklaringsbehov.FATTE_VEDTAK_KODE

@JsonTypeName(value = FATTE_VEDTAK_KODE)
class FatteVedtakLøsning(begrunnelse: String, endretAv: String) :
    AvklaringsbehovLøsning(begrunnelse, endretAv) {

}
