package no.nav.aap.behandlingsflyt.flyt

import no.nav.aap.verdityper.flyt.StegType
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.GeneriskPlaceholderFlytSteg
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

        assertThat(gjenståendeStegIAktivGruppe).containsExactly(StegType.VURDER_BISTANDSBEHOV, StegType.FRITAK_MELDEPLIKT)
    }

    private val førstegangsbehandling = BehandlingFlytBuilder()
        .medSteg(GeneriskPlaceholderFlytSteg(StegType.START_BEHANDLING))
        .medSteg(GeneriskPlaceholderFlytSteg(StegType.VURDER_MEDLEMSKAP))
        .medSteg(GeneriskPlaceholderFlytSteg(StegType.FASTSETT_GRUNNLAG))
        .build()

    private val sykdomsbehandling = BehandlingFlytBuilder()
        .medSteg(GeneriskPlaceholderFlytSteg(StegType.AVKLAR_SYKDOM))
        .medSteg(GeneriskPlaceholderFlytSteg(StegType.VURDER_BISTANDSBEHOV))
        .medSteg(GeneriskPlaceholderFlytSteg(StegType.FRITAK_MELDEPLIKT))
        .medSteg(GeneriskPlaceholderFlytSteg(StegType.FASTSETT_GRUNNLAG))
        .medSteg(GeneriskPlaceholderFlytSteg(StegType.IVERKSETT_VEDTAK))
        .build()
}
