package no.nav.aap.domene.behandling.avklaringsbehov

import no.nav.aap.flyt.StegType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class AvklaringsbehoveneTest {

    @Test
    fun `skal kunne legge til nytt avklaringsbehov`() {
        val avklaringsbehovene = Avklaringsbehovene()
        avklaringsbehovene.leggTil(Avklaringsbehov(definisjon = Definisjon.AVKLAR_SYKDOM, funnetISteg = StegType.AVKLAR_SYKDOM))

        assertThat(avklaringsbehovene.antall()).isEqualTo(1)
    }

    @Test
    fun `skal ikke legge til duplikate avklaringsbehov`() {
        val avklaringsbehovene = Avklaringsbehovene()
        avklaringsbehovene.leggTil(Avklaringsbehov(definisjon = Definisjon.AVKLAR_SYKDOM, funnetISteg = StegType.AVKLAR_SYKDOM))
        avklaringsbehovene.leggTil(Avklaringsbehov(definisjon = Definisjon.AVKLAR_SYKDOM, funnetISteg = StegType.AVKLAR_SYKDOM))

        assertThat(avklaringsbehovene.antall()).isEqualTo(1)
    }

    @Test
    fun `skal løse avklaringsbehov`() {
        val avklaringsbehovene = Avklaringsbehovene()
        val avklaringsbehov = Avklaringsbehov(definisjon = Definisjon.AVKLAR_SYKDOM, funnetISteg = StegType.AVKLAR_SYKDOM)
        avklaringsbehovene.leggTil(avklaringsbehov)

        assertThat(avklaringsbehov.erÅpent()).isTrue

        avklaringsbehovene.løsAvklaringsbehov(Definisjon.AVKLAR_SYKDOM, begrunnelse = "Derfor", endretAv = "Meg")

        assertThat(avklaringsbehov.erÅpent()).isFalse
    }
}