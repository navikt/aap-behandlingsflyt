package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.FastsettPåklagetBehandlingLøser
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.LøsningsResultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.påklagetbehandling.PåklagetBehandlingVurderingLøsningDto
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AvklaringsbehovKode
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.FASTSETT_PÅKLAGET_BEHANDLING_KODE
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(value = FASTSETT_PÅKLAGET_BEHANDLING_KODE)
class FastsettPåklagetBehandlingLøsning(
    @param:JsonProperty("påklagetBehandlingVurdering", required = true)
    val påklagetBehandlingVurdering: PåklagetBehandlingVurderingLøsningDto,
    @param:JsonProperty(
        "behovstype",
        required = true,
        defaultValue = FASTSETT_PÅKLAGET_BEHANDLING_KODE
    ) val behovstype: AvklaringsbehovKode = AvklaringsbehovKode.`5999`
) : AvklaringsbehovLøsning {
    override fun løs(repositoryProvider: RepositoryProvider, kontekst: AvklaringsbehovKontekst, gatewayProvider: GatewayProvider): LøsningsResultat {
        return FastsettPåklagetBehandlingLøser(repositoryProvider).løs(kontekst, this)
    }
}