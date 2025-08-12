package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.FatteVedtakLøser
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.LøsningsResultat
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.vedtak.TotrinnsVurdering
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AvklaringsbehovKode
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.FATTE_VEDTAK_KODE
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider

@JsonTypeName(value = FATTE_VEDTAK_KODE)
class FatteVedtakLøsning(
    @param:JsonProperty("vurderinger", required = true) val vurderinger: List<TotrinnsVurdering>,
    @param:JsonProperty(
        "behovstype",
        required = true,
        defaultValue = FATTE_VEDTAK_KODE
    ) val behovstype: AvklaringsbehovKode = AvklaringsbehovKode.`5099`
) : AvklaringsbehovLøsning {
    override fun løs(repositoryProvider: RepositoryProvider, kontekst: AvklaringsbehovKontekst, gatewayProvider: GatewayProvider): LøsningsResultat {
        return FatteVedtakLøser(repositoryProvider, gatewayProvider).løs(kontekst, this)
    }
}
