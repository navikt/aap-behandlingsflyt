package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.AvklarForutgåendeMedlemskapLøser
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.LøsningsResultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.ManuellVurderingForForutgåendeMedlemskap
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.ManuellVurderingForForutgåendeMedlemskapDto
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AVKLAR_FORUTGÅENDE_MEDLEMSKAP_KODE
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AvklaringsbehovKode
import no.nav.aap.komponenter.dbconnect.DBConnection

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(value = AVKLAR_FORUTGÅENDE_MEDLEMSKAP_KODE)
class AvklarForutgåendeMedlemskapLøsning(
    @JsonProperty("manuellVurderingForForutgåendeMedlemskap", required = true) val manuellVurderingForForutgåendeMedlemskap: ManuellVurderingForForutgåendeMedlemskapDto,
    @JsonProperty("behovstype", required = true, defaultValue = AVKLAR_FORUTGÅENDE_MEDLEMSKAP_KODE) val behovstype: AvklaringsbehovKode = AvklaringsbehovKode.`5020`
) : AvklaringsbehovLøsning {
    override fun løs(connection: DBConnection, kontekst: AvklaringsbehovKontekst): LøsningsResultat {
        return AvklarForutgåendeMedlemskapLøser(connection).løs(kontekst, this)
    }
}
