package no.nav.aap.behandlingsflyt.avklaringsbehov.løsning

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.behandlingsflyt.avklaringsbehov.FORESLÅ_VEDTAK_KODE
import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.ForeslåVedtakLøser
import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.LøsningsResultat
import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.verdityper.flyt.FlytKontekst

@JsonTypeName(value = FORESLÅ_VEDTAK_KODE)
class ForeslåVedtakLøsning(
    @JsonProperty("foreslåvedtakVurdering", required = true) val foreslåvedtakVurdering: String,
    @JsonProperty(
        "behovstype",
        required = true,
        defaultValue = FORESLÅ_VEDTAK_KODE
    ) val behovstype: String = FORESLÅ_VEDTAK_KODE
) : AvklaringsbehovLøsning {
    override fun løs(connection: DBConnection, kontekst: FlytKontekst): LøsningsResultat {
        return ForeslåVedtakLøser(connection).løs(kontekst, this)
    }
}
