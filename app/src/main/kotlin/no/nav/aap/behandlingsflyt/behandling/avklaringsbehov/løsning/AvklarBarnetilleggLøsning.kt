package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AVKLAR_BARNETILLEGG_KODE
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.AvklarBarnetilleggLøser
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.LøsningsResultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn.VurderingerForBarnetillegg
import no.nav.aap.komponenter.dbconnect.DBConnection

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(value = AVKLAR_BARNETILLEGG_KODE)
class AvklarBarnetilleggLøsning(
    @JsonProperty("vurderingerForBarnetillegg", required = true) val vurderingerForBarnetillegg: VurderingerForBarnetillegg,
    @JsonProperty("behovstype", required = true, defaultValue = AVKLAR_BARNETILLEGG_KODE) val behovstype: String = AVKLAR_BARNETILLEGG_KODE
) : AvklaringsbehovLøsning {
    override fun løs(connection: DBConnection, kontekst: AvklaringsbehovKontekst): LøsningsResultat {
        return AvklarBarnetilleggLøser(connection).løs(kontekst, this)
    }
}
