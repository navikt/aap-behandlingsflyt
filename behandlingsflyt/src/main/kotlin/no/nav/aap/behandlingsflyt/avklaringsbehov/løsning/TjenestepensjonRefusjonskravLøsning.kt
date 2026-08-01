package no.nav.aap.behandlingsflyt.avklaringsbehov.løsning

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.behandlingsflyt.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.LøsningsResultat
import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.TjenestepensjonRefusjonskravLøser
import no.nav.aap.samordning.refusjonskrav.TjenestepensjonRefusjonskravVurdering
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AvklaringsbehovKode
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.SAMORDNING_REFUSJONS_KRAV
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(value = SAMORDNING_REFUSJONS_KRAV)
class TjenestepensjonRefusjonskravLøsning (
    @param:JsonProperty(
        "samordningRefusjonskrav",
        required = true
    ) val samordningRefusjonskrav: TjenestepensjonRefusjonskravVurdering,
    @param:JsonProperty(
        "behovstype",
        required = true,
        defaultValue = SAMORDNING_REFUSJONS_KRAV
    ) val behovstype: AvklaringsbehovKode = AvklaringsbehovKode.`5056`
): EnkeltAvklaringsbehovLøsning {
    override fun løs(repositoryProvider: RepositoryProvider, kontekst: AvklaringsbehovKontekst, gatewayProvider: GatewayProvider): LøsningsResultat {
        return TjenestepensjonRefusjonskravLøser(repositoryProvider).løs(kontekst, this)
    }
}
