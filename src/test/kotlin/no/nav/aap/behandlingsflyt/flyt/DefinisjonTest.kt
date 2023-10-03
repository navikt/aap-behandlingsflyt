package no.nav.aap.behandlingsflyt.flyt

import no.nav.aap.behandlingsflyt.flyt.steg.GeneriskPlaceholderSteg
import no.nav.aap.behandlingsflyt.flyt.steg.InnhentPersonopplysningerSteg
import no.nav.aap.behandlingsflyt.flyt.steg.StartBehandlingSteg
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class DefinisjonTest {

    private val førstegangsbehandling = BehandlingFlytBuilder()
        .medSteg(StartBehandlingSteg())
        .medSteg(InnhentPersonopplysningerSteg())
        .medSteg(GeneriskPlaceholderSteg(StegType.VURDER_MEDLEMSKAP))
        .medSteg(GeneriskPlaceholderSteg(StegType.FASTSETT_GRUNNLAG))
        .build()

    @Test
    fun `Skal finne neste steg for førstegangsbehandling`() {
        val neste = førstegangsbehandling.neste(StegType.START_BEHANDLING)

        assertThat(neste!!.type()).isEqualTo(StegType.INNHENT_PERSONOPPLYSNINGER)
    }

    @Test
    fun `Skal finne forrige steg for førstegangsbehandling`() {
        val forrige = førstegangsbehandling.forrige(StegType.FASTSETT_GRUNNLAG)

        assertThat(forrige!!.type()).isEqualTo(StegType.VURDER_MEDLEMSKAP)
    }
}
