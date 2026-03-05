package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.AvklarSamordningBarnepensjonLøser
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.LøsningsResultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barnepensjon.BarnepensjonLøsningDto
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AVKLAR_SAMORDNING_BARNEPENSJON_KODE
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AvklaringsbehovKode
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider

@Suppress("unused") // Reflection
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(value = AVKLAR_SAMORDNING_BARNEPENSJON_KODE)
class AvklarSamordningBarnepensjonLøsning(
    @param:JsonProperty(
        "barnepensjonVurdering",
        required = true
    ) val barnepensjonVurdering: BarnepensjonLøsningDto,
    @param:JsonProperty(
        "behovstype",
        required = true,
        defaultValue = AVKLAR_SAMORDNING_BARNEPENSJON_KODE
    ) val behovstype: AvklaringsbehovKode = AvklaringsbehovKode.`5036`
) : EnkeltAvklaringsbehovLøsning {
    override fun løs(repositoryProvider: RepositoryProvider, kontekst: AvklaringsbehovKontekst, gatewayProvider: GatewayProvider): LøsningsResultat {
        return AvklarSamordningBarnepensjonLøser(repositoryProvider).løs(kontekst, this)
    }
}