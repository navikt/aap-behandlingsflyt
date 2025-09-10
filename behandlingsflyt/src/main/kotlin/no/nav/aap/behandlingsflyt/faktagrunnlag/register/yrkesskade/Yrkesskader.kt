package no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade

/**
 * Liste med yrkesskader fra register.
 */
data class Yrkesskader(val yrkesskader: List<Yrkesskade>) {

    fun harYrkesskade(): Boolean {
        return yrkesskader.isNotEmpty()
    }
}
