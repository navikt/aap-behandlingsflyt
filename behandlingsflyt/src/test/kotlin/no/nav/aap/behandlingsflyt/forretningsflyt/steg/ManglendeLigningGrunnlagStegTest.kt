package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.aap.behandlingsflyt.behandling.avbrytrevurdering.AvbrytRevurderingService
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovService
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.behandlingsflyt.behandling.beregning.BeregningService
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.InntektGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.InntektGrunnlagRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.InntektPerÅr
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.ManuellInntektGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.ManuellInntektGrunnlagRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.ManuellInntektVurdering
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Status
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovOgÅrsak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.test.FakeUnleash
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryAvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryBehandlingRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemorySakRepository
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.behandlingsflyt.test.modell.genererIdent
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Beløp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.EnumSource.Mode
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Year
import java.util.*

class ManglendeLigningGrunnlagStegTest {
    private lateinit var avklaringsbehovRepository: AvklaringsbehovRepository
    private lateinit var inntektGrunnlagRepository: InntektGrunnlagRepository
    private lateinit var manuellInntektGrunnlagRepository: ManuellInntektGrunnlagRepository
    private lateinit var tidligereVurderinger: TidligereVurderinger
    private lateinit var beregningService: BeregningService
    private lateinit var steg: ManglendeLigningGrunnlagSteg
    private val sakRepository = InMemorySakRepository
    private val behandlingRepository = InMemoryBehandlingRepository
    private lateinit var avbrytRevurderingService: AvbrytRevurderingService
    private lateinit var avklaringsbehovService: AvklaringsbehovService

    private val sisteÅr = Year.of(2025)

    @BeforeEach
    fun setup() {
        avklaringsbehovRepository = mockk()
        beregningService = mockk {
            every { utledRelevanteBeregningsÅr(any()) } returns setOf(
                sisteÅr.minusYears(2), sisteÅr.minusYears(1), sisteÅr
            )
        }
        inntektGrunnlagRepository = mockk {
            every { hentHvisEksisterer(any()) } returns InntektGrunnlag(
                setOf(
                    InntektPerÅr(sisteÅr.minusYears(2), Beløp(250_000)),
                    InntektPerÅr(sisteÅr.minusYears(1), Beløp(275_000)),
                    InntektPerÅr(sisteÅr, Beløp(300_000))
                ), emptySet()
            )
        }
        manuellInntektGrunnlagRepository = mockk(relaxed = true) {
            every { hentHvisEksisterer(any()) } returns null
        }
        tidligereVurderinger = mockk {
            every { girAvslagEllerIngenBehandlingsgrunnlag(any(), StegType.MANGLENDE_LIGNING) } returns false
        }

        avklaringsbehovRepository = mockk()

        avbrytRevurderingService = mockk {
            every { revurderingErAvbrutt(any()) } returns false
        }

        avklaringsbehovService = AvklaringsbehovService(
            avklaringsbehovRepository, avbrytRevurderingService
        )

        steg = ManglendeLigningGrunnlagSteg(
            avklaringsbehovRepository,
            inntektGrunnlagRepository,
            manuellInntektGrunnlagRepository,
            tidligereVurderinger,
            beregningService,
            avklaringsbehovService,
            FakeUnleash
        )
    }

    @Test
    fun `oppretter ikke avklaringsbehov når det finnes inntekt for siste relevante år`() {
        val behandling = behandling(typeBehandling = TypeBehandling.Førstegangsbehandling)
        val flytKontekst = flytKontekstMedPerioder(behandling)
        val avklaringsbehovene = Avklaringsbehovene(InMemoryAvklaringsbehovRepository, behandling.id)

        every { avklaringsbehovRepository.hentAvklaringsbehovene(behandling.id) } returns avklaringsbehovene

        val resultat = steg.utfør(flytKontekst)

        val avklaringsbehov = avklaringsbehovene.hentBehovForDefinisjon(listOf(Definisjon.FASTSETT_MANUELL_INNTEKT))
            .firstOrNull { it.erÅpent() }

        assertThat(avklaringsbehov).isNull()
        assertThat(resultat).isEqualTo(Fullført)
    }

