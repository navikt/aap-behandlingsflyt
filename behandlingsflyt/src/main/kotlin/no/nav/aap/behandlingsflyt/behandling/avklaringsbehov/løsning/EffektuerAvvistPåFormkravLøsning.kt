package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.EffektuerAvvistPåFormkravLøser
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.LøsningsResultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.effektueravvistpåformkrav.flate.EffektuerAvvistPåFormkravLøsningDto
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AvklaringsbehovKode
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.EFFEKTUER_AVVIST_PÅ_FORMKRAV_KODE
import no.nav.aap.lookup.repository.RepositoryProvider

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(value = EFFEKTUER_AVVIST_PÅ_FORMKRAV_KODE)
class EffektuerAvvistPåFormkravLøsning(
    @JsonProperty("effektuerAvvistPåFormkravVurdering", required = true)
    val effektuerAvvistPåFormkravVurdering: EffektuerAvvistPåFormkravLøsningDto,
    @JsonProperty(
        "behovstype",
        required = true,
        defaultValue = EFFEKTUER_AVVIST_PÅ_FORMKRAV_KODE
    ) val behovstype: AvklaringsbehovKode = AvklaringsbehovKode.`6004`
) : AvklaringsbehovLøsning {
    override fun løs(repositoryProvider: RepositoryProvider, kontekst: AvklaringsbehovKontekst): LøsningsResultat {
        return EffektuerAvvistPåFormkravLøser(repositoryProvider).løs(kontekst, this)
    }
}