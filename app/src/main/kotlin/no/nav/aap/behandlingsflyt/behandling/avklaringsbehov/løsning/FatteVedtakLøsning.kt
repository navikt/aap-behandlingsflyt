package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.FATTE_VEDTAK_KODE
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.FatteVedtakLøser
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.LøsningsResultat
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.vedtak.TotrinnsVurdering
import no.nav.aap.komponenter.dbconnect.DBConnection

@JsonTypeName(value = FATTE_VEDTAK_KODE)
class FatteVedtakLøsning(
    @JsonProperty("vurderinger", required = true) val vurderinger: List<TotrinnsVurdering>,
    @JsonProperty(
        "behovstype",
        required = true,
        defaultValue = FATTE_VEDTAK_KODE
    ) val behovstype: String = FATTE_VEDTAK_KODE
) : AvklaringsbehovLøsning {
    override fun løs(connection: DBConnection, kontekst: AvklaringsbehovKontekst): LøsningsResultat {
        return FatteVedtakLøser(connection).løs(kontekst, this)
    }
}
