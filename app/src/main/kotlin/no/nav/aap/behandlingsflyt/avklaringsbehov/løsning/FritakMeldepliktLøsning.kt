package no.nav.aap.behandlingsflyt.avklaringsbehov.løsning

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.behandlingsflyt.avklaringsbehov.FRITAK_MELDEPLIKT_KODE
import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.FritakFraMeldepliktLøser
import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.LøsningsResultat
import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.Fritaksvurdering
import no.nav.aap.verdityper.flyt.FlytKontekst

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(value = FRITAK_MELDEPLIKT_KODE)
class FritakMeldepliktLøsning(
    @JsonProperty("fritakmeldepliktVurdering", required = true) val vurdering: Fritaksvurdering?,
    @JsonProperty("behovstype", required = true, defaultValue = FRITAK_MELDEPLIKT_KODE) val behovstype: String = FRITAK_MELDEPLIKT_KODE
) : AvklaringsbehovLøsning {
    override fun løs(connection: DBConnection, kontekst: FlytKontekst): LøsningsResultat {
        return FritakFraMeldepliktLøser(connection).løs(kontekst, this)
    }
}
