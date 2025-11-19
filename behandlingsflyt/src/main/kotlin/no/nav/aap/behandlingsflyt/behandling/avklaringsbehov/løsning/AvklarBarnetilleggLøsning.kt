package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.AvklarBarnetilleggLøser
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.LøsningsResultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn.VurderingerForBarnetillegg
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AVKLAR_BARNETILLEGG_KODE
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AvklaringsbehovKode
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(value = AVKLAR_BARNETILLEGG_KODE)
class AvklarBarnetilleggLøsning(
    @param:JsonProperty(
        "vurderingerForBarnetillegg",
        required = true
    ) val vurderingerForBarnetillegg: VurderingerForBarnetillegg,
    @param:JsonProperty("behovstype", required = true, defaultValue = AVKLAR_BARNETILLEGG_KODE) val behovstype: AvklaringsbehovKode = AvklaringsbehovKode.`5009`
) : EnkeltAvklaringsbehovLøsning {
    override fun løs(repositoryProvider: RepositoryProvider, kontekst: AvklaringsbehovKontekst, gatewayProvider: GatewayProvider): LøsningsResultat {
        return AvklarBarnetilleggLøser(repositoryProvider, gatewayProvider).løs(kontekst, this)
    }
}
