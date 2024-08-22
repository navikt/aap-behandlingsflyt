package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AVKLAR_BISTANDSBEHOV_KODE
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.AvklarBistandLøser
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.LøsningsResultat
import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.flate.BistandVurderingDto


@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(value = AVKLAR_BISTANDSBEHOV_KODE)
class AvklarBistandsbehovLøsning(
    @JsonProperty("bistandsVurdering", required = true)
    val bistandsVurderingDto: BistandVurderingDto,
    @JsonProperty(
        "behovstype",
        required = true,
        defaultValue = AVKLAR_BISTANDSBEHOV_KODE
    ) val behovstype: String = AVKLAR_BISTANDSBEHOV_KODE
) : AvklaringsbehovLøsning {
    override fun løs(connection: DBConnection, kontekst: AvklaringsbehovKontekst): LøsningsResultat {
        return AvklarBistandLøser(connection).løs(kontekst, this)
    }
}


