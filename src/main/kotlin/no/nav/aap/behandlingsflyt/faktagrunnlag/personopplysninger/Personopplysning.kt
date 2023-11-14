package no.nav.aap.behandlingsflyt.faktagrunnlag.personopplysninger

class Personopplysning(val fødselsdato: Fødselsdato) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Personopplysning

        return fødselsdato == other.fødselsdato
    }

    override fun hashCode(): Int {
        return fødselsdato.hashCode()
    }
}
