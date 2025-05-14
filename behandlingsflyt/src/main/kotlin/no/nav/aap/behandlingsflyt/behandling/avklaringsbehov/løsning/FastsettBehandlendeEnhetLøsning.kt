package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.FastsettBehandlendeEnhetLøser
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.LøsningsResultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.behandlendeenhet.flate.BehandlendeEnhetLøsningDto
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AvklaringsbehovKode
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.FASTSETT_BEHANDLENDE_ENHET_KODE
import no.nav.aap.lookup.repository.RepositoryProvider

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(value = FASTSETT_BEHANDLENDE_ENHET_KODE)
class FastsettBehandlendeEnhetLøsning(
    @JsonProperty("behandlendeEnhetVurdering", required = true)
    val behandlendeEnhetVurdering: BehandlendeEnhetLøsningDto,
    @JsonProperty(
        "behovstype",
        required = true,
        defaultValue = FASTSETT_BEHANDLENDE_ENHET_KODE
    ) val behovstype: AvklaringsbehovKode = AvklaringsbehovKode.`6001`
) : AvklaringsbehovLøsning {
    override fun løs(repositoryProvider: RepositoryProvider, kontekst: AvklaringsbehovKontekst): LøsningsResultat {
        return FastsettBehandlendeEnhetLøser(repositoryProvider).løs(kontekst, this)
    }
}