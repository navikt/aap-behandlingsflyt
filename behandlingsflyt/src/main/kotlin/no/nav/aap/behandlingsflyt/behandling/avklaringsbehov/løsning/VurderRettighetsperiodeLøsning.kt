package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.LøsningsResultat
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.VurderRettighetsperiodeLøser
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.rettighetsperiode.RettighetsperiodeVurdering
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AvklaringsbehovKode
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.VURDER_RETTIGHETSPERIODE_KODE
import no.nav.aap.lookup.repository.RepositoryProvider

@JsonTypeName(value = VURDER_RETTIGHETSPERIODE_KODE)
class VurderRettighetsperiodeLøsning(
    @JsonProperty(
        "rettighetsperiodeVurdering",
        required = true
    ) val rettighetsperiodeVurdering: RettighetsperiodeVurdering,
    @JsonProperty("behovstype", required = true, defaultValue = VURDER_RETTIGHETSPERIODE_KODE)
    val behovstype: AvklaringsbehovKode = AvklaringsbehovKode.`5029`
) : AvklaringsbehovLøsning {
    override fun løs(repositoryProvider: RepositoryProvider, kontekst: AvklaringsbehovKontekst): LøsningsResultat {
        return VurderRettighetsperiodeLøser(repositoryProvider).løs(kontekst, this)
    }

}
