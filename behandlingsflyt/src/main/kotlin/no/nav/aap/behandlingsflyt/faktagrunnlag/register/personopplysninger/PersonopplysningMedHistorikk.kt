package no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.Dødsdato
import java.time.LocalDate

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

enum class PersonStatus {
    bosatt,
    utflyttet,
    forsvunnet,
    doed,
    opphort,
    foedselsregistrert,
    ikkeBosatt,
    midlertidig,
    inaktiv
}

data class Statsborgerskap(
    val land: String,
    override val gyldigFraOgMed: LocalDate? = null,
    override val gyldigTilOgMed: LocalDate? = null,
) : GyldigPeriode

data class UtenlandsAdresse(
    override val gyldigFraOgMed: LocalDate? = null,
    override val gyldigTilOgMed: LocalDate? = null,
    val adresseNavn: String?,
    val postkode: String?,
    val bySted: String?,
    val landkode: String?,
    val adresseType: AdresseType?
) : GyldigPeriode

enum class AdresseType {
    KONTAKT_ADRESSE,
    OPPHOLDS_ADRESSE,
    BOSTEDS_ADRESSE
}