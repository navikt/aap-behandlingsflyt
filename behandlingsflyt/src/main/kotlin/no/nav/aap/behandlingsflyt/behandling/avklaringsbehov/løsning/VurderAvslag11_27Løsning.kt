package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.LøsningsResultat
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.VurderAvslag11_27Løser
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.avslag11_27.flate.Avslag11_27VurderingerDto
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AvklaringsbehovKode
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.VURDER_AVSLAG_11_27_KODE
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(value = VURDER_AVSLAG_11_27_KODE)
class VurderAvslag11_27Løsning(
    @param:JsonProperty(
        "avslag11_27Vurdering",
        required = true
    ) val avslag11_27Vurdering: Avslag11_27VurderingerDto,
    @param:JsonProperty(
        "behovstype",
        required = true,
        defaultValue = VURDER_AVSLAG_11_27_KODE
    ) val behovstype: AvklaringsbehovKode = AvklaringsbehovKode.`5042`
) :
    EnkeltAvklaringsbehovLøsning {
    override fun løs(
        repositoryProvider: RepositoryProvider,
        kontekst: AvklaringsbehovKontekst,
        gatewayProvider: GatewayProvider
    ): LøsningsResultat {
        return VurderAvslag11_27Løser(repositoryProvider).løs(kontekst, this)
    }
}