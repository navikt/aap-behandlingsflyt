package no.nav.aap.domene.behandling.grunnlag.yrkesskade

class Yrkesskader(val yrkesskader: List<Yrkesskade>) {

    fun harYrkesskade(): Boolean {
        return yrkesskader.isNotEmpty()
    }
}