    @Test
    fun `oppretter avklaringsbehov når det mangler inntekt for siste relevante år og tidligere vurdering tilsier mulig rett til AAP`() {
        val behandling = behandling(typeBehandling = TypeBehandling.Førstegangsbehandling)
        val flytKontekst = flytKontekstMedPerioder(behandling)
        val avklaringsbehovene = Avklaringsbehovene(InMemoryAvklaringsbehovRepository, behandling.id)

        every { avklaringsbehovRepository.hentAvklaringsbehovene(behandling.id) } returns avklaringsbehovene
        every { inntektGrunnlagRepository.hentHvisEksisterer(behandling.id) } returns InntektGrunnlag(
            setOf(
                InntektPerÅr(sisteÅr.minusYears(2), Beløp(250_000)),
                InntektPerÅr(sisteÅr.minusYears(1), Beløp(275_000)),
            ), emptySet()
        )

        val resultat = steg.utfør(flytKontekst)

        val avklaringsbehov = avklaringsbehovene.hentBehovForDefinisjon(listOf(Definisjon.FASTSETT_MANUELL_INNTEKT))
            .firstOrNull { it.erÅpent() }

        assertThat(avklaringsbehov).isNotNull
        assertThat(resultat).isEqualTo(Fullført)
    }

    @Test
    fun `oppretter ikke avklaringsbehov når det mangler inntekt men dette ikke er 3 siste relevante år og tidligere vurdering tilsier mulig rett til AAP`() {
        val behandling = behandling(typeBehandling = TypeBehandling.Førstegangsbehandling)
        val flytKontekst = flytKontekstMedPerioder(behandling)
        val avklaringsbehovene = Avklaringsbehovene(InMemoryAvklaringsbehovRepository, behandling.id)

        every { avklaringsbehovRepository.hentAvklaringsbehovene(behandling.id) } returns avklaringsbehovene
        every { inntektGrunnlagRepository.hentHvisEksisterer(behandling.id) } returns InntektGrunnlag(
            setOf(
                InntektPerÅr(sisteÅr.minusYears(1), Beløp(250_000)),
                InntektPerÅr(sisteÅr.minusYears(2), Beløp(250_000)),
                InntektPerÅr(sisteÅr.minusYears(5), Beløp(250_000)),
                InntektPerÅr(sisteÅr, Beløp(300_000)),
            ), emptySet()
        )

        val resultat = steg.utfør(flytKontekst)

        val avklaringsbehov = avklaringsbehovene.hentBehovForDefinisjon(listOf(Definisjon.FASTSETT_MANUELL_INNTEKT))
            .firstOrNull { it.erÅpent() }

        assertThat(avklaringsbehov).isNull()
        assertThat(resultat).isEqualTo(Fullført)
    }

    @Test
    fun `oppretter ikke avklaringsbehov når det finnes manuell inntekt for 3 siste relevante år`() {
        val behandling = behandling(typeBehandling = TypeBehandling.Førstegangsbehandling)
        val flytKontekst = flytKontekstMedPerioder(behandling)
        val avklaringsbehovene = Avklaringsbehovene(InMemoryAvklaringsbehovRepository, behandling.id)

        // Legger til tidligere avklaringsbehov som er løst og avsluttet for å sjekke at det ikke opprettes nytt behov
        leggTilLøstOgAvsluttetAvklaringsbehov(avklaringsbehovene)

        every { avklaringsbehovRepository.hentAvklaringsbehovene(behandling.id) } returns avklaringsbehovene
        every { inntektGrunnlagRepository.hentHvisEksisterer(behandling.id) } returns InntektGrunnlag(
            emptySet(), emptySet()
        )
        every { manuellInntektGrunnlagRepository.hentHvisEksisterer(behandling.id) } returns ManuellInntektGrunnlag(
            manuelleVurderinger().toSet()
        )

        val resultat = steg.utfør(flytKontekst)

        val avklaringsbehov = avklaringsbehovene.hentBehovForDefinisjon(listOf(Definisjon.FASTSETT_MANUELL_INNTEKT))
            .firstOrNull { it.erÅpent() }

        assertThat(avklaringsbehov).isNull()
        assertThat(resultat).isEqualTo(Fullført)
    }

