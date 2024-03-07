
package no.nav.aap.behandlingsflyt.avklaringsbehov.løsning

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.behandlingsflyt.avklaringsbehov.AVKLAR_STUDENT_KODE
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.StudentVurdering

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(value = AVKLAR_STUDENT_KODE)
class AvklarStudentLøsning(
    @JsonProperty("studentvurdering", required = true) val studentvurdering: StudentVurdering
) :
    AvklaringsbehovLøsning
