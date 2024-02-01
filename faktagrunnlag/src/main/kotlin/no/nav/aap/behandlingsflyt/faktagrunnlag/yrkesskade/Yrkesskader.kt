package no.nav.aap.behandlingsflyt.faktagrunnlag.yrkesskade

class Yrkesskader(val yrkesskader: List<Yrkesskade>) {

    fun harYrkesskade(): Boolean {
        return yrkesskader.isNotEmpty()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Yrkesskader

        return yrkesskader == other.yrkesskader
    }

    override fun hashCode(): Int {
        return yrkesskader.hashCode()
    }
}
