package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.FastsettYrkesskadeInntektLøser
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.LøsningsResultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningYrkeskaderBeløpVurderingDTO
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AvklaringsbehovKode
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.FASTSETT_YRKESSKADE_BELØP_KODE
import no.nav.aap.lookup.repository.RepositoryProvider

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(value = FASTSETT_YRKESSKADE_BELØP_KODE)
class FastsettYrkesskadeInntektLøsning(
    @JsonProperty(
        "yrkesskadeInntektVurdering",
        required = true
    ) val yrkesskadeInntektVurdering: BeregningYrkeskaderBeløpVurderingDTO,
    @JsonProperty(
        "behovstype",
        required = true,
        defaultValue = FASTSETT_YRKESSKADE_BELØP_KODE
    ) val behovstype: AvklaringsbehovKode = AvklaringsbehovKode.`5014`
) : AvklaringsbehovLøsning {
    override fun løs(repositoryProvider: RepositoryProvider, kontekst: AvklaringsbehovKontekst): LøsningsResultat {
        return FastsettYrkesskadeInntektLøser(repositoryProvider).løs(kontekst, this)
    }
}