    @Test
    fun `tilbakestiller manuell inntekt og avbryter avklaringsbehov hvis ingen behov for vurdering og manuell inntekt finnes for behandling`() {
        val behandling = behandling(typeBehandling = TypeBehandling.Førstegangsbehandling)
        val flytKontekst = flytKontekstMedPerioder(behandling)
        val avklaringsbehovene = Avklaringsbehovene(InMemoryAvklaringsbehovRepository, behandling.id)

        leggTilLøstOgAvsluttetAvklaringsbehov(avklaringsbehovene)

        every { avklaringsbehovRepository.hentAvklaringsbehovene(behandling.id) } returns avklaringsbehovene
        every { manuellInntektGrunnlagRepository.hentHvisEksisterer(behandling.id) } returns ManuellInntektGrunnlag(
            manuelleVurderinger().toSet()
        )

        val resultat = steg.utfør(flytKontekst)

        val avklaringsbehov =
            avklaringsbehovene.hentBehovForDefinisjon(listOf(Definisjon.FASTSETT_MANUELL_INNTEKT)).firstOrNull()

        assertThat(avklaringsbehov?.status()).isEqualTo(Status.AVBRUTT)
        assertThat(resultat).isEqualTo(Fullført)

        // Tilbakestiller manuell inntekt
        verify { manuellInntektGrunnlagRepository.lagre(behandling.id, any<Set<ManuellInntektVurdering>>()) }
    }

    @Test
    fun `ingen avklaringsbehov hvis tidligere vurdering tilsier ingen rett til AAP`() {
        val behandling = behandling(typeBehandling = TypeBehandling.Førstegangsbehandling)
        val flytKontekst = flytKontekstMedPerioder(behandling)
        val avklaringsbehovene = Avklaringsbehovene(InMemoryAvklaringsbehovRepository, behandling.id)

        // Vil i utgangspunktet opprette avklaringsbehov dersom ingen inntekter finnes
        every { inntektGrunnlagRepository.hentHvisEksisterer(behandling.id) } returns InntektGrunnlag(
            emptySet(), emptySet()
        )
        every {
            tidligereVurderinger.girAvslagEllerIngenBehandlingsgrunnlag(
                any(), StegType.MANGLENDE_LIGNING
            )
        } returns true
        every { avklaringsbehovRepository.hentAvklaringsbehovene(behandling.id) } returns avklaringsbehovene

        val resultat = steg.utfør(flytKontekst)

        val avklaringsbehov = avklaringsbehovene.hentBehovForDefinisjon(listOf(Definisjon.FASTSETT_MANUELL_INNTEKT))
            .firstOrNull { it.erÅpent() }

        assertThat(avklaringsbehov).isNull()
        assertThat(resultat).isEqualTo(Fullført)
    }

    @ParameterizedTest
    @EnumSource(VurderingType::class, mode = Mode.EXCLUDE, names = ["FØRSTEGANGSBEHANDLING", "REVURDERING"])
    fun `oppretter ikke avklaringsbehov for vurderingstyper som ikke er FØRSTEGANGSBEHANDLING eller REVURDERING`(
        vurderingType: VurderingType
    ) {
        val behandling = behandling(typeBehandling = TypeBehandling.Førstegangsbehandling)
        val flytKontekst = flytKontekstMedPerioder(behandling, vurderingType = vurderingType)
        val avklaringsbehovene = Avklaringsbehovene(InMemoryAvklaringsbehovRepository, behandling.id)

        // Vil i utgangspunktet opprette avklaringsbehov dersom ingen inntekter finnes
        every { inntektGrunnlagRepository.hentHvisEksisterer(behandling.id) } returns InntektGrunnlag(
            emptySet(), emptySet()
        )
        every { avklaringsbehovRepository.hentAvklaringsbehovene(behandling.id) } returns avklaringsbehovene

        val resultat = steg.utfør(flytKontekst)

        val avklaringsbehov = avklaringsbehovene.hentBehovForDefinisjon(listOf(Definisjon.FASTSETT_MANUELL_INNTEKT))
            .firstOrNull { it.erÅpent() }

        assertThat(avklaringsbehov).isNull()
        assertThat(resultat).isEqualTo(Fullført)
    }

