package no.nav.aap.behandlingsflyt.faktagrunnlag.personopplysninger

class PersoninfoGrunnlag(private val id: Long, val personinfo: Personinfo){
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PersoninfoGrunnlag

        return personinfo == other.personinfo
    }

    override fun hashCode(): Int {
        return personinfo.hashCode()
    }
}
