package no.nav.aap.behandlingsflyt.avklaringsbehov.løser

import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.behandlingsflyt.avklaringsbehov.MANUELT_SATT_PÅ_VENT_KODE

@JsonTypeName(value = MANUELT_SATT_PÅ_VENT_KODE)
class SattPåVentLøsning: AvklaringsbehovLøsning