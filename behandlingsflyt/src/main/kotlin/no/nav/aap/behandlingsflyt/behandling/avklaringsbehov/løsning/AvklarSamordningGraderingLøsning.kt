package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.AvklarSamordningGraderingLøser
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.LøsningsResultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.samordning.VurderingerForSamordning
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AVKLAR_SAMORDNING_GRADERING_KODE
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AvklaringsbehovKode
import no.nav.aap.lookup.repository.RepositoryProvider

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(value = AVKLAR_SAMORDNING_GRADERING_KODE)
class AvklarSamordningGraderingLøsning(
    @JsonProperty("vurderingerForSamordning", required = true) val vurderingerForSamordning: VurderingerForSamordning,
    @JsonProperty(
        "behovstype",
        required = true,
        defaultValue = AVKLAR_SAMORDNING_GRADERING_KODE
    ) val behovstype: AvklaringsbehovKode = AvklaringsbehovKode.`5012`
) : AvklaringsbehovLøsning {
    override fun løs(repositoryProvider: RepositoryProvider, kontekst: AvklaringsbehovKontekst): LøsningsResultat {
        return AvklarSamordningGraderingLøser(repositoryProvider).løs(kontekst, this)
    }
}