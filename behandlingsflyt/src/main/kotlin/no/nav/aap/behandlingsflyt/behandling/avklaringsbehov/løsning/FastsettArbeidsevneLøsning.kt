package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.FastsettArbeidsevneLøser
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.LøsningsResultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsevne.flate.FastsettArbeidsevneDto
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AvklaringsbehovKode
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.FASTSETT_ARBEIDSEVNE_KODE
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(value = FASTSETT_ARBEIDSEVNE_KODE)
class FastsettArbeidsevneLøsning(
    @param:JsonProperty(
        "arbeidsevneVurderinger",
        required = true
    ) val arbeidsevneVurderinger: List<FastsettArbeidsevneDto>,
    @param:JsonProperty(
        "behovstype",
        required = true,
        defaultValue = FASTSETT_ARBEIDSEVNE_KODE
    ) val behovstype: AvklaringsbehovKode = AvklaringsbehovKode.`5004`
) : AvklaringsbehovLøsning {
    override fun løs(repositoryProvider: RepositoryProvider, kontekst: AvklaringsbehovKontekst, gatewayProvider: GatewayProvider): LøsningsResultat {
        return FastsettArbeidsevneLøser(repositoryProvider).løs(kontekst, this)
    }
}
