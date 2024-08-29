package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.FASTSETT_ARBEIDSEVNE_KODE
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.FastsettArbeidsevneLøser
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.LøsningsResultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsevne.Arbeidsevne
import no.nav.aap.komponenter.dbconnect.DBConnection

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
    override fun løs(connection: DBConnection, kontekst: AvklaringsbehovKontekst): LøsningsResultat {
        return FastsettArbeidsevneLøser(connection).løs(kontekst, this)
    }
}
