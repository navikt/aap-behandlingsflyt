package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.LøsningsResultat
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.VurderInntektsbortfallLøser
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.InntektsbortfallVurderingDto
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AvklaringsbehovKode
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.VURDER_INNTEKTSBORTFALL
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(value = VURDER_INNTEKTSBORTFALL)
class VurderInntektsbortfallLøsning(
    @param:JsonProperty("vurdering", required = true) val vurdering: InntektsbortfallVurderingDto,
    @param:JsonProperty(
        "behovstype",
        required = true,
        defaultValue = VURDER_INNTEKTSBORTFALL
    ) val behovstype: AvklaringsbehovKode = AvklaringsbehovKode.`5040`
) : EnkeltAvklaringsbehovLøsning {
    override fun løs(
        repositoryProvider: RepositoryProvider,
        kontekst: AvklaringsbehovKontekst,
        gatewayProvider: GatewayProvider
    ): LøsningsResultat {
        return VurderInntektsbortfallLøser(repositoryProvider).løs(kontekst, this)
    }
}