package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.ForeslåVedtakVedtakslengdeLøser
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.LøsningsResultat
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AvklaringsbehovKode
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.FORESLÅ_VEDTAK_VEDTAKSLENGDE_KODE
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider

@JsonTypeName(value = FORESLÅ_VEDTAK_VEDTAKSLENGDE_KODE)
class ForeslåVedtakVedtakslengdeLøsning(
    @param:JsonProperty(
        "behovstype",
        required = true,
        defaultValue = FORESLÅ_VEDTAK_VEDTAKSLENGDE_KODE
    ) val behovstype: AvklaringsbehovKode = AvklaringsbehovKode.`5060`
) : EnkeltAvklaringsbehovLøsning {
    override fun løs(repositoryProvider: RepositoryProvider, kontekst: AvklaringsbehovKontekst, gatewayProvider: GatewayProvider): LøsningsResultat {
        return ForeslåVedtakVedtakslengdeLøser(repositoryProvider).løs(kontekst, this)
    }
}

