package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.LøsningsResultat
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.TjenestepensjonRefusjonskravLøser
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.samordning.refusjonskrav.TjenestepensjonRefusjonskravVurdering
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AvklaringsbehovKode
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.SAMORDNING_REFUSJONS_KRAV
import no.nav.aap.lookup.repository.RepositoryProvider

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(value = SAMORDNING_REFUSJONS_KRAV)
class TjenestepensjonRefusjonskravLøsning (
    @JsonProperty("samordningRefusjonskrav", required = true) val samordningRefusjonskrav: TjenestepensjonRefusjonskravVurdering,
    @JsonProperty(
        "behovstype",
        required = true,
        defaultValue = SAMORDNING_REFUSJONS_KRAV
    ) val behovstype: AvklaringsbehovKode = AvklaringsbehovKode.`5056`
): AvklaringsbehovLøsning {
    override fun løs(repositoryProvider: RepositoryProvider, kontekst: AvklaringsbehovKontekst): LøsningsResultat {
        return TjenestepensjonRefusjonskravLøser(repositoryProvider).løs(kontekst, this)
    }
}
