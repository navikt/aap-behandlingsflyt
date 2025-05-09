package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.LøsningsResultat
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.SamordningVentPaVirkningstidspunktLøser
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AvklaringsbehovKode
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.SAMORDNING_VENT_PA_VIRKNINGSTIDSPUNKT
import no.nav.aap.lookup.repository.RepositoryProvider


@JsonTypeName(value = SAMORDNING_VENT_PA_VIRKNINGSTIDSPUNKT)
class SamordningVentPaVirkningstidspunktLøsning(
    @JsonProperty(
        "behovstype",
        required = true,
        defaultValue = SAMORDNING_VENT_PA_VIRKNINGSTIDSPUNKT
    ) val behovstype: AvklaringsbehovKode = AvklaringsbehovKode.`5025`
) : AvklaringsbehovLøsning {
    override fun løs(repositoryProvider: RepositoryProvider, kontekst: AvklaringsbehovKontekst): LøsningsResultat {
        return SamordningVentPaVirkningstidspunktLøser(repositoryProvider).løs(kontekst, this)
    }
}