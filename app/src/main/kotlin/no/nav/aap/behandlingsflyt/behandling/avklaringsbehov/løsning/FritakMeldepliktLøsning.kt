package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.FRITAK_MELDEPLIKT_KODE
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.FritakFraMeldepliktLøser
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.LøsningsResultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.Fritaksvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.flate.FritaksvurderingDto
import no.nav.aap.komponenter.dbconnect.DBConnection

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(value = FRITAK_MELDEPLIKT_KODE)
class FritakMeldepliktLøsning(
    val fritaksvurdering: Fritaksvurdering,
    val behovstype: String
) : AvklaringsbehovLøsning {

    @JsonCreator
    constructor(
        @JsonProperty("fritaksvurdering", required = true) fritaksvurdering: FritaksvurderingDto,
        @JsonProperty("behovstype", required = true, defaultValue = FRITAK_MELDEPLIKT_KODE) behovstype: String = FRITAK_MELDEPLIKT_KODE
    ): this(fritaksvurdering.toFritaksvurdering(), behovstype)

    override fun løs(connection: DBConnection, kontekst: AvklaringsbehovKontekst): LøsningsResultat {
        return FritakFraMeldepliktLøser(connection).løs(kontekst, this)
    }
}
