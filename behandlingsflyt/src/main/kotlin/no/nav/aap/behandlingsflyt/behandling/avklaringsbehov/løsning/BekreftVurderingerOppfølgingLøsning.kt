package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.BekreftVurderingerOppfølgingLøser
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.LøsningsResultat
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AvklaringsbehovKode
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.BEKREFT_VURDERINGER_OPPFØLGING_KODE
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(value = BEKREFT_VURDERINGER_OPPFØLGING_KODE)
class BekreftVurderingerOppfølgingLøsning(
    @param:JsonProperty(
        "behovstype",
        required = true,
        defaultValue = BEKREFT_VURDERINGER_OPPFØLGING_KODE
    ) val behovstype: AvklaringsbehovKode = AvklaringsbehovKode.`5054`
) : EnkeltAvklaringsbehovLøsning {
    override fun løs(repositoryProvider: RepositoryProvider, kontekst: AvklaringsbehovKontekst, gatewayProvider: GatewayProvider): LøsningsResultat {
        return BekreftVurderingerOppfølgingLøser(repositoryProvider).løs(kontekst, this)
    }
}