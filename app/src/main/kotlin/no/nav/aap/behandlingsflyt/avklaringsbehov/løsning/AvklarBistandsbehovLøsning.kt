package no.nav.aap.behandlingsflyt.avklaringsbehov.løsning

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.behandlingsflyt.avklaringsbehov.AVKLAR_BISTANDSBEHOV_KODE
import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.AvklarBistandLøser
import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.LøsningsResultat
import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.BistandVurdering
import no.nav.aap.verdityper.flyt.FlytKontekst

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(value = AVKLAR_BISTANDSBEHOV_KODE)
class AvklarBistandsbehovLøsning(
    @JsonProperty("bistandsVurdering", required = true) val bistandsVurdering: BistandVurdering,
    @JsonProperty(
        "behovstype",
        required = true,
        defaultValue = AVKLAR_BISTANDSBEHOV_KODE
    ) val behovstype: String = AVKLAR_BISTANDSBEHOV_KODE
) : AvklaringsbehovLøsning {
    override fun løs(connection: DBConnection, kontekst: FlytKontekst): LøsningsResultat {
        return AvklarBistandLøser(connection).løs(kontekst, this)
    }
}
