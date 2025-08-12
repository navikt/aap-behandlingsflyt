package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.AvklarLovvalgMedlemskapLøser
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.LøsningsResultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.ManuellVurderingForLovvalgMedlemskapDto
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AVKLAR_LOVVALG_MEDLEMSKAP_KODE
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AvklaringsbehovKode
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(value = AVKLAR_LOVVALG_MEDLEMSKAP_KODE)
class AvklarLovvalgMedlemskapLøsning(
    @param:JsonProperty(
        "manuellVurderingForLovvalgMedlemskap",
        required = true
    ) val manuellVurderingForLovvalgMedlemskap: ManuellVurderingForLovvalgMedlemskapDto,
    @param:JsonProperty(
        "behovstype",
        required = true,
        defaultValue = AVKLAR_LOVVALG_MEDLEMSKAP_KODE
    ) val behovstype: AvklaringsbehovKode = AvklaringsbehovKode.`5017`
) : AvklaringsbehovLøsning {
    override fun løs(repositoryProvider: RepositoryProvider, kontekst: AvklaringsbehovKontekst, gatewayProvider: GatewayProvider): LøsningsResultat {
        return AvklarLovvalgMedlemskapLøser(repositoryProvider).løs(kontekst, this)
    }
}
