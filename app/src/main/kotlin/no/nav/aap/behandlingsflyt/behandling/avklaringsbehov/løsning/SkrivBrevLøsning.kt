package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.LøsningsResultat
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.SkrivBrevLøser
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.SKRIV_BREV_KODE
import no.nav.aap.komponenter.dbconnect.DBConnection
import java.util.UUID

@JsonTypeName(value = SKRIV_BREV_KODE)
class SkrivBrevLøsning(
    @JsonProperty("brevbestillingReferanse", required = true) val brevbestillingReferanse: UUID,
    @JsonProperty(
        "behovstype",
        required = true,
        defaultValue = SKRIV_BREV_KODE
    ) val behovstype: String = SKRIV_BREV_KODE
) : AvklaringsbehovLøsning {
    override fun løs(connection: DBConnection, kontekst: AvklaringsbehovKontekst): LøsningsResultat {
        return SkrivBrevLøser(connection).løs(kontekst, this)
    }
}
