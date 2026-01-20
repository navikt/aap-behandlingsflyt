package no.nav.aap.behandlingsflyt.flyt

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehov
import no.nav.aap.behandlingsflyt.behandling.lovvalg.LovvalgInformasjonskrav
import no.nav.aap.behandlingsflyt.forretningsflyt.behandlingstyper.Førstegangsbehandling
import no.nav.aap.behandlingsflyt.forretningsflyt.behandlingstyper.Revurdering
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
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
            .medSteg(
                GeneriskTestFlytSteg(
                    StegType.FASTSETT_BEREGNINGSTIDSPUNKT,
                    avklaringsbehovRekkefølge = listOf(
                        Definisjon.FASTSETT_BEREGNINGSTIDSPUNKT,
                        Definisjon.FASTSETT_YRKESSKADEINNTEKT
                    )
                )
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

    @Test
    fun `hent faktagrunnlag for gjeldende steg`() {
        val behandlingFlyt = BehandlingFlytBuilder()
            .medSteg(GeneriskTestFlytSteg(StegType.START_BEHANDLING))
            .medSteg(
                GeneriskTestFlytSteg(StegType.VURDER_MEDLEMSKAP),
                informasjonskrav = listOf(LovvalgInformasjonskrav)
            )
            .medSteg(GeneriskTestFlytSteg(StegType.FASTSETT_GRUNNLAG))
            .build()

        // Forventer å få tilbake LovvalgInformasjonskrav
        behandlingFlyt.forberedFlyt(StegType.VURDER_MEDLEMSKAP)
        val faktagrunnlag = behandlingFlyt.faktagrunnlagForGjeldendeSteg()

        assertThat(faktagrunnlag).containsExactly(Pair(StegType.VURDER_MEDLEMSKAP, LovvalgInformasjonskrav))

        // Flytter til nytt steg, her er ingen informasjonskrav
        behandlingFlyt.forberedFlyt(StegType.FASTSETT_GRUNNLAG)
        val faktagrunnlag2 = behandlingFlyt.faktagrunnlagForGjeldendeSteg()

        assertThat(faktagrunnlag2).isEmpty()
    }

    @Test
    fun `hent faktagrunnlag før gjeldende steg`() {
        val behandlingFlyt = BehandlingFlytBuilder()
            .medSteg(GeneriskTestFlytSteg(StegType.START_BEHANDLING))
            .medSteg(
                GeneriskTestFlytSteg(StegType.VURDER_MEDLEMSKAP),
                informasjonskrav = listOf(LovvalgInformasjonskrav)
            )
            .medSteg(GeneriskTestFlytSteg(StegType.FASTSETT_GRUNNLAG))
            .build()

        // Forventer å få tilbake LovvalgInformasjonskrav
        behandlingFlyt.forberedFlyt(StegType.VURDER_MEDLEMSKAP)
        val faktagrunnlag = behandlingFlyt.alleFaktagrunnlagFørGjeldendeSteg()

        // Strengt _før_ VURDER_MEDLEMSKAP er det ingen informasjonskrav
        assertThat(faktagrunnlag).isEmpty()

        // Flytter til nytt steg, her er ingen informasjonskrav
        behandlingFlyt.forberedFlyt(StegType.FASTSETT_GRUNNLAG)
        val faktagrunnlag2 = behandlingFlyt.alleFaktagrunnlagFørGjeldendeSteg()

        assertThat(faktagrunnlag2).containsExactly(Pair(StegType.VURDER_MEDLEMSKAP, LovvalgInformasjonskrav))
    }


    @Test
    fun `utlede nytt tidlig steg for førstegangsbehandling`() {
        val flyt = Førstegangsbehandling.flyt()
        flyt.forberedFlyt(StegType.BARNETILLEGG)

        assertThat(flyt.aktivtSteg().type()).isEqualTo(StegType.BARNETILLEGG)

        val resultat = flyt.tilbakeflytEtterEndringer(
            oppdaterteGrunnlagstype = listOf(),
            nyeVurderingsbehov = listOf(Vurderingsbehov.SYKDOM_ARBEVNE_BEHOV_FOR_BISTAND)
        )

        val sisteStegITilbakeføringsflyt = resultat.stegene().last()

        assertThat(flyt.erStegFørEllerLik(sisteStegITilbakeføringsflyt, StegType.AVKLAR_SYKDOM)).isTrue()
    }

    @Test
    fun `utlede nytt tidlig steg for revurdering`() {
        val flyt = Revurdering.flyt()
        flyt.forberedFlyt(StegType.BARNETILLEGG)

        assertThat(flyt.aktivtSteg().type()).isEqualTo(StegType.BARNETILLEGG)

        val resultat = flyt.tilbakeflytEtterEndringer(
            oppdaterteGrunnlagstype = listOf(),
            nyeVurderingsbehov = listOf(Vurderingsbehov.SYKDOM_ARBEVNE_BEHOV_FOR_BISTAND)
        )

        val sisteStegITilbakeføringsflyt = resultat.stegene().last()

        assertThat(flyt.erStegFørEllerLik(sisteStegITilbakeføringsflyt, StegType.AVKLAR_SYKDOM)).isTrue()
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
