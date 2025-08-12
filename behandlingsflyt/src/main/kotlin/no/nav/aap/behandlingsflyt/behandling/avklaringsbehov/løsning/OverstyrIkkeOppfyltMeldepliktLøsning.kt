package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.LøsningsResultat
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.OverstyrIkkeOppfyltMeldepliktLøser
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.flate.RimeligGrunnVurderingDto
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AvklaringsbehovKode
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.OVERSTYR_IKKE_OPPFYKT_MELDEPLIKT_KODE
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(value = OVERSTYR_IKKE_OPPFYKT_MELDEPLIKT_KODE)
class OverstyrIkkeOppfyltMeldepliktLøsning(
    @param:JsonProperty("rimeligGrunnVurderinger", required = true) val rimeligGrunnVurderinger: List<RimeligGrunnVurderingDto>,
    @param:JsonProperty(
        "behovstype",
        required = true,
        defaultValue = OVERSTYR_IKKE_OPPFYKT_MELDEPLIKT_KODE
    ) val behovstype: AvklaringsbehovKode = AvklaringsbehovKode.`5002`
) : AvklaringsbehovLøsning {

    override fun løs(repositoryProvider: RepositoryProvider, kontekst: AvklaringsbehovKontekst, gatewayProvider: GatewayProvider): LøsningsResultat {
        return OverstyrIkkeOppfyltMeldepliktLøser(repositoryProvider).løs(kontekst, this)
    }
}
