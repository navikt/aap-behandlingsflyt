package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.AvklarOverstyrtForutgåendeMedlemskapLøser
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.LøsningsResultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.ManuellVurderingForForutgåendeMedlemskapDto
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AvklaringsbehovKode
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.MANUELL_OVERSTYRING_MEDLEMSKAP
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(value = MANUELL_OVERSTYRING_MEDLEMSKAP)
class AvklarOverstyrtForutgåendeMedlemskapLøsning(
    @param:JsonProperty(
        "manuellVurderingForForutgåendeMedlemskap",
        required = true
    ) val manuellVurderingForForutgåendeMedlemskap: ManuellVurderingForForutgåendeMedlemskapDto,
    @param:JsonProperty(
        "behovstype",
        required = true,
        defaultValue = MANUELL_OVERSTYRING_MEDLEMSKAP
    ) val behovstype: AvklaringsbehovKode = AvklaringsbehovKode.`5022`
) : EnkeltAvklaringsbehovLøsning {
    override fun løs(repositoryProvider: RepositoryProvider, kontekst: AvklaringsbehovKontekst, gatewayProvider: GatewayProvider): LøsningsResultat {
        return AvklarOverstyrtForutgåendeMedlemskapLøser(repositoryProvider).løs(kontekst, this)
    }
}
