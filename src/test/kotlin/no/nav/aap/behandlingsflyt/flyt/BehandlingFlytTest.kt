package no.nav.aap.behandlingsflyt.flyt

import no.nav.aap.behandlingsflyt.flyt.steg.StegType
import no.nav.aap.behandlingsflyt.flyt.steg.impl.GeneriskPlaceholderSteg
import no.nav.aap.behandlingsflyt.flyt.steg.impl.InnhentPersonopplysningerSteg
import no.nav.aap.behandlingsflyt.flyt.steg.impl.StartBehandlingSteg
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class BehandlingFlytTest {

    @Test
    fun `Skal finne neste steg for førstegangsbehandling`() {
        førstegangsbehandling.forberedFlyt(StegType.START_BEHANDLING)
        val neste = førstegangsbehandling.neste()

        assertThat(neste!!.type()).isEqualTo(StegType.INNHENT_PERSONOPPLYSNINGER)
    }

    private val førstegangsbehandling = BehandlingFlytBuilder()
        .medSteg(StartBehandlingSteg())
        .medSteg(InnhentPersonopplysningerSteg())
        .medSteg(GeneriskPlaceholderSteg(StegType.VURDER_MEDLEMSKAP))
        .medSteg(GeneriskPlaceholderSteg(StegType.FASTSETT_GRUNNLAG))
        .build()
}
