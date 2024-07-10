package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AVKLAR_SONINGSFORRHOLD_KODE
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.AvklarsoningsforholdLøser
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.LøsningsResultat
import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon.flate.SoningsvurderingDto

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(value = AVKLAR_SONINGSFORRHOLD_KODE)
class AvklarSoningsforholdLøsning(
    @JsonProperty("soningsvurdering", required = true) val soningsvurdering: SoningsvurderingDto,
    @JsonProperty(
        "behovstype",
        required = true,
        defaultValue = AVKLAR_SONINGSFORRHOLD_KODE
    ) val behovstype: String = AVKLAR_SONINGSFORRHOLD_KODE
) :
    AvklaringsbehovLøsning {
    override fun løs(connection: DBConnection, kontekst: AvklaringsbehovKontekst): LøsningsResultat {
        return AvklarsoningsforholdLøser(connection).løs(kontekst, this)
    }

}
