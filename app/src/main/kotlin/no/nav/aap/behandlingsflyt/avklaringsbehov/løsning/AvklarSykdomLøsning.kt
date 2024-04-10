package no.nav.aap.behandlingsflyt.avklaringsbehov.løsning

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.behandlingsflyt.avklaringsbehov.AVKLAR_SYKDOM_KODE
import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.AvklarSykdomLøser
import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.LøsningsResultat
import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.flate.SykdomsvurderingDto
import no.nav.aap.verdityper.flyt.FlytKontekst

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(value = AVKLAR_SYKDOM_KODE)
class AvklarSykdomLøsning(
    @JsonProperty("sykdomsvurdering", required = true) val sykdomsvurdering: SykdomsvurderingDto,
    @JsonProperty(
        "behovstype",
        required = true,
        defaultValue = AVKLAR_SYKDOM_KODE
    ) val behovstype: String = AVKLAR_SYKDOM_KODE
) : AvklaringsbehovLøsning {
    override fun løs(connection: DBConnection, kontekst: FlytKontekst): LøsningsResultat {
        return AvklarSykdomLøser(connection).løs(kontekst, this)
    }
}
