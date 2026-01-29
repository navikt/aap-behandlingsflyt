package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov

import io.mockk.every
import io.mockk.mockk
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.PeriodisertAvklaringsbehovLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangarbeid.flate.OvergangArbeidVurderingLøsningDto
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekst
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.test.april
import no.nav.aap.behandlingsflyt.test.februar
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryAvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.behandlingsflyt.test.mars
import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException
import no.nav.aap.komponenter.repository.RepositoryProvider
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Tid
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertFailsWith

class AvklaringsbehoveneTest {

    private val avklaringsbehovRepository = InMemoryAvklaringsbehovRepository
    private val repositoryProviderMock = mockk<RepositoryProvider>()
    private val løsningMock = mockk<PeriodisertAvklaringsbehovLøsning<OvergangArbeidVurderingLøsningDto>>()

    @BeforeEach
    fun setup() {
        every { løsningMock.hentLagredeLøstePerioder(any(), any()) } returns Tidslinje<Unit>()
        every { løsningMock.definisjon() } returns Definisjon.AVKLAR_SYKDOM
    }


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
            avklaringsbehov.definisjon, avklaringsbehov.funnetISteg, null, null
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
            avklaringsbehov.definisjon, avklaringsbehov.funnetISteg, null, null
        )
        val avklaringsbehov1 = Avklaringsbehov(
            definisjon = Definisjon.AVKLAR_SYKDOM,
            funnetISteg = StegType.AVKLAR_SYKDOM,
            id = 1L,
            kreverToTrinn = null
        )
        avklaringsbehovene.leggTil(
            avklaringsbehov1.definisjon, avklaringsbehov1.funnetISteg, null, null
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
            avklaringsbehov1.definisjon, avklaringsbehov1.funnetISteg, null, null
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
            avklaringsbehov2.definisjon, avklaringsbehov2.funnetISteg, null, null
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
        avklaringsbehovene.leggTil(avklaringsbehov.definisjon, avklaringsbehov.funnetISteg, null, null)

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
        avklaringsbehovene.leggTil(avklaringsbehov.definisjon, avklaringsbehov.funnetISteg, null, null)

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
            avklaringsbehov.definisjon, avklaringsbehov.funnetISteg, null, null
        )
        val avklaringsbehov1 = Avklaringsbehov(
            definisjon = Definisjon.FATTE_VEDTAK,
            funnetISteg = StegType.FATTE_VEDTAK,
            id = 1L,
            kreverToTrinn = null
        )
        avklaringsbehovene.leggTil(
            avklaringsbehov1.definisjon, avklaringsbehov1.funnetISteg, null, null
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
            avklaringsbehov.definisjon,
            avklaringsbehov.funnetISteg,
            perioderSomIkkeErTilstrekkeligVurdert = gamlePerioder,
            perioderVedtaketBehøverVurdering = gamlePerioder,
        )

        assertThat(avklaringsbehovene.åpne()).hasSize(1)
        assertThat(
            avklaringsbehovene.hentBehovForDefinisjon(Definisjon.AVKLAR_SYKDOM)?.perioderVedtaketBehøverVurdering()
        )
            .isEqualTo(gamlePerioder)

        val nyePerioder = setOf(
            Periode(1 januar 2021, 1 april 2022),
            Periode(10 april 2022, Tid.MAKS)
        )
        avklaringsbehovene.oppdaterPerioder(Definisjon.AVKLAR_SYKDOM, nyePerioder, nyePerioder)

        assertThat(avklaringsbehovene.åpne()).hasSize(1)
        assertThat(
            avklaringsbehovene.hentBehovForDefinisjon(Definisjon.AVKLAR_SYKDOM)?.perioderVedtaketBehøverVurdering()
        )
            .isEqualTo(nyePerioder)
        assertThat(
            avklaringsbehovene.hentBehovForDefinisjon(Definisjon.AVKLAR_SYKDOM)?.perioderSomIkkeErTilstrekkeligVurdert()
        )
            .isEqualTo(nyePerioder)

    }

    @Test
    fun `Periodisert løsning må dekke periodene avklaringsbehovet ber om`() {
        val avklaringsbehovene = Avklaringsbehovene(avklaringsbehovRepository, BehandlingId(9))
        val flytKontekst = lagFlytKontekst(BehandlingId(9))
        val avklaringsbehov = Avklaringsbehov(
            definisjon = Definisjon.AVKLAR_OVERGANG_ARBEID,
            funnetISteg = StegType.OVERGANG_ARBEID,
            id = 1L,
            kreverToTrinn = null
        )
        avklaringsbehovene.leggTil(
            perioderVedtaketBehøverVurdering =
                setOf(Periode(1 januar 2021, 1 februar 2021), Periode(1 mars 2021, 1 april 2021)),
            perioderSomIkkeErTilstrekkeligVurdert = setOf(
                Periode(1 januar 2021, 1 februar 2021),
                Periode(1 mars 2021, 1 april 2021)
            ),
            definisjon =
                avklaringsbehov.definisjon, funnetISteg = avklaringsbehov.funnetISteg
        )


        assertThat(avklaringsbehov.erÅpent()).isTrue

        every { løsningMock.definisjon() } returns Definisjon.AVKLAR_OVERGANG_ARBEID
        every { løsningMock.løsningerForPerioder } returns listOf(
            OvergangArbeidVurderingLøsningDto(
                fom = 1 januar 2021,
                tom = 1 februar 2021,
                begrunnelse = "begrunnelse",
                brukerRettPåAAP = false
            )
        )

        val exception = assertThrows<UgyldigForespørselException> {
            avklaringsbehovene.validerPerioder(
                kontekst = flytKontekst,
                løsning = løsningMock,
                repositoryProvider = repositoryProviderMock
            )
        }

        assertThat(exception.message).isEqualTo("Løsning mangler vurdering for perioder: [Periode(fom=2021-03-01, tom=2021-04-01)]")

        every { løsningMock.definisjon() } returns Definisjon.AVKLAR_OVERGANG_ARBEID
        every { løsningMock.løsningerForPerioder } returns listOf(
            OvergangArbeidVurderingLøsningDto(
                fom = 1 januar 2021,
                tom = 1 januar 2022,
                begrunnelse = "begrunnelse",
                brukerRettPåAAP = false
            )
        )

        assertDoesNotThrow {
            avklaringsbehovene.validerPerioder(
                kontekst = flytKontekst,
                løsning = løsningMock,
                repositoryProvider = repositoryProviderMock
            )
        }
    }

    @Test
    fun `Avklaringsbehov med tom mengde med perioder som skal vurderes skal ikke bry seg om perioder`() {
        val avklaringsbehovene = Avklaringsbehovene(avklaringsbehovRepository, BehandlingId(10))
        val flytKontekst = lagFlytKontekst(BehandlingId(10))
        val avklaringsbehov = Avklaringsbehov(
            definisjon = Definisjon.AVKLAR_OVERGANG_ARBEID,
            funnetISteg = StegType.OVERGANG_ARBEID,
            id = 1L,
            kreverToTrinn = null
        )
        avklaringsbehovene.leggTil(
            perioderVedtaketBehøverVurdering = emptySet(),
            perioderSomIkkeErTilstrekkeligVurdert = null,
            definisjon = avklaringsbehov.definisjon,
            funnetISteg = avklaringsbehov.funnetISteg,
        )


        assertThat(avklaringsbehov.erÅpent()).isTrue

        every { løsningMock.definisjon() } returns Definisjon.AVKLAR_OVERGANG_ARBEID
        every { løsningMock.løsningerForPerioder } returns listOf(
            OvergangArbeidVurderingLøsningDto(
                fom = 1 januar 2021,
                tom = 1 januar 2022,
                begrunnelse = "begrunnelse",
                brukerRettPåAAP = false
            )
        )

        assertDoesNotThrow {
            avklaringsbehovene.validerPerioder(
                kontekst = flytKontekst,
                løsning = løsningMock,
                repositoryProvider = repositoryProviderMock
            )
        }
    }

    @Test
    fun `Avklaringsbehov med null-perioder som skal vurderes skal ikke bry seg om perioder`() {
        val avklaringsbehovene = Avklaringsbehovene(avklaringsbehovRepository, BehandlingId(11))
        val flytKontekst = lagFlytKontekst(BehandlingId(11))
        val avklaringsbehov = Avklaringsbehov(
            definisjon = Definisjon.AVKLAR_OVERGANG_ARBEID,
            funnetISteg = StegType.OVERGANG_ARBEID,
            id = 1L,
            kreverToTrinn = null
        )
        avklaringsbehovene.leggTil(
            perioderVedtaketBehøverVurdering = null,
            perioderSomIkkeErTilstrekkeligVurdert = null,
            definisjon = avklaringsbehov.definisjon,
            funnetISteg = avklaringsbehov.funnetISteg
        )

        assertThat(avklaringsbehov.erÅpent()).isTrue

        every { løsningMock.definisjon() } returns Definisjon.AVKLAR_OVERGANG_ARBEID
        every { løsningMock.løsningerForPerioder } returns listOf(
            OvergangArbeidVurderingLøsningDto(
                fom = 1 januar 2021,
                tom = 1 januar 2022,
                begrunnelse = "begrunnelse",
                brukerRettPåAAP = false
            )
        )

        assertDoesNotThrow {
            avklaringsbehovene.validerPerioder(
                kontekst = flytKontekst,
                løsning = løsningMock,
                repositoryProvider = repositoryProviderMock
            )
        }
    }

    @Test
    fun `Periodisert løsning skal validere OK om periodene avklaringsbehovet ber om er dekket av tidligere vurderinger`() {
        val avklaringsbehovene = Avklaringsbehovene(avklaringsbehovRepository, BehandlingId(15))
        val flytKontekst = lagFlytKontekst(BehandlingId(15))
        val avklaringsbehov = Avklaringsbehov(
            definisjon = Definisjon.AVKLAR_OVERGANG_ARBEID,
            funnetISteg = StegType.OVERGANG_ARBEID,
            id = 1L,
            kreverToTrinn = null
        )
        avklaringsbehovene.leggTil(
            perioderVedtaketBehøverVurdering =
                setOf(Periode(1 januar 2021, 1 februar 2021), Periode(1 mars 2021, 1 april 2021)),
            perioderSomIkkeErTilstrekkeligVurdert = setOf(
                Periode(1 januar 2021, 1 februar 2021),
                Periode(1 mars 2021, 1 april 2021)
            ),
            definisjon =
                avklaringsbehov.definisjon, funnetISteg = avklaringsbehov.funnetISteg
        )


        assertThat(avklaringsbehov.erÅpent()).isTrue

        every { løsningMock.definisjon() } returns Definisjon.AVKLAR_OVERGANG_ARBEID
        every { løsningMock.hentLagredeLøstePerioder(any(), any()) } returns Tidslinje<Unit>(
            initSegmenter = listOf(
                Segment(periode = Periode(1 januar 2021, 1 februar 2021), verdi = Unit),
                Segment(periode = Periode(1 mars 2021, Tid.MAKS), verdi = Unit),
            )
        )
        every { løsningMock.løsningerForPerioder } returns emptyList()

        assertDoesNotThrow {
            avklaringsbehovene.validerPerioder(
                kontekst = flytKontekst,
                løsning = løsningMock,
                repositoryProvider = repositoryProviderMock
            )
        }
    }


    private fun lagFlytKontekst(behandlingId: BehandlingId): FlytKontekst =
        FlytKontekst(
            behandlingId = behandlingId,
            sakId = SakId(1L),
            forrigeBehandlingId = null,
            behandlingType = TypeBehandling.Førstegangsbehandling
        )
}