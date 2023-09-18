package no.nav.aap.flyt

import no.nav.aap.domene.behandling.Førstegangsbehandling
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class DefinisjonTest {

    @Test
    fun `Skal finne neste steg for førstegangsbehandling`() {
        val førstegangsbehandling = Førstegangsbehandling.flyt()

        val neste = førstegangsbehandling.neste(StegType.START_BEHANDLING)

        assertThat(neste.type()).isEqualTo(StegType.INNHENT_REGISTERDATA)
    }

    @Test
    fun `Skal finne forrige steg for førstegangsbehandling`() {
        val førstegangsbehandling = Førstegangsbehandling.flyt()

        val forrige = førstegangsbehandling.forrige(StegType.FASTSETT_GRUNNLAG)

        assertThat(forrige.type()).isEqualTo(StegType.AVKLAR_SYKDOM)
    }
}
