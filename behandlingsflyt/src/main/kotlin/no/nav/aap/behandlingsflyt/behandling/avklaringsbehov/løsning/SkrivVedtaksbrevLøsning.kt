package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.LøsningsResultat
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.SkrivVedtaksbrevLøser
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AvklaringsbehovKode
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.SKRIV_VEDTAKSBREV_KODE
import no.nav.aap.komponenter.dbconnect.DBConnection
import java.util.*

@JsonTypeName(value = SKRIV_VEDTAKSBREV_KODE)
class SkrivVedtaksbrevLøsning(
    @JsonProperty("brevbestillingReferanse", required = true) val brevbestillingReferanse: UUID,
    @JsonProperty("handling", required = true) val handling: SkrivBrevAvklaringsbehovLøsning.Handling,
    @JsonProperty("behovstype", required = true, defaultValue = SKRIV_VEDTAKSBREV_KODE)
    val behovstype: AvklaringsbehovKode = AvklaringsbehovKode.`5051`
) : AvklaringsbehovLøsning {
    override fun løs(connection: DBConnection, kontekst: AvklaringsbehovKontekst): LøsningsResultat {
        return SkrivVedtaksbrevLøser(connection).løs(kontekst, this)
    }
}
