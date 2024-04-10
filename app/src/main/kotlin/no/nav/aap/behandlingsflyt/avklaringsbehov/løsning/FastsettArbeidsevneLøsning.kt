package no.nav.aap.behandlingsflyt.avklaringsbehov.løsning

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.behandlingsflyt.avklaringsbehov.FASTSETT_ARBEIDSEVNE_KODE
import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.FastsettArbeidsevneLøser
import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.LøsningsResultat
import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsevne.Arbeidsevne
import no.nav.aap.verdityper.flyt.FlytKontekst

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(value = FASTSETT_ARBEIDSEVNE_KODE)
class FastsettArbeidsevneLøsning(
    @JsonProperty("arbeidsevneVurdering", required = true) val arbeidsevne: Arbeidsevne,
    @JsonProperty(
        "behovstype",
        required = true,
        defaultValue = FASTSETT_ARBEIDSEVNE_KODE
    ) val behovstype: String = FASTSETT_ARBEIDSEVNE_KODE
) : AvklaringsbehovLøsning {
    override fun løs(connection: DBConnection, kontekst: FlytKontekst): LøsningsResultat {
        return FastsettArbeidsevneLøser(connection).løs(kontekst, this)
    }
}
