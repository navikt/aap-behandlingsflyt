package no.nav.aap.behandlingsflyt.grunnlag.person

class Personinfo(val fødselsdato: Fødselsdato){
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Personinfo

        return fødselsdato == other.fødselsdato
    }

    override fun hashCode(): Int {
        return fødselsdato.hashCode()
    }
}
