package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.AvklarManuellInntektVurderingLøser
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.LøsningsResultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.ManuellInntektVurderingDto
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AvklaringsbehovKode
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.FASTSETT_MANUELL_INNTEKT
import no.nav.aap.lookup.repository.RepositoryProvider

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(value = FASTSETT_MANUELL_INNTEKT)
class AvklarManuellInntektVurderingLøsning(
    @param:JsonProperty(
        "manuellVurderingForManglendeInntekt",
        required = true
    ) val manuellVurderingForManglendeInntekt: ManuellInntektVurderingDto,
    @param:JsonProperty(
        "behovstype",
        required = true,
        defaultValue = FASTSETT_MANUELL_INNTEKT
    ) val behovstype: AvklaringsbehovKode = AvklaringsbehovKode.`7001`
) : AvklaringsbehovLøsning {
    override fun løs(repositoryProvider: RepositoryProvider, kontekst: AvklaringsbehovKontekst): LøsningsResultat {
        return AvklarManuellInntektVurderingLøser(repositoryProvider).løs(kontekst, this)
    }
}