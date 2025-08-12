package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.FastsettBeregningstidspunktLøser
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.LøsningsResultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningstidspunktVurderingDto
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AvklaringsbehovKode
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.FASTSETT_BEREGNINGSTIDSPUNKT_KODE
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(value = FASTSETT_BEREGNINGSTIDSPUNKT_KODE)
class FastsettBeregningstidspunktLøsning(
    @param:JsonProperty("beregningVurdering", required = true) val beregningVurdering: BeregningstidspunktVurderingDto,
    @param:JsonProperty(
        "behovstype",
        required = true,
        defaultValue = FASTSETT_BEREGNINGSTIDSPUNKT_KODE
    ) val behovstype: AvklaringsbehovKode = AvklaringsbehovKode.`5008`
) : AvklaringsbehovLøsning {
    override fun løs(repositoryProvider: RepositoryProvider, kontekst: AvklaringsbehovKontekst, gatewayProvider: GatewayProvider): LøsningsResultat {
        return FastsettBeregningstidspunktLøser(repositoryProvider).løs(kontekst, this)
    }
}
