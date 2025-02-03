package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.AvklarLovvalgMedlemskapLøser
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.LøsningsResultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.VurderingerForLovvalgMedlemskap
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AVKLAR_LOVVALG_MEDLEMSKAP_KODE
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AvklaringsbehovKode
import no.nav.aap.komponenter.dbconnect.DBConnection

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(value = AVKLAR_LOVVALG_MEDLEMSKAP_KODE)
class AvklarLovvalgMedlemskapLøsning(
    @JsonProperty("vurderingerForLovvalgMedlemskap", required = true) val vurderingerForLovvalgMedlemskap: VurderingerForLovvalgMedlemskap,
    @JsonProperty("behovstype", required = true, defaultValue = AVKLAR_LOVVALG_MEDLEMSKAP_KODE) val behovstype: AvklaringsbehovKode = AvklaringsbehovKode.`5017`
) : AvklaringsbehovLøsning {
    override fun løs(connection: DBConnection, kontekst: AvklaringsbehovKontekst): LøsningsResultat {
        return AvklarLovvalgMedlemskapLøser(connection).løs(kontekst, this)
    }
}