    @Test
    fun `oppretter avklaringsbehov ved manuell revurdering og tidligere vurdering tilsier mulig rett til AAP`() {
        val behandling = behandling(
            typeBehandling = TypeBehandling.Revurdering,
            årsak = ÅrsakTilOpprettelse.MANUELL_OPPRETTELSE,
            vurderingsbehov = Vurderingsbehov.BARNETILLEGG
        )

        val flytKontekst = flytKontekstMedPerioder(behandling)
        val avklaringsbehovene = Avklaringsbehovene(InMemoryAvklaringsbehovRepository, behandling.id)

        every { avklaringsbehovRepository.hentAvklaringsbehovene(behandling.id) } returns avklaringsbehovene
        every { inntektGrunnlagRepository.hentHvisEksisterer(behandling.id) } returns InntektGrunnlag(
            setOf(
                InntektPerÅr(sisteÅr.minusYears(2), Beløp(250_000)),
                InntektPerÅr(sisteÅr.minusYears(1), Beløp(275_000)),
            ), emptySet()
        )

        val resultat = steg.utfør(flytKontekst)

        val avklaringsbehov = avklaringsbehovene.hentBehovForDefinisjon(listOf(Definisjon.FASTSETT_MANUELL_INNTEKT))
            .firstOrNull { it.erÅpent() }

        assertThat(avklaringsbehov).isNotNull
        assertThat(resultat).isEqualTo(Fullført)
    }

    private fun leggTilLøstOgAvsluttetAvklaringsbehov(avklaringsbehovene: Avklaringsbehovene) {
        avklaringsbehovene.leggTil(listOf(Definisjon.FASTSETT_MANUELL_INNTEKT), StegType.MANGLENDE_LIGNING, null, null)
        avklaringsbehovene.løsAvklaringsbehov(Definisjon.FASTSETT_MANUELL_INNTEKT, "begrunnelse", "saksbehandler")
        avklaringsbehovene.avslutt(Definisjon.FASTSETT_MANUELL_INNTEKT)
    }

    private fun manuelleVurderinger(): Set<ManuellInntektVurdering> = setOf(
        ManuellInntektVurdering(
            år = sisteÅr,
            begrunnelse = "begrunnelse",
            belop = Beløp(350_000),
            vurdertAv = "saksbehandler",
            opprettet = LocalDateTime.now()
        ), ManuellInntektVurdering(
            år = sisteÅr.minusYears(1),
            begrunnelse = "begrunnelse",
            belop = Beløp(350_000),
            vurdertAv = "saksbehandler",
            opprettet = LocalDateTime.now()
        ), ManuellInntektVurdering(
            år = sisteÅr.minusYears(2),
            begrunnelse = "begrunnelse",
            belop = Beløp(350_000),
            vurdertAv = "saksbehandler",
            opprettet = LocalDateTime.now()
        )
    )

    private fun flytKontekstMedPerioder(
        behandling: Behandling, vurderingType: VurderingType? = null
    ): FlytKontekstMedPerioder = FlytKontekstMedPerioder(
        behandling.sakId,
        behandling.id,
        behandling.forrigeBehandlingId,
        behandling.typeBehandling(),
        vurderingType = vurderingType ?: when (behandling.typeBehandling()) {
            TypeBehandling.Førstegangsbehandling -> VurderingType.FØRSTEGANGSBEHANDLING
            TypeBehandling.Revurdering -> VurderingType.REVURDERING
            else -> VurderingType.IKKE_RELEVANT
        },
        vurderingsbehovRelevanteForSteg = behandling.vurderingsbehov().map { it.type }.toSet(),
        rettighetsperiode = Periode(1 januar 2025, 1 januar 2026)
    )

    private fun behandling(
        typeBehandling: TypeBehandling,
        årsak: ÅrsakTilOpprettelse = ÅrsakTilOpprettelse.SØKNAD,
        vurderingsbehov: Vurderingsbehov = Vurderingsbehov.MOTTATT_SØKNAD
    ): Behandling {
        val person = person()
        val sak = sak(person)

        return behandlingRepository.opprettBehandling(
            sakId = sak.id,
            typeBehandling = typeBehandling,
            forrigeBehandlingId = null,
            vurderingsbehovOgÅrsak = VurderingsbehovOgÅrsak(
                vurderingsbehov = listOf(VurderingsbehovMedPeriode(vurderingsbehov)), årsak = årsak
            )
        )
    }

    private fun sak(person: Person): Sak =
        sakRepository.finnEllerOpprett(person, Periode(LocalDate.now(), LocalDate.now().plusYears(1)))

    private fun person(): Person = Person(
        PersonId(Random(1235123).nextLong()), UUID.randomUUID(), listOf(genererIdent(LocalDate.now().minusYears(23)))
    )
}
