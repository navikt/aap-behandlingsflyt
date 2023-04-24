package no.nav.aap.flyt

class DefinisjonTest {

    @org.junit.jupiter.api.Test
    fun `Skal finne neste steg for førstegangsbehandling`() {
        val førstegangsbehandling = Definisjon.førstegangsbehandling

        val neste = førstegangsbehandling.neste(StegType.START_BEHANDLING)

        assert(neste == StegType.INNGANGSVILKÅR)
    }
}
