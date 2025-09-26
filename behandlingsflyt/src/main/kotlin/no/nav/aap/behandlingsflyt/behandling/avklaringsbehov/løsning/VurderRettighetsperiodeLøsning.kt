package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.LøsningsResultat
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.VurderRettighetsperiodeLøser
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.rettighetsperiode.RettighetsperiodeVurderingDTO
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AvklaringsbehovKode
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.VURDER_RETTIGHETSPERIODE_KODE
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider

@JsonTypeName(value = VURDER_RETTIGHETSPERIODE_KODE)
class VurderRettighetsperiodeLøsning(
    @param:JsonProperty(
        "rettighetsperiodeVurdering",
        required = true
    ) val rettighetsperiodeVurdering: RettighetsperiodeVurderingDTO,
    @param:JsonProperty("behovstype", required = true, defaultValue = VURDER_RETTIGHETSPERIODE_KODE)
    val behovstype: AvklaringsbehovKode = AvklaringsbehovKode.`5029`
) : EnkeltAvklaringsbehovLøsning {
    override fun løs(repositoryProvider: RepositoryProvider, kontekst: AvklaringsbehovKontekst, gatewayProvider: GatewayProvider): LøsningsResultat {
        return VurderRettighetsperiodeLøser(repositoryProvider, gatewayProvider).løs(kontekst, this)
    }

}
