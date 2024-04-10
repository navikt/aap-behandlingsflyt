package no.nav.aap.behandlingsflyt.avklaringsbehov.løsning

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.behandlingsflyt.avklaringsbehov.AVKLAR_STUDENT_KODE
import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.AvklarStudentLøser
import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.LøsningsResultat
import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.StudentVurdering
import no.nav.aap.verdityper.flyt.FlytKontekst

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(value = AVKLAR_STUDENT_KODE)
class AvklarStudentLøsning(
    @JsonProperty("studentvurdering", required = true) val studentvurdering: StudentVurdering,
    @JsonProperty(
        "behovstype",
        required = true,
        defaultValue = AVKLAR_STUDENT_KODE
    ) val behovstype: String = AVKLAR_STUDENT_KODE
) :
    AvklaringsbehovLøsning {
    override fun løs(connection: DBConnection, kontekst: FlytKontekst): LøsningsResultat {
        return AvklarStudentLøser(connection).løs(kontekst, this)
    }

}
