package no.nav.aap.avklaringsbehov.yrkesskade

import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.avklaringsbehov.AvklaringsbehovLøsning
import no.nav.aap.domene.behandling.avklaringsbehov.AVKLAR_YRKESSKADE_KODE

@JsonTypeName(value = AVKLAR_YRKESSKADE_KODE)
class AvklarYrkesskadeLøsning(begrunnelse: String, endretAv: String) :
    AvklaringsbehovLøsning(begrunnelse, endretAv) {

}
