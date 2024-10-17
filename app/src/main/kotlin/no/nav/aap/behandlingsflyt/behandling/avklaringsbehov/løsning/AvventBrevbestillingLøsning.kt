package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.AvventBrevbestillingLøser
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.LøsningsResultat
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AVVENTER_BREV_BESTILLING_KODE
import no.nav.aap.behandlingsflyt.kontrakt.brev.BrevbestillingStatusDto
import no.nav.aap.komponenter.dbconnect.DBConnection

@JsonTypeName(value = AVVENTER_BREV_BESTILLING_KODE)
class AvventBrevbestillingLøsning(
    @JsonProperty("status", required = true) val brevbestillingStatus: BrevbestillingStatusDto,
    @JsonProperty(
        "behovstype",
        required = true,
        defaultValue = AVVENTER_BREV_BESTILLING_KODE
    ) val behovstype: String = AVVENTER_BREV_BESTILLING_KODE
) : AvklaringsbehovLøsning {
    override fun løs(connection: DBConnection, kontekst: AvklaringsbehovKontekst): LøsningsResultat {
        return AvventBrevbestillingLøser(connection).løs(kontekst, this)
    }
}
