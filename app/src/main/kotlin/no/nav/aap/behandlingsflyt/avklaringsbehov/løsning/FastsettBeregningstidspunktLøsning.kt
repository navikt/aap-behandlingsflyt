package no.nav.aap.behandlingsflyt.avklaringsbehov.løsning

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.behandlingsflyt.avklaringsbehov.FASTSETT_BEREGNINGSTIDSPUNKT_KODE
import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.FastsettBeregningstidspunktLøser
import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.LøsningsResultat
import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningVurdering
import no.nav.aap.verdityper.flyt.FlytKontekst

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(value = FASTSETT_BEREGNINGSTIDSPUNKT_KODE)
class FastsettBeregningstidspunktLøsning(
    @JsonProperty("beregningVurdering", required = true) val beregningVurdering: BeregningVurdering,
    @JsonProperty(
        "behovstype",
        required = true,
        defaultValue = FASTSETT_BEREGNINGSTIDSPUNKT_KODE
    ) val behovstype: String = FASTSETT_BEREGNINGSTIDSPUNKT_KODE
) : AvklaringsbehovLøsning {
    override fun løs(connection: DBConnection, kontekst: FlytKontekst): LøsningsResultat {
        return FastsettBeregningstidspunktLøser(connection).løs(kontekst, this)
    }
}
