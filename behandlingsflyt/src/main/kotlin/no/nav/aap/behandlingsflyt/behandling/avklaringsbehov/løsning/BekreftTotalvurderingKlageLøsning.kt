package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.BekreftTotalvurderingKlageLøser
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.LøsningsResultat
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AvklaringsbehovKode
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.BEKREFT_TOTALVURDERING_KLAGE
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider

@JsonTypeName(value = BEKREFT_TOTALVURDERING_KLAGE)
class BekreftTotalvurderingKlageLøsning(
    @param:JsonProperty(
        "behovstype",
        required = true,
        defaultValue = BEKREFT_TOTALVURDERING_KLAGE
    ) val behovstype: AvklaringsbehovKode = AvklaringsbehovKode.`6006`
) : EnkeltAvklaringsbehovLøsning {
    override fun løs(repositoryProvider: RepositoryProvider, kontekst: AvklaringsbehovKontekst, gatewayProvider: GatewayProvider): LøsningsResultat {
        return BekreftTotalvurderingKlageLøser(repositoryProvider).løs(kontekst, this)
    }
}
