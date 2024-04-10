package no.nav.aap.behandlingsflyt.avklaringsbehov.løsning

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.behandlingsflyt.avklaringsbehov.MANUELT_SATT_PÅ_VENT_KODE
import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.LøsningsResultat
import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.SattPåVentLøser
import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.verdityper.flyt.FlytKontekst

@JsonTypeName(value = MANUELT_SATT_PÅ_VENT_KODE)
class SattPåVentLøsning(
    @JsonProperty(
        "behovstype",
        required = true,
        defaultValue = MANUELT_SATT_PÅ_VENT_KODE
    ) val behovstype: String = MANUELT_SATT_PÅ_VENT_KODE
) : AvklaringsbehovLøsning {
    override fun løs(connection: DBConnection, kontekst: FlytKontekst): LøsningsResultat {
        return SattPåVentLøser(connection).løs(kontekst, this)
    }
}