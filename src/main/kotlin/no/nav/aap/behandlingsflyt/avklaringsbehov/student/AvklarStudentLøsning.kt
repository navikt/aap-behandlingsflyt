
package no.nav.aap.behandlingsflyt.avklaringsbehov.student

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.behandlingsflyt.avklaringsbehov.AvklaringsbehovLøsning
import no.nav.aap.behandlingsflyt.domene.behandling.avklaringsbehov.AVKLAR_STUDENT_KODE

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(value = AVKLAR_STUDENT_KODE)
class AvklarStudentLøsning(
    @JsonProperty("studentvurdering", required = true) val studentvurdering: StudentVurdering
) :
    AvklaringsbehovLøsning
