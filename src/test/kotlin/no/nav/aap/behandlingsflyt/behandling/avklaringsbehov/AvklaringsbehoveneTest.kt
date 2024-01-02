package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov

import no.nav.aap.behandlingsflyt.avklaringsbehov.Avklaringsbehov
import no.nav.aap.behandlingsflyt.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.behandlingsflyt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.flyt.steg.StegType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class AvklaringsbehoveneTest {

    private val avklaringsbehovRepository = FakeAvklaringsbehovRepository()

    @Test
    fun `skal kunne legge til nytt avklaringsbehov`() {
        val avklaringsbehovene = Avklaringsbehovene(avklaringsbehovRepository, BehandlingId(5))
        val avklaringsbehov = Avklaringsbehov(
            definisjon = Definisjon.AVKLAR_SYKDOM,
            funnetISteg = StegType.AVKLAR_SYKDOM,
            id = 1L,
            kreverToTrinn = null
        )
        avklaringsbehovene.leggTil(
            listOf(avklaringsbehov.definisjon), avklaringsbehov.funnetISteg
        )

        assertThat(avklaringsbehovene.alle()).hasSize(1)
    }

    @Test
    fun `skal ikke legge til duplikate avklaringsbehov`() {
        val avklaringsbehovene = Avklaringsbehovene(avklaringsbehovRepository, BehandlingId(5))
        val avklaringsbehov = Avklaringsbehov(
            definisjon = Definisjon.AVKLAR_SYKDOM,
            funnetISteg = StegType.AVKLAR_SYKDOM,
            id = 1L,
            kreverToTrinn = null
        )
        avklaringsbehovene.leggTil(
            listOf(avklaringsbehov.definisjon), avklaringsbehov.funnetISteg
        )
        val avklaringsbehov1 = Avklaringsbehov(
            definisjon = Definisjon.AVKLAR_SYKDOM,
            funnetISteg = StegType.AVKLAR_SYKDOM,
            id = 1L,
            kreverToTrinn = null
        )
        avklaringsbehovene.leggTil(
            listOf(avklaringsbehov1.definisjon), avklaringsbehov1.funnetISteg
        )

        assertThat(avklaringsbehovene.alle()).hasSize(1)
    }

    @Test
    fun `skal løse avklaringsbehov`() {
        val avklaringsbehovene = Avklaringsbehovene(avklaringsbehovRepository, BehandlingId(5))
        val avklaringsbehov = Avklaringsbehov(
            definisjon = Definisjon.AVKLAR_SYKDOM,
            funnetISteg = StegType.AVKLAR_SYKDOM,
            id = 1L,
            kreverToTrinn = null
        )
        avklaringsbehovene.leggTil(listOf(avklaringsbehov.definisjon), avklaringsbehov.funnetISteg)

        assertThat(avklaringsbehov.erÅpent()).isTrue

        avklaringsbehovene.løsAvklaringsbehov(Definisjon.AVKLAR_SYKDOM, begrunnelse = "Derfor", endretAv = "Meg")

        assertThat(avklaringsbehovene.hentBehovForDefinisjon(Definisjon.AVKLAR_SYKDOM)!!.erÅpent()).isFalse()
    }

    @Test
    fun `forsøk på å løse et avklaringsbehov som ikke finnes skal gi exception`() {
        val avklaringsbehovene = Avklaringsbehovene(avklaringsbehovRepository, BehandlingId(5))
        val avklaringsbehov = Avklaringsbehov(
            definisjon = Definisjon.AVKLAR_SYKDOM,
            funnetISteg = StegType.AVKLAR_SYKDOM,
            id = 1L,
            kreverToTrinn = null
        )
        avklaringsbehovene.leggTil(listOf(avklaringsbehov.definisjon), avklaringsbehov.funnetISteg)

        assertThat(avklaringsbehov.erÅpent()).isTrue

        assertFailsWith<NoSuchElementException>(
            message = "Collection contains no element matching the predicate.",
            block = {
                avklaringsbehovene.løsAvklaringsbehov(
                    Definisjon.MANUELT_SATT_PÅ_VENT,
                    begrunnelse = "Derfor",
                    endretAv = "Meg"
                )
            }
        )
    }

    @Test
    fun `skal returnere alle åpne avklaringsbehov`() {
        val avklaringsbehovene = Avklaringsbehovene(avklaringsbehovRepository, BehandlingId(5))
        val avklaringsbehov = Avklaringsbehov(
            definisjon = Definisjon.AVKLAR_SYKDOM,
            funnetISteg = StegType.AVKLAR_SYKDOM,
            id = 1L,
            kreverToTrinn = null
        )
        avklaringsbehovene.leggTil(
            listOf(avklaringsbehov.definisjon), avklaringsbehov.funnetISteg
        )
        val avklaringsbehov1 = Avklaringsbehov(
            definisjon = Definisjon.FATTE_VEDTAK,
            funnetISteg = StegType.FATTE_VEDTAK,
            id = 1L,
            kreverToTrinn = null
        )
        avklaringsbehovene.leggTil(
            listOf(avklaringsbehov1.definisjon), avklaringsbehov1.funnetISteg
        )

        assertThat(avklaringsbehovene.åpne()).hasSize(2)

        avklaringsbehovene.løsAvklaringsbehov(Definisjon.AVKLAR_SYKDOM, begrunnelse = "Derfor", endretAv = "Meg")

        assertThat(avklaringsbehovene.åpne()).hasSize(1)
    }
}