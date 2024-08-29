package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning

import HelseinstitusjonVurderingDto
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AVKLAR_HELSEINSTITUSJON_KODE
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AVKLAR_SONINGSFORRHOLD_KODE
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.AvklarHelseinstitusjonLøser
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.LøsningsResultat
import no.nav.aap.komponenter.dbconnect.DBConnection

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(value = AVKLAR_HELSEINSTITUSJON_KODE)
class AvklarHelseinstitusjonLøsning(
    @JsonProperty(
        "helseinstitusjonVurdering",
        required = true
    ) val helseinstitusjonVurdering: HelseinstitusjonVurderingDto,
    @JsonProperty(
        "behovstype",
        required = true,
        defaultValue = AVKLAR_HELSEINSTITUSJON_KODE
    ) val behovstype: String = AVKLAR_SONINGSFORRHOLD_KODE
) :
    AvklaringsbehovLøsning {
    override fun løs(connection: DBConnection, kontekst: AvklaringsbehovKontekst): LøsningsResultat {
        return AvklarHelseinstitusjonLøser(connection).løs(kontekst, this)
    }
}