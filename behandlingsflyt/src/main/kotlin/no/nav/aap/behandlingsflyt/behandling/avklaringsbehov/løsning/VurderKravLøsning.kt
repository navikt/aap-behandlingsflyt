package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.LøsningsResultat
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.VurderKravLøser
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.KravVurderingLøsningDto
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AvklaringsbehovKode
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.VURDER_KRAV_KODE
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider

@JsonTypeName(value = VURDER_KRAV_KODE)
class VurderKravLøsning(
    @param:JsonProperty("kravVurderinger", required = true)
    val kravVurderinger: Set<KravVurderingLøsningDto>,
    @param:JsonProperty(
        "behovstype",
        required = true,
        defaultValue = VURDER_KRAV_KODE
    ) val behovstype: AvklaringsbehovKode = AvklaringsbehovKode.`5037`
) : EnkeltAvklaringsbehovLøsning {
    override fun løs(
        repositoryProvider: RepositoryProvider,
        kontekst: AvklaringsbehovKontekst,
        gatewayProvider: GatewayProvider
    ): LøsningsResultat {
        return VurderKravLøser(repositoryProvider).løs(kontekst, this)
    }
}
