package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.KansellerRevurderingLøser
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.LøsningsResultat
import no.nav.aap.behandlingsflyt.behandling.kansellerrevurdering.flate.KansellerRevurderingVurderingDto
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.KANSELLER_REVURDERING_KODE
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AvklaringsbehovKode
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider

@JsonTypeName(value = KANSELLER_REVURDERING_KODE)
class KansellerRevurderingLøsning(
    @param:JsonProperty("behovstype", required = true, defaultValue = KANSELLER_REVURDERING_KODE)
    val behovstype: AvklaringsbehovKode = AvklaringsbehovKode.`5033`,
    val vurdering: KansellerRevurderingVurderingDto
) : AvklaringsbehovLøsning {
    override fun løs(repositoryProvider: RepositoryProvider, kontekst: AvklaringsbehovKontekst, gatewayProvider: GatewayProvider): LøsningsResultat {
        return KansellerRevurderingLøser(repositoryProvider).løs(kontekst, this)
    }
}