package no.nav.aap.behandlingsflyt.faktagrunnlag.personopplysninger

class PersonopplysningGrunnlag(private val id: Long, val personopplysning: Personopplysning) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PersonopplysningGrunnlag

        return personopplysning == other.personopplysning
    }

    override fun hashCode(): Int {
        return personopplysning.hashCode()
    }
}
