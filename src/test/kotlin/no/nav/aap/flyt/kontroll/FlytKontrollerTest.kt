package no.nav.aap.flyt.kontroll

import org.junit.jupiter.api.Test

class FlytKontrollerTest {

    private val flytKontroller = FlytKontroller()

    @Test
    fun name() {
        val kontekst = FlytKontekst(1L, 1L)
        flytKontroller.prosesserBehandling(kontekst)

        val behandling = flytKontroller.behandliger[1L]
    }
}
