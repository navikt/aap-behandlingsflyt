package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.AvklarSamordningArbeidsgiverLøser
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.LøsningsResultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsgiver.SamordningArbeidsgiverVurderingDto
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AVKLAR_SAMORDNING_ARBEIDSGIVER_KODE
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AvklaringsbehovKode
import no.nav.aap.lookup.repository.RepositoryProvider

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(value = AVKLAR_SAMORDNING_ARBEIDSGIVER_KODE)
class AvklarSamordningArbeidsgiverLøsning(
    @JsonProperty("samordningArbeidsgiverVurdering", required = true) val samordningArbeidsgiverVurdering: SamordningArbeidsgiverVurderingDto,
    @JsonProperty(
        "behovstype",
        required = true,
        defaultValue =AVKLAR_SAMORDNING_ARBEIDSGIVER_KODE
    ) val behovstype: AvklaringsbehovKode = AvklaringsbehovKode.`5030`
) : AvklaringsbehovLøsning {
    override fun løs(repositoryProvider: RepositoryProvider, kontekst: AvklaringsbehovKontekst): LøsningsResultat {
        return AvklarSamordningArbeidsgiverLøser(repositoryProvider).løs(kontekst, this)
    }
}