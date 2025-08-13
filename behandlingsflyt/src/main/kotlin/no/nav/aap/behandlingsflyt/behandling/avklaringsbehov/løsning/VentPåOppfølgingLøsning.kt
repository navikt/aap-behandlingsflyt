package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.LøsningsResultat
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.VentPåOppfølgingLøser
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AvklaringsbehovKode
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.VENT_PÅ_OPPFØLGING
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider

@JsonTypeName(value = VENT_PÅ_OPPFØLGING)
class VentPåOppfølgingLøsning(
    @param:JsonProperty(
        "behovstype",
        required = true,
        defaultValue = VENT_PÅ_OPPFØLGING
    ) val behovstype: AvklaringsbehovKode = AvklaringsbehovKode.`8002`
) : AvklaringsbehovLøsning {
    override fun løs(
        repositoryProvider: RepositoryProvider,
        kontekst: AvklaringsbehovKontekst,
        gatewayProvider: GatewayProvider,
    ): LøsningsResultat {
        return VentPåOppfølgingLøser(repositoryProvider).løs(kontekst, this)
    }
}