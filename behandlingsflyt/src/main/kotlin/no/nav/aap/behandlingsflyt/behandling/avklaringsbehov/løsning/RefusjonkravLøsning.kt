package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.LøsningsResultat
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.RefusjonkravLøser
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.refusjonkrav.RefusjonkravVurderingDto
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AvklaringsbehovKode
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.REFUSJON_KRAV
import no.nav.aap.lookup.repository.RepositoryProvider

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(value = REFUSJON_KRAV)
class RefusjonkravLøsning(
    @JsonProperty("refusjonkravVurderinger", required = true) val refusjonkravVurderinger: List<RefusjonkravVurderingDto>,
    @JsonProperty(
        "behovstype",
        required = true,
        defaultValue = REFUSJON_KRAV
    ) val behovstype: AvklaringsbehovKode = AvklaringsbehovKode.`5026`
) : AvklaringsbehovLøsning {
    override fun løs(repositoryProvider: RepositoryProvider, kontekst: AvklaringsbehovKontekst): LøsningsResultat {
        return RefusjonkravLøser(repositoryProvider).løs(kontekst, this)
    }
}
