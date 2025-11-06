package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.AvklarOverstyrtLovvalgMedlemskapLøser
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.LøsningsResultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.ManuellVurderingForLovvalgMedlemskapDto
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AvklaringsbehovKode
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.MANUELL_OVERSTYRING_LOVVALG
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(value = MANUELL_OVERSTYRING_LOVVALG)
class AvklarOverstyrtLovvalgMedlemskapLøsning(
    @param:JsonProperty(
        "manuellVurderingForLovvalgMedlemskap",
        required = true
    ) val manuellVurderingForLovvalgMedlemskap: ManuellVurderingForLovvalgMedlemskapDto,
    @param:JsonProperty(
        "behovstype",
        required = true,
        defaultValue = MANUELL_OVERSTYRING_LOVVALG
    ) val behovstype: AvklaringsbehovKode = AvklaringsbehovKode.`5021`
) : EnkeltAvklaringsbehovLøsning {
    override fun løs(repositoryProvider: RepositoryProvider, kontekst: AvklaringsbehovKontekst, gatewayProvider: GatewayProvider): LøsningsResultat {
        return AvklarOverstyrtLovvalgMedlemskapLøser(repositoryProvider).løs(kontekst, this)
    }
}
