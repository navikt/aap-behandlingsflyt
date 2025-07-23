package no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.Dødsdato
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters.PersonStatus
import java.time.LocalDate
import java.time.LocalDateTime

data class PersonopplysningMedHistorikk(
    val fødselsdato: Fødselsdato,
    val id: Long? = null,
    val dødsdato: Dødsdato? = null,
    val statsborgerskap: List<Statsborgerskap>,
    val folkeregisterStatuser: List<FolkeregisterStatus>,
    val utenlandsAddresser: List<UtenlandsAdresse>? = null
)

data class FolkeregisterStatus(
    val status: PersonStatus,
    val gyldighetstidspunkt: LocalDate?,
    val opphoerstidspunkt: LocalDate?
)

data class Statsborgerskap(
    val land: String,
    val gyldigFraOgMed: LocalDate? = null,
    val gyldigTilOgMed: LocalDate? = null,
)

data class UtenlandsAdresse (
    val gyldigFraOgMed: LocalDate? = null,
    val gyldigTilOgMed: LocalDate? = null,
    val adresseNavn: String?,
    val postkode: String?,
    val bySted: String?,
    val landkode: String?,
    val adresseType: AdresseType?
)

enum class AdresseType {
    KONTAKT_ADRESSE,
    OPPHOLDS_ADRESSE,
    BOSTEDS_ADRESSE
}