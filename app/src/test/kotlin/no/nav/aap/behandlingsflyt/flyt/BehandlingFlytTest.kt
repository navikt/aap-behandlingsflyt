package no.nav.aap.behandlingsflyt.flyt

import no.nav.aap.verdityper.flyt.StegType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class BehandlingFlytTest {

    @Test
    fun `Skal finne neste steg for førstegangsbehandling`() {
        førstegangsbehandling.forberedFlyt(StegType.START_BEHANDLING)
        val neste = førstegangsbehandling.neste()

        assertThat(neste!!.type()).isEqualTo(StegType.VURDER_MEDLEMSKAP)
    }

    @Test
    fun `Skal finne gjenstående steg i aktiv gruppe`() {
        sykdomsbehandling.forberedFlyt(StegType.AVKLAR_SYKDOM)

        val gjenståendeStegIAktivGruppe = sykdomsbehandling.gjenståendeStegIAktivGruppe()

        assertThat(gjenståendeStegIAktivGruppe).containsExactly(
            StegType.VURDER_BISTANDSBEHOV,
            StegType.FRITAK_MELDEPLIKT
        )
    }

    private val førstegangsbehandling = BehandlingFlytBuilder()
        .medSteg(GeneriskTestFlytSteg(StegType.START_BEHANDLING))
        .medSteg(GeneriskTestFlytSteg(StegType.VURDER_MEDLEMSKAP))
        .medSteg(GeneriskTestFlytSteg(StegType.FASTSETT_GRUNNLAG))
        .build()

    private val sykdomsbehandling = BehandlingFlytBuilder()
        .medSteg(GeneriskTestFlytSteg(StegType.AVKLAR_SYKDOM))
        .medSteg(GeneriskTestFlytSteg(StegType.VURDER_BISTANDSBEHOV))
        .medSteg(GeneriskTestFlytSteg(StegType.FRITAK_MELDEPLIKT))
        .medSteg(GeneriskTestFlytSteg(StegType.FASTSETT_GRUNNLAG))
        .medSteg(GeneriskTestFlytSteg(StegType.IVERKSETT_VEDTAK))
        .build()
}
