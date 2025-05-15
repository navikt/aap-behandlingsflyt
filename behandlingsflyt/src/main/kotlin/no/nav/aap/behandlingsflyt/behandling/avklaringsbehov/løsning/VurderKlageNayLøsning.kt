package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.LøsningsResultat
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.VurderKlageNayLøser
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.klagebehandling.nay.flate.KlagevurderingNayLøsningDto
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AvklaringsbehovKode
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.VURDER_KLAGE_NAY_KODE
import no.nav.aap.lookup.repository.RepositoryProvider

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(value = VURDER_KLAGE_NAY_KODE)
class VurderKlageNayLøsning(
    @JsonProperty("klagevurderingNay", required = true)
    val klagevurderingNay: KlagevurderingNayLøsningDto,
    @JsonProperty(
        "behovstype",
        required = true,
        defaultValue = VURDER_KLAGE_NAY_KODE
    ) val behovstype: AvklaringsbehovKode = AvklaringsbehovKode.`6003`
) : AvklaringsbehovLøsning {
    override fun løs(repositoryProvider: RepositoryProvider, kontekst: AvklaringsbehovKontekst): LøsningsResultat {
        return VurderKlageNayLøser(repositoryProvider).løs(kontekst, this)
    }
}