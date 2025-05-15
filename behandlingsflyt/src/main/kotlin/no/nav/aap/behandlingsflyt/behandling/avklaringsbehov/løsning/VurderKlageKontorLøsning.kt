package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.LøsningsResultat
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.VurderKlageKontorLøser
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.klagebehandling.kontor.flate.KlagevurderingKontorLøsningDto
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AvklaringsbehovKode
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.VURDER_KLAGE_KONTOR_KODE
import no.nav.aap.lookup.repository.RepositoryProvider

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(value = VURDER_KLAGE_KONTOR_KODE)
class VurderKlageKontorLøsning(
    @JsonProperty("klagevurderingKontor", required = true)
    val klagevurderingKontor: KlagevurderingKontorLøsningDto,
    @JsonProperty(
        "behovstype",
        required = true,
        defaultValue = VURDER_KLAGE_KONTOR_KODE
    ) val behovstype: AvklaringsbehovKode = AvklaringsbehovKode.`6002`
) : AvklaringsbehovLøsning {
    override fun løs(repositoryProvider: RepositoryProvider, kontekst: AvklaringsbehovKontekst): LøsningsResultat {
        return VurderKlageKontorLøser(repositoryProvider).løs(kontekst, this)
    }
}