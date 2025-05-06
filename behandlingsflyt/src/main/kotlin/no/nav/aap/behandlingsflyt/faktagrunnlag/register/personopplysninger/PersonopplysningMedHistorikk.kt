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
    val utenlandsAddresser: UtenlandsAdresser? = null
) {

    // Denne skal kun sammenlikne data og ikke tidspunkter
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PersonopplysningMedHistorikk

        if (fødselsdato != other.fødselsdato) return false
        if (dødsdato != other.dødsdato) return false

        return true
    }

    override fun hashCode(): Int {
        var result = fødselsdato.hashCode()
        result = 31 * result + (dødsdato?.hashCode() ?: 0)
        return result
    }
}

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

data class UtenlandsAdresser (
    val kontaktAdresser: List<KontaktAdresse>?,
    val bostedsAdresser: List<BostedsAdresse>?,
    val oppholdsAdresse: List<OppholdsAdresse>?
)

data class KontaktAdresse(
    val gyldigFraOgMed: LocalDateTime? = null,
    val gyldigTilOgMed: LocalDateTime? = null,
    val utenlandskAdresse: Adresse
)

data class BostedsAdresse(
    val gyldigFraOgMed: LocalDateTime? = null,
    val gyldigTilOgMed: LocalDateTime? = null,
    val utenlandskAdresse: Adresse
)

data class OppholdsAdresse(
    val gyldigFraOgMed: LocalDateTime? = null,
    val gyldigTilOgMed: LocalDateTime? = null,
    val utenlandskAdresse: Adresse
)

data class Adresse (
    val adresseNavn: String?,
    val postkode: String?,
    val bySted: String?,
    val landkode: String?
)