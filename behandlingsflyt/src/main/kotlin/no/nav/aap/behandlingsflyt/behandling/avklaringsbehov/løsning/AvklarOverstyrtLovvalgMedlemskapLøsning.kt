package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.AvklarOverstyrtLovvalgMedlemskapLøser
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.LøsningsResultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.ManuellVurderingForLovvalgMedlemskap
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AvklaringsbehovKode
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.MANUELL_OVERSTYRING_LOVVALG
import no.nav.aap.komponenter.dbconnect.DBConnection

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(value = MANUELL_OVERSTYRING_LOVVALG)
class AvklarOverstyrtLovvalgMedlemskapLøsning(
    @JsonProperty("manuellVurderingForLovvalgMedlemskap", required = true) val manuellVurderingForLovvalgMedlemskap: ManuellVurderingForLovvalgMedlemskap,
    @JsonProperty("behovstype", required = true, defaultValue = MANUELL_OVERSTYRING_LOVVALG) val behovstype: AvklaringsbehovKode = AvklaringsbehovKode.`5021`
) : AvklaringsbehovLøsning {
    override fun løs(connection: DBConnection, kontekst: AvklaringsbehovKontekst): LøsningsResultat {
        return AvklarOverstyrtLovvalgMedlemskapLøser(connection).løs(kontekst, this)
    }
}
