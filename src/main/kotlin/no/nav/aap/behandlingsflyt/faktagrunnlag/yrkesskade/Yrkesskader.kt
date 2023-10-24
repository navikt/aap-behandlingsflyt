package no.nav.aap.behandlingsflyt.faktagrunnlag.yrkesskade

class Yrkesskader(val yrkesskader: List<Yrkesskade>) {

    fun harYrkesskade(): Boolean {
        return yrkesskader.isNotEmpty()
    }
}
