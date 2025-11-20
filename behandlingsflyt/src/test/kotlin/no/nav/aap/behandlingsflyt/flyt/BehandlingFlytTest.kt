package no.nav.aap.behandlingsflyt.flyt

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehov
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
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

    @Test
    fun `Skal sortere avklaringsbehov etter steg og deretter rekkefølge definert innenfor steg`() {
        val flyt = BehandlingFlytBuilder()
            .medSteg(GeneriskTestFlytSteg(StegType.AVKLAR_SYKDOM))
            .medSteg(GeneriskTestFlytSteg(StegType.FASTSETT_BEREGNINGSTIDSPUNKT,
                avklaringsbehovRekkefølge = listOf(
                    Definisjon.FASTSETT_BEREGNINGSTIDSPUNKT,
                    Definisjon.FASTSETT_YRKESSKADEINNTEKT
                ))
            )
            .medSteg(GeneriskTestFlytSteg(StegType.FORESLÅ_VEDTAK))
            .build()

        val avklaringsbehov1 = Avklaringsbehov(
            id = 1,
            definisjon = Definisjon.FASTSETT_YRKESSKADEINNTEKT,
            funnetISteg = StegType.FASTSETT_BEREGNINGSTIDSPUNKT,
            kreverToTrinn = false
        )
        val avklaringsbehov2 = Avklaringsbehov(
            id = 2,
            definisjon = Definisjon.FASTSETT_BEREGNINGSTIDSPUNKT,
            funnetISteg = StegType.FASTSETT_BEREGNINGSTIDSPUNKT,
            kreverToTrinn = false
        )
        val avklaringsbehov3 = Avklaringsbehov(
            id = 3,
            definisjon = Definisjon.AVKLAR_SYKDOM,
            funnetISteg = StegType.AVKLAR_SYKDOM,
            kreverToTrinn = false
        )
        val avklaringsbehov4 = Avklaringsbehov(
            id = 3,
            definisjon = Definisjon.FORESLÅ_VEDTAK,
            funnetISteg = StegType.FORESLÅ_VEDTAK,
            kreverToTrinn = false
        )

        // Forventet rekkefølge er at stegene kommer i rekkefølgen de er definert i flyten,
        // deretter kommer avklaringsbehovene i den rekkefølgen de er definert innenfor steget
        val sortert = listOf(avklaringsbehov1, avklaringsbehov4, avklaringsbehov2, avklaringsbehov3)
            .sortedWith(flyt.avklaringsbehovComparator)

        assertThat(sortert).containsExactly(avklaringsbehov3, avklaringsbehov2, avklaringsbehov1, avklaringsbehov4)
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
