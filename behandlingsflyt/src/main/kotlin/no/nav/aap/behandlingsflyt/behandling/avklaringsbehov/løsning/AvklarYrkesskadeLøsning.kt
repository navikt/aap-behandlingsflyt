package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.AvklarYrkesskadeLøser
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.LøsningsResultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.flate.YrkesskadevurderingDto
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AVKLAR_YRKESSKADE_KODE
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AvklaringsbehovKode
import no.nav.aap.lookup.repository.RepositoryProvider

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(value = AVKLAR_YRKESSKADE_KODE)
class AvklarYrkesskadeLøsning(
    @JsonProperty("yrkesskadesvurdering", required = true) val yrkesskadesvurdering: YrkesskadevurderingDto,
    @JsonProperty(
        "behovstype",
        required = true,
        defaultValue = AVKLAR_YRKESSKADE_KODE
    ) val behovstype: AvklaringsbehovKode = AvklaringsbehovKode.`5013`
) : AvklaringsbehovLøsning {
    override fun løs(repositoryProvider: RepositoryProvider, kontekst: AvklaringsbehovKontekst): LøsningsResultat {
        return AvklarYrkesskadeLøser(repositoryProvider).løs(kontekst, this)
    }
}
