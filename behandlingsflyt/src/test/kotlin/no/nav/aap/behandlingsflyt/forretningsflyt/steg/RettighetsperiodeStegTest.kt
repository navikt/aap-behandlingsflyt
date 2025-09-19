package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.aap.behandlingsflyt.behandling.avbrytrevurdering.AvbrytRevurderingService
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovService
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.behandlingsflyt.behandling.rettighetsperiode.VurderRettighetsperiodeRepository
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovOgÅrsak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryAvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryBehandlingRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemorySakRepository
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.behandlingsflyt.test.modell.genererIdent
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.EnumSource.Mode
import java.time.LocalDate
import java.util.*

class RettighetsperiodeStegTest {
    private lateinit var vilkårsresultatRepository: VilkårsresultatRepository
    private lateinit var sakService: SakService
    private lateinit var avklaringsbehovRepository: AvklaringsbehovRepository
    private lateinit var tidligereVurderinger: TidligereVurderinger
    private lateinit var rettighetsperiodeRepository: VurderRettighetsperiodeRepository
    private lateinit var steg: RettighetsperiodeSteg
    private val sakRepository = InMemorySakRepository
    private val behandlingRepository = InMemoryBehandlingRepository

    @BeforeEach
    fun setup() {
        vilkårsresultatRepository = mockk(relaxed = true)
        sakService = mockk(relaxed = true)
        avklaringsbehovRepository = mockk()
        tidligereVurderinger = mockk {
            every { muligMedRettTilAAP(any(), StegType.VURDER_RETTIGHETSPERIODE) } returns true
        }
        rettighetsperiodeRepository = mockk(relaxed = true) {
            every { hentVurdering(any()) } returns null
        }

        val avbrytRevurderingService: AvbrytRevurderingService = mockk {
            every { revurderingErAvbrutt(any()) } returns false
        }

        steg = RettighetsperiodeSteg(
            vilkårsresultatRepository,
            sakService,
            avklaringsbehovRepository,
            AvklaringsbehovService(avklaringsbehovRepository, avbrytRevurderingService),
            tidligereVurderinger,
            rettighetsperiodeRepository,
            erProd = false
        )
    }

    @ParameterizedTest
    @EnumSource(
        Vurderingsbehov::class,
        names = ["VURDER_RETTIGHETSPERIODE", "HELHETLIG_VURDERING"]
    )
    fun `oppretter avklaringsbehov og oppdaterer vilkårsresultat når vurderingsbehov er VURDER_RETTIGHETSPERIODE og HELHETLIG_VURDERING`(
        vurderingsbehov: Vurderingsbehov
    ) {
        val behandling = behandling(TypeBehandling.Førstegangsbehandling)
        val flytKontekst = flytKontekstMedPerioder(behandling, vurderingsbehov = vurderingsbehov)
        val avklaringsbehovene = Avklaringsbehovene(InMemoryAvklaringsbehovRepository, behandling.id)

        every { avklaringsbehovRepository.hentAvklaringsbehovene(behandling.id) } returns avklaringsbehovene
        every { rettighetsperiodeRepository.hentVurdering(behandling.id) } returns mockk(relaxed = true)

        val resultat = steg.utfør(flytKontekst)

        val avklaringsbehov = avklaringsbehovene.hentBehovForDefinisjon(listOf(Definisjon.VURDER_RETTIGHETSPERIODE))
            .firstOrNull { it.erÅpent() }

        assertThat(avklaringsbehov).isNotNull
        assertThat(resultat).isEqualTo(Fullført)

        verify { vilkårsresultatRepository.lagre(behandling.id, any()) }
    }

    @Test
    fun `oppretter ikke avklaringsbehov når vurdering mangler og vurderingsbehov er HELHETLIG_VURDERING`() {
        val behandling = behandling(TypeBehandling.Førstegangsbehandling)
        val flytKontekst = flytKontekstMedPerioder(behandling, vurderingsbehov = Vurderingsbehov.HELHETLIG_VURDERING)
        val avklaringsbehovene = Avklaringsbehovene(InMemoryAvklaringsbehovRepository, behandling.id)

        every { avklaringsbehovRepository.hentAvklaringsbehovene(behandling.id) } returns avklaringsbehovene

        val resultat = steg.utfør(flytKontekst)

        val avklaringsbehov = avklaringsbehovene.hentBehovForDefinisjon(listOf(Definisjon.VURDER_RETTIGHETSPERIODE))
            .firstOrNull { it.erÅpent() }

        assertThat(avklaringsbehov).isNull()
        assertThat(resultat).isEqualTo(Fullført)

        verify(exactly = 0) { vilkårsresultatRepository.lagre(behandling.id, any()) }
    }

