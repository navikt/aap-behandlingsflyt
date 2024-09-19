package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.VURDER_SYKEPENGEERSTATNING_KODE
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.AvklarSykepengerErstatningLøser
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.LøsningsResultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykepengerVurdering
import no.nav.aap.komponenter.dbconnect.DBConnection

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(value = VURDER_SYKEPENGEERSTATNING_KODE)
class AvklarSykepengerErstatningLøsning(
    @JsonProperty(
        "sykepengeerstatningVurdering",
        required = true
    ) val sykepengeerstatningVurdering: SykepengerVurdering,
    @JsonProperty(
        "behovstype",
        required = true,
        defaultValue = VURDER_SYKEPENGEERSTATNING_KODE
    ) val behovstype: String = VURDER_SYKEPENGEERSTATNING_KODE
) : AvklaringsbehovLøsning {
    override fun løs(connection: DBConnection, kontekst: AvklaringsbehovKontekst): LøsningsResultat {
        return AvklarSykepengerErstatningLøser(connection).løs(kontekst, this)
    }
}
