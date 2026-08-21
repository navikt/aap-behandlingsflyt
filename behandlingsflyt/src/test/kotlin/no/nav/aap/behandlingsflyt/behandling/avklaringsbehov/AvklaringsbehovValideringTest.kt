package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov

import io.mockk.every
import io.mockk.mockk
import no.nav.aap.behandlingsflyt.SYSTEMBRUKER
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarSykdomLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.PeriodisertAvklaringsbehovLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Avslagsårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.gjeldendeVurderinger
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.Kravreferanse
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.RelevantKrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.Søknadsdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.SøknadsdatoÅrsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangarbeid.flate.OvergangArbeidVurderingLøsningDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.stønadsperiode.RelevantKravType
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.stønadsperiode.StønadsperiodeVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.ArbeidsevneNedsattValg
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.flate.SykdomsvurderingLøsningDto
import no.nav.aap.behandlingsflyt.help.assertTidslinje
import no.nav.aap.behandlingsflyt.help.opprettInMemorySak
import no.nav.aap.behandlingsflyt.integrasjon.createGatewayProvider
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekst
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.test.LokalUnleash
import no.nav.aap.behandlingsflyt.test.april
import no.nav.aap.behandlingsflyt.test.februar
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryAvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryKravRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryStønadsperiodeRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.inMemoryRepositoryProvider
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.behandlingsflyt.test.mars
import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.komponenter.verdityper.Tid
import no.nav.aap.verdityper.dokument.JournalpostId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.time.Instant
import java.time.LocalDate
import java.util.concurrent.atomic.*

class AvklaringsbehovValideringTest {
    private val avklaringsbehovRepository = InMemoryAvklaringsbehovRepository
    private val løsningMock = mockk<PeriodisertAvklaringsbehovLøsning<OvergangArbeidVurderingLøsningDto>>(relaxed = true)

    val avklaringsbehovValidering = AvklaringsbehovValidering(inMemoryRepositoryProvider, createGatewayProvider {
        register<LokalUnleash>()
    })

    private fun lagFlytKontekst(
        sakId: SakId = opprettInMemorySak().id,
        behandlingId: BehandlingId,
        forrigeBehandlingId: BehandlingId? = null
    ): FlytKontekst {
        return FlytKontekst(
            behandlingId = behandlingId,
            sakId = sakId,
            forrigeBehandlingId = forrigeBehandlingId,
            behandlingType = TypeBehandling.Førstegangsbehandling
        )
    }

    private val idTeller = AtomicLong(90_000L)
    private val brukteBehandlingIder = mutableListOf<BehandlingId>()

    private fun nesteBehandlingId(): BehandlingId =
        BehandlingId(idTeller.incrementAndGet()).also { brukteBehandlingIder.add(it) }

    @AfterEach
    fun ryddOpp() {
        brukteBehandlingIder.forEach { InMemoryKravRepository.slett(it) }
        brukteBehandlingIder.clear()
    }


    @BeforeEach
    fun setup() {
        // TODO: Skriv om fra mocks
        every { løsningMock.hentLagredeLøstePerioder(any(), any()) } returns Tidslinje<Unit>()
        every { løsningMock.definisjon() } returns Definisjon.AVKLAR_SYKDOM
    }

    @Test
    fun `Periodisert løsning må dekke periodene avklaringsbehovet ber om`() {
        val avklaringsbehovene = Avklaringsbehovene(avklaringsbehovRepository, BehandlingId(9))
        val flytKontekst = lagFlytKontekst(behandlingId = BehandlingId(9))
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
            avklaringsbehovValidering.validerPerioder(
                Bruker("saksbehandler"),
                avklaringsbehovene = avklaringsbehovene,
                kontekst = flytKontekst,
                løsning = løsningMock,
            )
        }

