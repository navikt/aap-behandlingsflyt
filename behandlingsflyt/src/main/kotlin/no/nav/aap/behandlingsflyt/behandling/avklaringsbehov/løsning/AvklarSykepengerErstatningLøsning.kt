package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.AvklarSykepengerErstatningLøser
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.LøsningsResultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.flate.SykepengerVurderingDto
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AvklaringsbehovKode
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.VURDER_SYKEPENGEERSTATNING_KODE
import no.nav.aap.lookup.repository.RepositoryProvider

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(value = VURDER_SYKEPENGEERSTATNING_KODE)
class AvklarSykepengerErstatningLøsning(
    @param:JsonProperty(
        "sykepengeerstatningVurdering",
        required = true
    ) val sykepengeerstatningVurdering: SykepengerVurderingDto,
    @param:JsonProperty(
        "behovstype",
        required = true,
        defaultValue = VURDER_SYKEPENGEERSTATNING_KODE
    ) val behovstype: AvklaringsbehovKode = AvklaringsbehovKode.`5007`
) : AvklaringsbehovLøsning {
    override fun løs(repositoryProvider: RepositoryProvider, kontekst: AvklaringsbehovKontekst): LøsningsResultat {
        return AvklarSykepengerErstatningLøser(repositoryProvider).løs(kontekst, this)
    }
}
