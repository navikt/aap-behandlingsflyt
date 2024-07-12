package no.nav.aap.institusjon

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import java.time.LocalDate
import java.time.LocalDateTime


data class InstitusjonoppholdRequest(
    val foedselsnumre: String
)

class InstitusjonoppholdRespons(
    val institusjonsopphold: List<Institusjonsopphold>
)

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class Institusjonsopphold(
    private val oppholdId: Long? = null,

    private val tssEksternId: String? = null,

    val organisasjonsnummer: String? = null,

    val institusjonstype: String? = null,

    private val varighet: String? = null,

    val kategori: String? = null,

    val startdato: LocalDate? = null,

    val faktiskSluttdato: LocalDate? = null,

    val forventetSluttdato: LocalDate? = null,

    private val kilde: String? = null,

    private val registrertAv: String? = null,

    private val overfoert: Boolean? = null,

    private val endretAv: String? = null,

    private val endringstidspunkt: LocalDateTime? = null,

    private val institusjonsnavn: String? = null,

    private val avdelingsnavn: String? = null,
)