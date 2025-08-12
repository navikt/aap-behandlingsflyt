package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.ForeslåUttakLøser
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.LøsningsResultat
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AvklaringsbehovKode
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.FORESLÅ_UTTAK_KODE
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider

@JsonTypeName(value = FORESLÅ_UTTAK_KODE)
class ForeslåUttakLøsning(
    @param:JsonProperty(
        "behovstype",
        required = true,
        defaultValue = FORESLÅ_UTTAK_KODE
    ) val behovstype: AvklaringsbehovKode = AvklaringsbehovKode.`5096`
) : AvklaringsbehovLøsning {
    override fun løs(repositoryProvider: RepositoryProvider, kontekst: AvklaringsbehovKontekst, gatewayProvider: GatewayProvider): LøsningsResultat {
        return ForeslåUttakLøser(repositoryProvider).løs(kontekst, this)
    }
}
