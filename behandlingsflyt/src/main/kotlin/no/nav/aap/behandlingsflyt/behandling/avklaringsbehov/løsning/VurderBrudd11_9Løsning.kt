package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.LøsningsResultat
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.VurderBrudd11_9Løser
import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.Aktivitetsplikt11_9LøsningDto
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AvklaringsbehovKode
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.VURDER_BRUDD_11_9_KODE
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider

@JsonTypeName(value = VURDER_BRUDD_11_9_KODE)
class VurderBrudd11_9Løsning(
    @param:JsonProperty("aktivitetsplikt11_9Vurderinger", required = true)
    val aktivitetsplikt11_9Vurderinger: Set<Aktivitetsplikt11_9LøsningDto>,
    @param:JsonProperty(
        "behovstype",
        required = true,
        defaultValue = VURDER_BRUDD_11_9_KODE
    ) val behovstype: AvklaringsbehovKode = AvklaringsbehovKode.`4201`
) : AvklaringsbehovLøsning {
    override fun løs(
        repositoryProvider: RepositoryProvider,
        kontekst: AvklaringsbehovKontekst,
        gatewayProvider: GatewayProvider
    ): LøsningsResultat {
        return VurderBrudd11_9Løser(repositoryProvider).løs(kontekst, this)
    }
}
