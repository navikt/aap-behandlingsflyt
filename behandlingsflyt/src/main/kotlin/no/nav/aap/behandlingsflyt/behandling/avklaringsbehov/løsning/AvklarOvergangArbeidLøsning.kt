package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.AvklarOvergangArbeidLøser
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.LøsningsResultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangarbeid.flate.OvergangArbeidVurderingLøsningDto
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AVKLAR_OVERGANG_ARBEID
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AvklaringsbehovKode
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider


@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(value = AVKLAR_OVERGANG_ARBEID)
class AvklarOvergangArbeidLøsning(
    @param:JsonProperty("overgangArbeidVurdering", required = true)
    val overgangArbeidVurdering: OvergangArbeidVurderingLøsningDto,
    @param:JsonProperty(
        "behovstype",
        required = true,
        defaultValue = AVKLAR_OVERGANG_ARBEID
    ) val behovstype: AvklaringsbehovKode = AvklaringsbehovKode.`5032`
) : EnkeltAvklaringsbehovLøsning {
    override fun løs(repositoryProvider: RepositoryProvider, kontekst: AvklaringsbehovKontekst, gatewayProvider: GatewayProvider): LøsningsResultat {
        return AvklarOvergangArbeidLøser(repositoryProvider).løs(kontekst, this)
    }
}