    @Test
    fun `oppretter ikke avklaringsbehov når vurdering finnes og vurdert tidligere i behandlingen`() {
        val behandling = behandling(TypeBehandling.Førstegangsbehandling)
        val flytKontekst =
            flytKontekstMedPerioder(behandling, vurderingsbehov = Vurderingsbehov.VURDER_RETTIGHETSPERIODE)
        val avklaringsbehovene = Avklaringsbehovene(InMemoryAvklaringsbehovRepository, behandling.id)

        leggTilLøstOgAvsluttetAvklaringsbehov(avklaringsbehovene)

        every { avklaringsbehovRepository.hentAvklaringsbehovene(behandling.id) } returns avklaringsbehovene
        every { rettighetsperiodeRepository.hentVurdering(behandling.id) } returns mockk(relaxed = true)

        val resultat = steg.utfør(flytKontekst)

        val avklaringsbehov = avklaringsbehovene.hentBehovForDefinisjon(listOf(Definisjon.VURDER_RETTIGHETSPERIODE))
            .firstOrNull { it.erÅpent() }

        assertThat(avklaringsbehov).isNull()
        assertThat(resultat).isEqualTo(Fullført)
    }

    @Test
    fun `ingen avklaringsbehov hvis tidligere vurdering tilsier ingen rett til AAP og vilkårsresultat forblir uforandret`() {
        val behandling = behandling(TypeBehandling.Førstegangsbehandling)
        val flytKontekst =
            flytKontekstMedPerioder(behandling, vurderingsbehov = Vurderingsbehov.VURDER_RETTIGHETSPERIODE)
        val avklaringsbehovene = Avklaringsbehovene(InMemoryAvklaringsbehovRepository, behandling.id)

        every { tidligereVurderinger.muligMedRettTilAAP(any(), StegType.VURDER_RETTIGHETSPERIODE) } returns false
        every { avklaringsbehovRepository.hentAvklaringsbehovene(behandling.id) } returns avklaringsbehovene

        val resultat = steg.utfør(flytKontekst)

        val avklaringsbehov = avklaringsbehovene.hentBehovForDefinisjon(listOf(Definisjon.VURDER_RETTIGHETSPERIODE))
            .firstOrNull { it.erÅpent() }

        assertThat(avklaringsbehov).isNull()
        assertThat(resultat).isEqualTo(Fullført)

        verify(exactly = 0) { vilkårsresultatRepository.lagre(behandling.id, any()) }
    }

    @ParameterizedTest
    @EnumSource(VurderingType::class, mode = Mode.EXCLUDE, names = ["FØRSTEGANGSBEHANDLING", "REVURDERING"])
    fun `oppretter ikke avklaringsbehov for vurderingstyper som ikke er FØRSTEGANGSBEHANDLING eller REVURDERING`(
        vurderingType: VurderingType
    ) {
        val behandling = behandling(TypeBehandling.Førstegangsbehandling)
        val flytKontekst = flytKontekstMedPerioder(
            behandling,
            vurderingType = vurderingType,
            vurderingsbehov = Vurderingsbehov.VURDER_RETTIGHETSPERIODE
        )
        val avklaringsbehovene = Avklaringsbehovene(InMemoryAvklaringsbehovRepository, behandling.id)

        every { avklaringsbehovRepository.hentAvklaringsbehovene(behandling.id) } returns avklaringsbehovene

        val resultat = steg.utfør(flytKontekst)

        val avklaringsbehov = avklaringsbehovene.hentBehovForDefinisjon(listOf(Definisjon.VURDER_RETTIGHETSPERIODE))
            .firstOrNull { it.erÅpent() }

        assertThat(avklaringsbehov).isNull()
        assertThat(resultat).isEqualTo(Fullført)

        verify(exactly = 0) { vilkårsresultatRepository.lagre(behandling.id, any()) }
    }

