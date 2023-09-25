package no.nav.aap.flyt

import no.nav.aap.flyt.steg.AvsluttBehandlingSteg
import no.nav.aap.flyt.steg.GeneriskPlaceholderSteg
import no.nav.aap.flyt.steg.InnhentRegisterdataSteg
import no.nav.aap.flyt.steg.StartBehandlingSteg
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class DefinisjonTest {

    private val førstegangsbehandling = BehandlingFlytBuilder()
        .medSteg(StartBehandlingSteg())
        .medSteg(InnhentRegisterdataSteg())
        .medSteg(GeneriskPlaceholderSteg(StegType.VURDER_MEDLEMSKAP))
        .medSteg(GeneriskPlaceholderSteg(StegType.FASTSETT_GRUNNLAG))
        .medSteg(AvsluttBehandlingSteg())
        .build()

    @Test
    fun `Skal finne neste steg for førstegangsbehandling`() {
        val neste = førstegangsbehandling.neste(StegType.START_BEHANDLING)

        assertThat(neste.type()).isEqualTo(StegType.INNHENT_REGISTERDATA)
    }

    @Test
    fun `Skal finne forrige steg for førstegangsbehandling`() {
        val forrige = førstegangsbehandling.forrige(StegType.FASTSETT_GRUNNLAG)

        assertThat(forrige.type()).isEqualTo(StegType.VURDER_MEDLEMSKAP)
    }
}
