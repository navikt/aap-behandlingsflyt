package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.LøsningsResultat
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.VentPåOppfølgingNyLøser
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AvklaringsbehovKode
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.VENT_PÅ_OPPFØLGING_NY_KODE
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider

@JsonTypeName(value = VENT_PÅ_OPPFØLGING_NY_KODE)
class VentPåOppfølgingNyLøsning(
    @param:JsonProperty(
        "behovstype",
        required = true,
        defaultValue = VENT_PÅ_OPPFØLGING_NY_KODE
    ) val behovstype: AvklaringsbehovKode = AvklaringsbehovKode.`8004`
) : EnkeltAvklaringsbehovLøsning {
    override fun løs(
        repositoryProvider: RepositoryProvider,
        kontekst: AvklaringsbehovKontekst,
        gatewayProvider: GatewayProvider,
    ): LøsningsResultat {
        return VentPåOppfølgingNyLøser(repositoryProvider).løs(kontekst, this)
    }
}