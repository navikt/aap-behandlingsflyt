package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.AvbrytRevurderingLøser
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.LøsningsResultat
import no.nav.aap.behandlingsflyt.behandling.avbrytrevurdering.flate.AvbrytRevurderingVurderingDto
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AVBRYT_REVURDERING_KODE
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AvklaringsbehovKode
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider

@JsonTypeName(value = AVBRYT_REVURDERING_KODE)
class AvbrytRevurderingLøsning(
    @param:JsonProperty("behovstype", required = true, defaultValue = AVBRYT_REVURDERING_KODE)
    val behovstype: AvklaringsbehovKode = AvklaringsbehovKode.`5033`,
    val vurdering: AvbrytRevurderingVurderingDto
) : EnkeltAvklaringsbehovLøsning {
    override fun løs(repositoryProvider: RepositoryProvider, kontekst: AvklaringsbehovKontekst, gatewayProvider: GatewayProvider): LøsningsResultat {
        return AvbrytRevurderingLøser(repositoryProvider).løs(kontekst, this)
    }
}