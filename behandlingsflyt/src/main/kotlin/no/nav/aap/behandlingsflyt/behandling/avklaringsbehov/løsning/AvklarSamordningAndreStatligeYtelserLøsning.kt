package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.AvklarSamordningAndreStatligeYtelserLøser
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.LøsningsResultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.andrestatligeytelservurdering.SamordningAndreStatligeYtelserVurderingDto
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AVKLAR_SAMORDNING_ANDRE_STATLIGE_YTELSER_KODE
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AvklaringsbehovKode
import no.nav.aap.lookup.repository.RepositoryProvider

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(value = AVKLAR_SAMORDNING_ANDRE_STATLIGE_YTELSER_KODE)
class AvklarSamordningAndreStatligeYtelserLøsning(
    @param:JsonProperty(
        "samordningAndreStatligeYtelserVurdering",
        required = true
    ) val samordningAndreStatligeYtelserVurdering: SamordningAndreStatligeYtelserVurderingDto,
    @param:JsonProperty(
        "behovstype",
        required = true,
        defaultValue = AVKLAR_SAMORDNING_ANDRE_STATLIGE_YTELSER_KODE
    ) val behovstype: AvklaringsbehovKode = AvklaringsbehovKode.`5027`
) : AvklaringsbehovLøsning {
    override fun løs(repositoryProvider: RepositoryProvider, kontekst: AvklaringsbehovKontekst): LøsningsResultat {
        return AvklarSamordningAndreStatligeYtelserLøser(repositoryProvider).løs(kontekst, this)
    }
}