    @Test
    fun `tilbakestiller vurdering til vurderingen som lå i forrige behandling hvis det ikke lenger er behov for vurdering`() {
        val behandling = behandling(TypeBehandling.Revurdering)
        val forrigeBehandling = behandling(TypeBehandling.Førstegangsbehandling)

        // Behandlingen har kun et vurderingsbehov som ikke er relevant for dette steget
        val flytKontekst = flytKontekstMedPerioder(
            behandling,
            forrigeBehandlingId = forrigeBehandling.id,
            vurderingsbehov = Vurderingsbehov.SØKNAD_TRUKKET
        )
        val avklaringsbehovene = Avklaringsbehovene(InMemoryAvklaringsbehovRepository, behandling.id)

        // Dette er nok litt søkt scenario, men legger til et tidligere avklaringsbehov med status OPPRETTET
        avklaringsbehovene.leggTil(listOf(Definisjon.VURDER_RETTIGHETSPERIODE), StegType.VURDER_RETTIGHETSPERIODE)

        every { avklaringsbehovRepository.hentAvklaringsbehovene(behandling.id) } returns avklaringsbehovene
        every { rettighetsperiodeRepository.hentVurdering(forrigeBehandling.id) } returns null
        every { rettighetsperiodeRepository.hentVurdering(behandling.id) } returns mockk(relaxed = true)

        steg.utfør(flytKontekst)

        verify { rettighetsperiodeRepository.lagreVurdering(behandling.id, null) }
    }

    private fun leggTilLøstOgAvsluttetAvklaringsbehov(avklaringsbehovene: Avklaringsbehovene) {
        avklaringsbehovene.leggTil(listOf(Definisjon.VURDER_RETTIGHETSPERIODE), StegType.VURDER_RETTIGHETSPERIODE)
        avklaringsbehovene.løsAvklaringsbehov(Definisjon.VURDER_RETTIGHETSPERIODE, "begrunnelse", "saksbehandler")
        avklaringsbehovene.avslutt(Definisjon.VURDER_RETTIGHETSPERIODE)
    }

    private fun flytKontekstMedPerioder(
        behandling: Behandling,
        forrigeBehandlingId: BehandlingId? = null,
        vurderingType: VurderingType? = null,
        vurderingsbehov: Vurderingsbehov = Vurderingsbehov.VURDER_RETTIGHETSPERIODE
    ): FlytKontekstMedPerioder = FlytKontekstMedPerioder(
        behandling.sakId, behandling.id, forrigeBehandlingId, behandling.typeBehandling(),
        vurderingType = vurderingType
            ?: when (behandling.typeBehandling()) {
                TypeBehandling.Førstegangsbehandling -> VurderingType.FØRSTEGANGSBEHANDLING
                TypeBehandling.Revurdering -> VurderingType.REVURDERING
                else -> VurderingType.IKKE_RELEVANT
            },
        vurderingsbehovRelevanteForSteg = setOf(vurderingsbehov),
        rettighetsperiode = Periode(1 januar 2025, 1 januar 2026)
    )

    private fun behandling(
        typeBehandling: TypeBehandling,
        årsak: ÅrsakTilOpprettelse = ÅrsakTilOpprettelse.SØKNAD,
        vurderingsbehov: Vurderingsbehov = Vurderingsbehov.VURDER_RETTIGHETSPERIODE
    ): Behandling {
        val person = person()
        val sak = sak(person)

        return behandlingRepository.opprettBehandling(
            sakId = sak.id,
            typeBehandling = typeBehandling,
            forrigeBehandlingId = null,
            vurderingsbehovOgÅrsak = VurderingsbehovOgÅrsak(
                vurderingsbehov = listOf(VurderingsbehovMedPeriode(vurderingsbehov)),
                årsak = årsak
            )
        )
    }

    private fun sak(person: Person): Sak =
        sakRepository.finnEllerOpprett(person, Periode(LocalDate.now(), LocalDate.now().plusYears(1)))

    private fun person(): Person =
        Person(
            id = PersonId(Random(1235123).nextLong()),
            identifikator = UUID.randomUUID(),
            identer = listOf(genererIdent(LocalDate.now().minusYears(23)))
        )
}
