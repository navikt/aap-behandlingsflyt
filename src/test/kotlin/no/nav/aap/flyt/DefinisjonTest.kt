package no.nav.aap.flyt

import org.junit.jupiter.api.Test

class DefinisjonTest {

    @Test
    fun `Skal finne neste steg for førstegangsbehandling`() {
        val førstegangsbehandling = Definisjon.førstegangsbehandling

        val neste = førstegangsbehandling.neste(StegType.START_BEHANDLING)

        assert(neste == StegType.AVKLAR_YRKESSKADE)
    }

    @Test
    fun `Skal finne forrige steg for førstegangsbehandling`() {
        val førstegangsbehandling = Definisjon.førstegangsbehandling

        val forrige = førstegangsbehandling.forrige(StegType.INNGANGSVILKÅR)

        assert(forrige == StegType.AVKLAR_YRKESSKADE)
    }
}
