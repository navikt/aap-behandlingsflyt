package no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade

data class Yrkesskader(val yrkesskader: List<Yrkesskade>) {

    fun harYrkesskade(): Boolean {
        return yrkesskader.isNotEmpty()
    }
}
