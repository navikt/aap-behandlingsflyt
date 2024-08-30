package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AVKLAR_SAMORDNING_GRADERING_KODE
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.AvklarSamordningGraderingLøser
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.LøsningsResultat
import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.samordning.SamordningGraderingVurderingDto

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(value = AVKLAR_SAMORDNING_GRADERING_KODE)
class AvklarSamordningGraderingLøsning(
    @JsonProperty("samordningGraderingVurdering", required = true) val samordningGraderingVurdering: SamordningGraderingVurderingDto,
    @JsonProperty("behovstype", required = true, defaultValue = AVKLAR_SAMORDNING_GRADERING_KODE) val behovstype: String = AVKLAR_SAMORDNING_GRADERING_KODE):AvklaringsbehovLøsning {
    override fun løs(connection: DBConnection, kontekst: AvklaringsbehovKontekst): LøsningsResultat {
        return AvklarSamordningGraderingLøser(connection).løs(kontekst, this)
    }

}