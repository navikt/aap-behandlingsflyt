package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov

import no.nav.aap.behandlingsflyt.flyt.steg.StegType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class AvklaringsbehoveneTest {

    @Test
    fun `skal kunne legge til nytt avklaringsbehov`() {
        val avklaringsbehovene = Avklaringsbehovene()
        avklaringsbehovene.leggTil(Avklaringsbehov(definisjon = Definisjon.AVKLAR_SYKDOM, funnetISteg = StegType.AVKLAR_SYKDOM, id = 1L))

        assertThat(avklaringsbehovene.alle()).hasSize(1)
    }

    @Test
    fun `skal ikke legge til duplikate avklaringsbehov`() {
        val avklaringsbehovene = Avklaringsbehovene()
        avklaringsbehovene.leggTil(Avklaringsbehov(definisjon = Definisjon.AVKLAR_SYKDOM, funnetISteg = StegType.AVKLAR_SYKDOM, id = 1L))
        avklaringsbehovene.leggTil(Avklaringsbehov(definisjon = Definisjon.AVKLAR_SYKDOM, funnetISteg = StegType.AVKLAR_SYKDOM, id = 1L))

        assertThat(avklaringsbehovene.alle()).hasSize(1)
    }

    @Test
    fun `skal løse avklaringsbehov`() {
        val avklaringsbehovene = Avklaringsbehovene()
        val avklaringsbehov = Avklaringsbehov(definisjon = Definisjon.AVKLAR_SYKDOM, funnetISteg = StegType.AVKLAR_SYKDOM, id = 1L)
        avklaringsbehovene.leggTil(avklaringsbehov)

        assertThat(avklaringsbehov.erÅpent()).isTrue

        avklaringsbehovene.løsAvklaringsbehov(Definisjon.AVKLAR_SYKDOM, begrunnelse = "Derfor", endretAv = "Meg")

        assertThat(avklaringsbehov.erÅpent()).isFalse
    }

    @Test
    fun `forsøk på å løse et avklaringsbehov som ikke finnes skal gi exception`() {
        val avklaringsbehovene = Avklaringsbehovene()
        val avklaringsbehov = Avklaringsbehov(definisjon = Definisjon.AVKLAR_SYKDOM, funnetISteg = StegType.AVKLAR_SYKDOM, id = 1L)
        avklaringsbehovene.leggTil(avklaringsbehov)

        assertThat(avklaringsbehov.erÅpent()).isTrue

        assertFailsWith<NoSuchElementException> (
            message = "Collection contains no element matching the predicate.",
            block = {
                avklaringsbehovene.løsAvklaringsbehov(Definisjon.MANUELT_SATT_PÅ_VENT, begrunnelse = "Derfor", endretAv = "Meg")
            }
        )
    }

    @Test
    fun `skal returnere alle åpne avklaringsbehov`() {
        val avklaringsbehovene = Avklaringsbehovene()
        avklaringsbehovene.leggTil(Avklaringsbehov(definisjon = Definisjon.AVKLAR_SYKDOM, funnetISteg = StegType.AVKLAR_SYKDOM, id = 1L))
        avklaringsbehovene.leggTil(Avklaringsbehov(definisjon = Definisjon.FATTE_VEDTAK, funnetISteg = StegType.FATTE_VEDTAK, id = 1L))

        assertThat(avklaringsbehovene.åpne()).hasSize(2)

        avklaringsbehovene.løsAvklaringsbehov(Definisjon.AVKLAR_SYKDOM, begrunnelse = "Derfor", endretAv = "Meg")

        assertThat(avklaringsbehovene.åpne()).hasSize(1)
    }

    @Test
    fun `skal kunne legge til en liste med avklaringsbehov funnet i et gitt steg`() {
        val avklaringsbehovene = Avklaringsbehovene()
        avklaringsbehovene.leggTil(listOf(Definisjon.AVKLAR_SYKDOM, Definisjon.FATTE_VEDTAK), StegType.AVKLAR_SYKDOM)
        assertThat(avklaringsbehovene.alle()).hasSize(2)
    }

}