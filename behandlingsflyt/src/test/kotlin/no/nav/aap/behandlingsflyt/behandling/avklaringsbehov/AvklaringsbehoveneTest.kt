package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov

import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.test.april
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryAvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Tid
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class AvklaringsbehoveneTest {

    private val avklaringsbehovRepository = InMemoryAvklaringsbehovRepository

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
        val avklaringsbehovene = Avklaringsbehovene(avklaringsbehovRepository, BehandlingId(6))
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
    fun `oppdaterer funnet i steg på avklaringsbehov for SKRIV_BREV når det legges til på nytt`() {
        val avklaringsbehovene = Avklaringsbehovene(avklaringsbehovRepository, BehandlingId(7))
        val avklaringsbehov1 = Avklaringsbehov(
            definisjon = Definisjon.SKRIV_BREV,
            funnetISteg = StegType.AVKLAR_SYKDOM,
            id = 2L,
            kreverToTrinn = null
        )
        avklaringsbehovene.leggTil(
            listOf(avklaringsbehov1.definisjon), avklaringsbehov1.funnetISteg
        )

        assertThat(avklaringsbehovene.alle()).hasSize(1)
        assertThat(avklaringsbehovene.alle().first().definisjon).isEqualTo(Definisjon.SKRIV_BREV)
        assertThat(avklaringsbehovene.alle().first().funnetISteg).isEqualTo(StegType.AVKLAR_SYKDOM)

        avklaringsbehovene.løsAvklaringsbehov(Definisjon.SKRIV_BREV, begrunnelse = "", endretAv = "")

        val avklaringsbehov2 = Avklaringsbehov(
            definisjon = Definisjon.SKRIV_BREV,
            funnetISteg = StegType.BREV,
            id = 1L,
            kreverToTrinn = null
        )
        avklaringsbehovene.leggTil(
            listOf(avklaringsbehov2.definisjon), avklaringsbehov2.funnetISteg
        )
        assertThat(avklaringsbehovene.alle()).hasSize(1)
        assertThat(avklaringsbehovene.alle().first().definisjon).isEqualTo(Definisjon.SKRIV_BREV)
        assertThat(avklaringsbehovene.alle().first().funnetISteg).isEqualTo(StegType.BREV)
    }

    @Test
    fun `skal løse avklaringsbehov`() {
        val avklaringsbehovene = Avklaringsbehovene(avklaringsbehovRepository, BehandlingId(8))
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
        val avklaringsbehovene = Avklaringsbehovene(avklaringsbehovRepository, BehandlingId(599))
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

    @Test
    fun `skal kunne oppdatere perioder avklaringsbehovet gjelder`() {
        val avklaringsbehovene = Avklaringsbehovene(avklaringsbehovRepository, BehandlingId(599))
        val avklaringsbehov = Avklaringsbehov(
            definisjon = Definisjon.AVKLAR_SYKDOM,
            funnetISteg = StegType.AVKLAR_SYKDOM,
            id = 1L,
            kreverToTrinn = null,
        )
        val gamlePerioder = setOf(
            Periode(1 januar 2021, Tid.MAKS),
        )
        avklaringsbehovene.leggTil(
            listOf(avklaringsbehov.definisjon),
            avklaringsbehov.funnetISteg,
            perioderSomIkkeErTilstrekkeligVurdert = gamlePerioder
        )

        assertThat(avklaringsbehovene.åpne()).hasSize(1)
        assertThat(avklaringsbehovene.hentBehovForDefinisjon(Definisjon.AVKLAR_SYKDOM)?.perioder())
            .isEqualTo(gamlePerioder)

        val nyePerioder = setOf(
            Periode(1 januar 2021, 1 april 2022),
            Periode(10 april 2022, Tid.MAKS)
        )
        avklaringsbehovene.oppdaterPerioder(Definisjon.AVKLAR_SYKDOM, nyePerioder)

        assertThat(avklaringsbehovene.åpne()).hasSize(1)
        assertThat(avklaringsbehovene.hentBehovForDefinisjon(Definisjon.AVKLAR_SYKDOM)?.perioder())
            .isEqualTo(nyePerioder)

    }
}