        assertThat(exception.message).isEqualTo("Du mangler vurdering for 01.03.2021–01.04.2021")

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
            avklaringsbehovValidering.validerPerioder(
                Bruker("saksbehandler"),
                avklaringsbehovene = avklaringsbehovene,
                kontekst = flytKontekst,
                løsning = løsningMock,
            )
        }
    }

    @Test
    fun `Avklaringsbehov med tom mengde med perioder som skal vurderes skal ikke bry seg om perioder`() {
        val avklaringsbehovValidering = AvklaringsbehovValidering(inMemoryRepositoryProvider, createGatewayProvider {
            register<LokalUnleash>()
        })
        val avklaringsbehovene = Avklaringsbehovene(avklaringsbehovRepository, BehandlingId(10))
        val flytKontekst = lagFlytKontekst(behandlingId = BehandlingId(10))
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
            avklaringsbehovValidering.validerPerioder(
                Bruker("saksbehandler"),
                avklaringsbehovene = avklaringsbehovene,
                kontekst = flytKontekst,
                løsning = løsningMock,
            )
        }
    }

    @Test
    fun `Avklaringsbehov med null-perioder som skal vurderes skal ikke bry seg om perioder`() {
        val avklaringsbehovValidering = AvklaringsbehovValidering(inMemoryRepositoryProvider, createGatewayProvider {
            register<LokalUnleash>()
        })
        val avklaringsbehovene = Avklaringsbehovene(avklaringsbehovRepository, BehandlingId(11))
        val flytKontekst = lagFlytKontekst(behandlingId = BehandlingId(11))
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
            avklaringsbehovValidering.validerPerioder(
                Bruker("saksbehandler"),
                avklaringsbehovene = avklaringsbehovene,
                kontekst = flytKontekst,
                løsning = løsningMock,
            )
        }
    }

    @Test
    fun `Periodisert løsning skal validere OK om periodene avklaringsbehovet ber om er dekket av tidligere vurderinger`() {
        val avklaringsbehovValidering = AvklaringsbehovValidering(inMemoryRepositoryProvider, createGatewayProvider {
            register<LokalUnleash>()
        })
        val avklaringsbehovene = Avklaringsbehovene(avklaringsbehovRepository, BehandlingId(15))
        val flytKontekst = lagFlytKontekst(behandlingId = BehandlingId(15), forrigeBehandlingId = BehandlingId(14))
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
        every { løsningMock.hentLagredeLøstePerioder(any(), any()) } returns Tidslinje(
            initSegmenter = listOf(
                Segment(periode = Periode(1 januar 2021, 1 februar 2021), verdi = Unit),
                Segment(periode = Periode(1 mars 2021, Tid.MAKS), verdi = Unit),
            )
        )
        every { løsningMock.løsningerForPerioder } returns emptyList()

        assertDoesNotThrow {
            avklaringsbehovValidering.validerPerioder(
                Bruker("saksbehandler"),
                avklaringsbehovene = avklaringsbehovene,
                kontekst = flytKontekst,
                løsning = løsningMock,
            )
        }
    }

    @Test
    fun `ingen krav mangler løsning når det ikke finnes kravgrunnlag for behandlingen`() {
        val behandlingId = nesteBehandlingId()
        val forrigeBehandlingId = nesteBehandlingId()
        val kontekst = lagFlytKontekst(behandlingId = behandlingId, forrigeBehandlingId = forrigeBehandlingId)
        // Ingen lagring i kravRepository

        val gjeldendeVurderinger =
            tomLøsning().somVurderinger(Bruker("saksbehandler"), forrigeBehandlingId).gjeldendeVurderinger()
        val resultat =
            avklaringsbehovValidering.nårKravHarLøsning(tomLøsning().definisjon(), gjeldendeVurderinger, kontekst)

        assertThat(resultat.segmenter()).isEmpty()
    }

    @Test
    fun `Krav mangler løsning selv om kravet ble vedtatt i forrige behandling`() {
        val behandlingId = nesteBehandlingId()
        val forrigeBehandlingId = nesteBehandlingId()
        val kontekst = lagFlytKontekst(behandlingId = behandlingId, forrigeBehandlingId = forrigeBehandlingId)
        // Krav er vurdert i forrigeBehandlingId, ikke i inneværende behandlingId
        val nyttKrav = nyttKrav(forrigeBehandlingId, LocalDate.now())
        InMemoryKravRepository.lagre(forrigeBehandlingId, setOf(nyttKrav))
        InMemoryStønadsperiodeRepository.lagre(
            forrigeBehandlingId, setOf(
                stønadsperiodevurering(
                    nyttKrav,
                    forrigeBehandlingId,
                    RelevantKravType.NY_STØNADSPERIODE,
                    nyttKrav.muligRettFra
                )
            )
        )

        InMemoryKravRepository.kopier(forrigeBehandlingId, behandlingId)
        InMemoryStønadsperiodeRepository.kopier(forrigeBehandlingId, behandlingId)

        val gjeldendeVurderinger =
            tomLøsning().somVurderinger(Bruker("saksbehandler"), forrigeBehandlingId).gjeldendeVurderinger()
        val resultat =
            avklaringsbehovValidering.nårKravHarLøsning(tomLøsning().definisjon(), gjeldendeVurderinger, kontekst)

        assertTidslinje(
            resultat,
            Periode(nyttKrav.muligRettFra, Tid.MAKS) to { assertFalse(it) }
        )
    }

    private fun stønadsperiodevurering(
        nyttKrav: RelevantKrav,
        vurdertIBehandling: BehandlingId? = null,
        relevantKravType: RelevantKravType,
        startDato: LocalDate = nyttKrav.muligRettFra,
    ): StønadsperiodeVurdering = StønadsperiodeVurdering(
        referanse = nyttKrav.referanse,
        opprettet = Instant.now(),
        vurdertIBehandling = vurdertIBehandling ?: nyttKrav.vurdertIBehandling,
        vurdertAv = SYSTEMBRUKER,
        begrunnelse = "",
        harHattOrdinærSiste52Uker = true,
        harGjenværendeKvote = when (relevantKravType) {
            RelevantKravType.AVSLAG -> TODO()
            RelevantKravType.GJENINNTREDEN_ETTER_OPPHØR -> true
            is RelevantKravType.GJENOPPTAK_ETTER_STANS -> true
            RelevantKravType.NY_STØNADSPERIODE -> false
        },
        relevantKravType = relevantKravType,
        startDato = startDato,
    )

    @Test
    fun `NyttKrav er dekket når løsning fom er lik muligRettFra`() {
        val behandlingId = nesteBehandlingId()
        val forrigeBehandlingId = nesteBehandlingId()
        val muligRettFra = LocalDate.of(2024, 1, 1)
        val kontekst = lagFlytKontekst(behandlingId = behandlingId, forrigeBehandlingId = forrigeBehandlingId)
        val krav = nyttKrav(behandlingId, muligRettFra)
        InMemoryKravRepository.lagre(behandlingId, setOf(krav))
        InMemoryStønadsperiodeRepository.lagre(
            behandlingId, setOf(
                stønadsperiodevurering(
                    krav,
                    behandlingId,
                    RelevantKravType.NY_STØNADSPERIODE,
                    krav.muligRettFra
                )
            )
        )

        val løsning = løsning(fom = muligRettFra)
        val gjeldendeVurderinger =
            løsning.somVurderinger(Bruker("saksbehandler"), forrigeBehandlingId).gjeldendeVurderinger()

        val resultat = avklaringsbehovValidering.nårKravHarLøsning(løsning.definisjon(), gjeldendeVurderinger, kontekst)

        assertTidslinje(
            resultat,
            Periode(muligRettFra, Tid.MAKS) to { assertTrue(it) }
        )
    }

    @Test
    fun `NyttKrav er dekket når løsning fom er etter muligRettFra`() {
        val behandlingId = nesteBehandlingId()
        val forrigeBehandlingId = nesteBehandlingId()
        val muligRettFra = LocalDate.of(2024, 1, 1)
        val kontekst = lagFlytKontekst(behandlingId = behandlingId, forrigeBehandlingId = forrigeBehandlingId)
        val krav = nyttKrav(behandlingId, muligRettFra)
        InMemoryKravRepository.lagre(behandlingId, setOf(krav))
        InMemoryStønadsperiodeRepository.lagre(
            behandlingId, setOf(
                stønadsperiodevurering(
                    krav,
                    relevantKravType = RelevantKravType.NY_STØNADSPERIODE,
                    startDato = krav.muligRettFra
                )
            )
        )
        val løsning = løsning(fom = muligRettFra.plusDays(2))
        val gjeldendeVurderinger =
            løsning.somVurderinger(Bruker("saksbehandler"), forrigeBehandlingId).gjeldendeVurderinger()

        val resultat = avklaringsbehovValidering.nårKravHarLøsning(løsning.definisjon(), gjeldendeVurderinger, kontekst)

        assertTidslinje(
            resultat,
            Periode(muligRettFra, Tid.MAKS) to { assertTrue(it) }
        )
    }

    @Test
    fun `NyttKrav er ikke dekket når løsning fom er før muligRettFra`() {
        val behandlingId = nesteBehandlingId()
        val forrigeBehandlingId = nesteBehandlingId()
        val muligRettFra = LocalDate.of(2024, 6, 1)
        val kontekst = lagFlytKontekst(behandlingId = behandlingId, forrigeBehandlingId = forrigeBehandlingId)
        val krav = nyttKrav(behandlingId, muligRettFra)
        InMemoryKravRepository.lagre(behandlingId, setOf(krav))
        InMemoryStønadsperiodeRepository.lagre(
            behandlingId, setOf(
                stønadsperiodevurering(
                    krav,
                    relevantKravType = RelevantKravType.NY_STØNADSPERIODE,
                    startDato = krav.muligRettFra
                )
            )
        )

        val løsningFom = muligRettFra.minusDays(1)
        val løsning = løsning(fom = løsningFom)
        val gjeldendeVurderinger =
            løsning.somVurderinger(Bruker("saksbehandler"), forrigeBehandlingId).gjeldendeVurderinger()
        val resultat = avklaringsbehovValidering.nårKravHarLøsning(løsning.definisjon(), gjeldendeVurderinger, kontekst)

        assertTidslinje(
            resultat,
            Periode(krav.muligRettFra, Tid.MAKS) to {
                assertFalse(it)
            }
        )
    }

    @Test
    fun `Gjenopptak er dekket når ingen stans eller opphør er registrert`() {
        val behandlingId = nesteBehandlingId()
        val forrigeBehandlingId = nesteBehandlingId()
        val muligRettFra = LocalDate.of(2024, 1, 1)
        val kontekst = lagFlytKontekst(behandlingId = behandlingId, forrigeBehandlingId = forrigeBehandlingId)
        val krav = nyttKrav(behandlingId, muligRettFra)

        InMemoryKravRepository.lagre(behandlingId, setOf(krav))
        InMemoryStønadsperiodeRepository.lagre(behandlingId, setOf(stønadsperiodevurering(
            krav,
            relevantKravType = RelevantKravType.GJENOPPTAK_ETTER_STANS(listOf())
        )))

        val løsning = løsning(fom = muligRettFra.minusDays(1))
        val gjeldendeVurderinger = løsning.somVurderinger(Bruker("saksbehandler"),forrigeBehandlingId).gjeldendeVurderinger()

        val resultat = avklaringsbehovValidering.nårKravHarLøsning(løsning.definisjon(), gjeldendeVurderinger, kontekst)

        assertTidslinje(
            resultat,
            Periode(muligRettFra, Tid.MAKS) to {
                assertTrue(it)
            }
        )
}

    @Test
    fun `Skal ikke løfte avklaringsbehov for bistand ved gjenopptak etter stans på sykdom`() {
        val behandlingId = nesteBehandlingId()
        val forrigeBehandlingId = nesteBehandlingId()
        val muligRettFra = LocalDate.of(2027, 1, 1)
        val kontekst = lagFlytKontekst(behandlingId = behandlingId, forrigeBehandlingId = forrigeBehandlingId)
        val krav = nyttKrav(behandlingId, muligRettFra)
        InMemoryKravRepository.lagre(behandlingId, setOf(krav))
        InMemoryStønadsperiodeRepository.lagre(behandlingId, setOf(stønadsperiodevurering(
            krav,
            relevantKravType = RelevantKravType.GJENOPPTAK_ETTER_STANS(listOf(Avslagsårsak.IKKE_SYKDOM_SKADE_LYTE))
        )))

        val løsningFom = muligRettFra.minusDays(30)
        val løsning = løsning(fom = løsningFom)
        val gjeldendeVurderinger =
            løsning.somVurderinger(Bruker("saksbehandler"), forrigeBehandlingId).gjeldendeVurderinger()

        // Løsning dekker ikke muligRettFra, men Stans betyr at kravet likevel er dekket
        val resultat = avklaringsbehovValidering.nårKravHarLøsning(
            Definisjon.AVKLAR_BISTANDSBEHOV,
            gjeldendeVurderinger,
            kontekst
        )

        assertTidslinje(
            resultat,
            Periode(muligRettFra, Tid.MAKS) to { assertTrue(it) }
        )
    }

    @Test
    fun `Skal løfte avklaringsbehov for sykdom ved gjenopptak etter stans på sykdom`() {
        val behandlingId = nesteBehandlingId()
        val forrigeBehandlingId = nesteBehandlingId()
        val muligRettFra = LocalDate.of(2027, 1, 1)
        val kontekst = lagFlytKontekst(behandlingId = behandlingId, forrigeBehandlingId = forrigeBehandlingId)
        val krav = nyttKrav(behandlingId, muligRettFra)
        InMemoryKravRepository.lagre(behandlingId, setOf(krav))
        InMemoryStønadsperiodeRepository.lagre(behandlingId, setOf(stønadsperiodevurering(
            krav,
            relevantKravType = RelevantKravType.GJENOPPTAK_ETTER_STANS(listOf(Avslagsårsak.IKKE_SYKDOM_SKADE_LYTE))
        )))

        val løsningFom = muligRettFra.minusDays(30)
        val løsning = løsning(fom = løsningFom)
        val gjeldendeVurderinger =
            løsning.somVurderinger(Bruker("saksbehandler"), forrigeBehandlingId).gjeldendeVurderinger()

        // Løsning dekker ikke muligRettFra, men Stans betyr at kravet likevel er dekket
        val resultat = avklaringsbehovValidering.nårKravHarLøsning(
            Definisjon.AVKLAR_SYKDOM,
            gjeldendeVurderinger,
            kontekst
        )

        assertTidslinje(
            resultat,
            Periode(muligRettFra, Tid.MAKS) to { assertFalse(it) }
        )
    }

    @Test
    fun `Gjenopptak etter opphør er dekket når løsning dekker muligRettFra`() {
        val behandlingId = nesteBehandlingId()
        val forrigeBehandlingId = nesteBehandlingId()
        val muligRettFra = LocalDate.of(2027, 1, 1)
        val kontekst = lagFlytKontekst(behandlingId = behandlingId, forrigeBehandlingId = forrigeBehandlingId)
        val krav = nyttKrav(behandlingId, muligRettFra)
        InMemoryStønadsperiodeRepository.lagre(
            behandlingId, setOf(
                stønadsperiodevurering(
                    krav,
                    relevantKravType = RelevantKravType.GJENINNTREDEN_ETTER_OPPHØR,
                    startDato = krav.muligRettFra,
                )
            )
        )
        InMemoryKravRepository.lagre(behandlingId, setOf(krav))

        val løsning = løsning(fom = muligRettFra)
        val gjeldendeVurderinger =
            løsning.somVurderinger(Bruker("saksbehandler"), forrigeBehandlingId).gjeldendeVurderinger()

        val resultat = avklaringsbehovValidering.nårKravHarLøsning(løsning.definisjon(), gjeldendeVurderinger, kontekst)

        assertTidslinje(
            resultat,
            Periode(muligRettFra, Tid.MAKS) to {
                assertTrue(it)
            }
        )
    }

    @Test
    fun `Gjeninntreden etter Opphør er ikke dekket når løsning ikke dekker muligRettFra`() {
        val behandlingId = nesteBehandlingId()
        val forrigeBehandlingId = nesteBehandlingId()
        val muligRettFra = LocalDate.of(2027, 1, 1)
        val kontekst = lagFlytKontekst(behandlingId = behandlingId, forrigeBehandlingId = forrigeBehandlingId)
        val krav = nyttKrav(behandlingId, muligRettFra)
        InMemoryKravRepository.lagre(behandlingId, setOf(krav))
        InMemoryStønadsperiodeRepository.lagre(
            behandlingId, setOf(
                stønadsperiodevurering(
                    krav,
                    relevantKravType = RelevantKravType.GJENINNTREDEN_ETTER_OPPHØR,
                    startDato = krav.muligRettFra,
                )
            )
        )
        val løsningFom = muligRettFra.minusDays(1)
        val løsning = løsning(løsningFom)
        val gjeldendeVurderinger =
            løsning.somVurderinger(Bruker("saksbehandler"), forrigeBehandlingId).gjeldendeVurderinger()

        val resultat = avklaringsbehovValidering.nårKravHarLøsning(løsning.definisjon(), gjeldendeVurderinger, kontekst)


        assertTidslinje(
            resultat,
            Periode(muligRettFra, Tid.MAKS) to {
                assertFalse(it)
            }
        )
    }

    private fun løsning(fom: LocalDate) = AvklarSykdomLøsning(
        løsningerForPerioder = listOf(
            SykdomsvurderingLøsningDto(
                begrunnelse = "Er syk nok",
                dokumenterBruktIVurdering = emptyList(),
                harSkadeSykdomEllerLyte = true,
                erSkadeSykdomEllerLyteVesentligdel = true,
                erNedsettelseIArbeidsevneMerEnnHalvparten = true,
                erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = null,
                harNedsattArbeidsevne = ArbeidsevneNedsattValg.JA,
                yrkesskadeBegrunnelse = null,
                fom = fom,
                tom = null,
            )
        )
    )

    private fun tomLøsning() = AvklarSykdomLøsning(emptyList())

    private fun nyttKrav(behandlingId: BehandlingId, muligRettFra: LocalDate) = RelevantKrav(
        referanse = Kravreferanse.ny(),
        journalpostId = JournalpostId("JP-001"),
        vurdertAv = Bruker("Z123456"),
        begrunnelse = "Nytt krav",
        vurdertIBehandling = behandlingId,
        opprettet = Instant.now(),
        søknadsdato = Søknadsdato(muligRettFra, SøknadsdatoÅrsak.SøknadMottatt),
        overstyrMuligRettFra = null,
        muligRettFra = muligRettFra,
    )
}