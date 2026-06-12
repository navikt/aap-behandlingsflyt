package no.nav.aap.behandlingsflyt.faktagrunnlag.register.sykepengemaksdato

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonId
import java.time.LocalDate
import java.time.OffsetDateTime

@JsonIgnoreProperties(ignoreUnknown = true)
data class SykepengevedtakKafkaMelding(
    val fødselsnummer: String,
    val foreløpigBeregnetSluttPåSykepenger: LocalDate,
    val tidspunkt: OffsetDateTime,
)

data class MaksdatoHendelse(
    val personId: PersonId,
    val foreløpigMaksdato: LocalDate,
    val kilde: MaksdatoHendelseKilde = MaksdatoHendelseKilde.SPEIL
)

enum class MaksdatoHendelseKilde {
    SPEIL,
}

