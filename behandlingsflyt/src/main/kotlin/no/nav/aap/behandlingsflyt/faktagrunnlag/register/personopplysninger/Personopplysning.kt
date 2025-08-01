package no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.Dødsdato

class Personopplysning(
    val fødselsdato: Fødselsdato,
    val id: Long? = null,
    val dødsdato: Dødsdato? = null,
    val status: PersonStatus,
    val statsborgerskap: List<Statsborgerskap>,
    val utenlandsAddresser: List<UtenlandsAdresse>? = null
) {

    // Denne skal kun sammenlikne data og ikke tidspunkter
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Personopplysning

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
