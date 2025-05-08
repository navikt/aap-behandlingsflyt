package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.LøsningsResultat
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.VurderFormkravLøser
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.formkrav.flate.FormkravVurderingLøsningDto
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AvklaringsbehovKode
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.VURDER_FORMKRAV_KODE
import no.nav.aap.komponenter.dbconnect.DBConnection

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(value = VURDER_FORMKRAV_KODE)
class VurderFormkravLøsning(
    @JsonProperty("formkravVurdering", required = true)
    val formkravVurdering: FormkravVurderingLøsningDto,
    @JsonProperty(
        "behovstype",
        required = true,
        defaultValue = VURDER_FORMKRAV_KODE
    ) val behovstype: AvklaringsbehovKode = AvklaringsbehovKode.`6000`
) : AvklaringsbehovLøsning {
    override fun løs(connection: DBConnection, kontekst: AvklaringsbehovKontekst): LøsningsResultat {
        return VurderFormkravLøser(connection).løs(kontekst, this)
    }
}