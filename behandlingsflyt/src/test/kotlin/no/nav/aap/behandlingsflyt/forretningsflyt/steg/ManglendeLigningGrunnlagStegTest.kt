package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
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
    private lateinit var avklaringsbehovene: Avklaringsbehovene
    private val sakRepository = InMemorySakRepository
    private val behandlingRepository = InMemoryBehandlingRepository

    private val sisteÅr = Year.of(2025)
    private val person = person()
    private val sak = sak(person)
    private val behandling = behandling(sak, typeBehandling = TypeBehandling.Førstegangsbehandling)
    private val flytKontekst = flytKontekstMedPerioder()

    @BeforeEach
    fun setup() {
        avklaringsbehovene = Avklaringsbehovene(InMemoryAvklaringsbehovRepository, behandling.id)
        avklaringsbehovRepository = mockk {
            every { hentAvklaringsbehovene(behandling.id) } returns avklaringsbehovene
        }
        beregningService = mockk {
            every { utledRelevanteBeregningsÅr(behandling.id) } returns setOf(
                sisteÅr.minusYears(2),
                sisteÅr.minusYears(1),
                sisteÅr
            )
        }
        inntektGrunnlagRepository = mockk {
            every { hentHvisEksisterer(behandling.id) } returns InntektGrunnlag(setOf(
                InntektPerÅr(sisteÅr.minusYears(2), Beløp(250_000)),
                InntektPerÅr(sisteÅr.minusYears(1), Beløp(275_000)),
                InntektPerÅr(sisteÅr, Beløp(300_000)))
            )
        }
        manuellInntektGrunnlagRepository = mockk(relaxed = true) {
            every { hentHvisEksisterer(behandling.id) } returns null
        }
        tidligereVurderinger = mockk {
            every { muligMedRettTilAAP(any(), any()) } returns true
            every { girAvslagEllerIngenBehandlingsgrunnlag(any(), any()) } returns false
        }

        steg = ManglendeLigningGrunnlagSteg(
            avklaringsbehovRepository,
            inntektGrunnlagRepository,
            manuellInntektGrunnlagRepository,
            tidligereVurderinger,
            beregningService
        )
    }

    @Test
    fun `oppretter ikke avklaringsbehov når det finnes inntekt for siste relevante år`() {
        val resultat = steg.utfør(flytKontekst)

        val avklaringsbehov = avklaringsbehovene.hentBehovForDefinisjon(listOf(Definisjon.FASTSETT_MANUELL_INNTEKT))
            .firstOrNull { it.erÅpent() }

        assertThat(avklaringsbehov).isNull()
        assertThat(resultat).isEqualTo(Fullført)
    }

    @Test
    fun `oppretter avklaringsbehov når det mangler inntekt for siste relevante år og tidligere vurdering tilsier mulig rett til AAP`() {
        every { inntektGrunnlagRepository.hentHvisEksisterer(behandling.id) } returns InntektGrunnlag(setOf(
            InntektPerÅr(sisteÅr.minusYears(2), Beløp(250_000)),
            InntektPerÅr(sisteÅr.minusYears(1), Beløp(275_000)),
        ))

        val resultat = steg.utfør(flytKontekst)

        val avklaringsbehov = avklaringsbehovene.hentBehovForDefinisjon(listOf(Definisjon.FASTSETT_MANUELL_INNTEKT))
            .firstOrNull { it.erÅpent() }

        assertThat(avklaringsbehov).isNotNull
        assertThat(resultat).isEqualTo(Fullført)
    }

    @Test
    fun `oppretter ikke avklaringsbehov når det mangler inntekt men dette ikke er siste relevante år og tidligere vurdering tilsier mulig rett til AAP`() {
        // Antar at dette på sikt skal støttes, men pr nå er det kun siste relevante år som trigger avklaringsbehov
        every { inntektGrunnlagRepository.hentHvisEksisterer(behandling.id) } returns InntektGrunnlag(setOf(
            InntektPerÅr(sisteÅr.minusYears(2), Beløp(250_000)),
            InntektPerÅr(sisteÅr, Beløp(300_000)),
        ))

        val resultat = steg.utfør(flytKontekst)

        val avklaringsbehov = avklaringsbehovene.hentBehovForDefinisjon(listOf(Definisjon.FASTSETT_MANUELL_INNTEKT))
            .firstOrNull { it.erÅpent() }

        assertThat(avklaringsbehov).isNull()
        assertThat(resultat).isEqualTo(Fullført)
    }

    @Test
    fun `oppretter ikke avklaringsbehov når det finnes manuell inntekt for siste relevante år`() {
        val manuellVurdering = manuellVurdering()

        // Legger til tidligere avklaringsbehov som er løst og avsluttet for å sjekke at det ikke opprettes nytt behov
        leggTilLøstOgAvsluttetAvklaringsbehov()

        every { inntektGrunnlagRepository.hentHvisEksisterer(behandling.id) } returns InntektGrunnlag(emptySet())
        every { manuellInntektGrunnlagRepository.hentHvisEksisterer(behandling.id) } returns ManuellInntektGrunnlag(setOf(manuellVurdering))

        val resultat = steg.utfør(flytKontekst)

        val avklaringsbehov = avklaringsbehovene.hentBehovForDefinisjon(listOf(Definisjon.FASTSETT_MANUELL_INNTEKT))
            .firstOrNull { it.erÅpent() }

        assertThat(avklaringsbehov).isNull()
        assertThat(resultat).isEqualTo(Fullført)
    }

    @Test
    fun `tilbakestiller manuell inntekt og avbryter avklaringsbehov hvis ingen behov for vurdering og manuell inntekt finnes for behandling`() {
        val manuellVurdering = manuellVurdering()

        leggTilLøstOgAvsluttetAvklaringsbehov()

        every { manuellInntektGrunnlagRepository.hentHvisEksisterer(behandling.id) } returns ManuellInntektGrunnlag(setOf(manuellVurdering))

        val resultat = steg.utfør(flytKontekst)

        val avklaringsbehov = avklaringsbehovene.hentBehovForDefinisjon(listOf(Definisjon.FASTSETT_MANUELL_INNTEKT))
            .firstOrNull()

        assertThat(avklaringsbehov?.status()).isEqualTo(Status.AVBRUTT)
        assertThat(resultat).isEqualTo(Fullført)

        // Tilbakestiller manuell inntekt
        verify { manuellInntektGrunnlagRepository.lagre(behandling.id, any<Set<ManuellInntektVurdering>>()) }
    }

    @Test
    fun `ingen avklaringsbehov hvis tidligere vurdering tilsier ingen rett til AAP`() {
        // Vil i utgangspunktet opprette avklaringsbehov dersom ingen inntekter finnes
        every { inntektGrunnlagRepository.hentHvisEksisterer(behandling.id) } returns InntektGrunnlag(emptySet())
        every { tidligereVurderinger.muligMedRettTilAAP(any(), any()) } returns false

        val resultat = steg.utfør(flytKontekst)

        val avklaringsbehov = avklaringsbehovene.hentBehovForDefinisjon(listOf(Definisjon.FASTSETT_MANUELL_INNTEKT))
            .firstOrNull { it.erÅpent() }

        assertThat(avklaringsbehov).isNull()
        assertThat(resultat).isEqualTo(Fullført)
    }

    @ParameterizedTest
    @EnumSource(VurderingType::class, mode = Mode.EXCLUDE, names = ["FØRSTEGANGSBEHANDLING", "REVURDERING"])
    fun `oppretter ikke avklaringsbehov for vurderingstyper som ikke er FØRSTEGANGSBEHANDLING eller REVURDERING`(vurderingType: VurderingType) {
        // Vil i utgangspunktet opprette avklaringsbehov dersom ingen inntekter finnes
        every { inntektGrunnlagRepository.hentHvisEksisterer(behandling.id) } returns InntektGrunnlag(emptySet())

        val resultat = steg.utfør(flytKontekstMedPerioder(vurderingType = vurderingType))

        val avklaringsbehov = avklaringsbehovene.hentBehovForDefinisjon(listOf(Definisjon.FASTSETT_MANUELL_INNTEKT))
            .firstOrNull { it.erÅpent() }

        assertThat(avklaringsbehov).isNull()
        assertThat(resultat).isEqualTo(Fullført)
    }

    private fun leggTilLøstOgAvsluttetAvklaringsbehov() {
        avklaringsbehovene.leggTil(listOf(Definisjon.FASTSETT_MANUELL_INNTEKT), StegType.MANGLENDE_LIGNING)
        avklaringsbehovene.løsAvklaringsbehov(Definisjon.FASTSETT_MANUELL_INNTEKT, "begrunnelse", "saksbehandler")
        avklaringsbehovene.avslutt(Definisjon.FASTSETT_MANUELL_INNTEKT)
    }

    private fun manuellVurdering(): ManuellInntektVurdering = ManuellInntektVurdering(
        år = sisteÅr,
        begrunnelse = "begrunnelse",
        belop = Beløp(350_000),
        vurdertAv = "saksbehandler",
        opprettet = LocalDateTime.now()
    )

    private fun flytKontekstMedPerioder(vurderingType: VurderingType = VurderingType.FØRSTEGANGSBEHANDLING): FlytKontekstMedPerioder = FlytKontekstMedPerioder(
        sak.id, behandling.id, behandling.forrigeBehandlingId, behandling.typeBehandling(),
        vurderingType = vurderingType,
        vurderingsbehovRelevanteForSteg = setOf(Vurderingsbehov.MOTTATT_SØKNAD),
        rettighetsperiode = Periode(1 januar 2025, 1 januar 2026)
    )

    private fun behandling(sak: Sak, typeBehandling: TypeBehandling): Behandling =
        behandlingRepository.opprettBehandling(
            sakId = sak.id,
            typeBehandling = typeBehandling,
            forrigeBehandlingId = null,
            vurderingsbehovOgÅrsak = VurderingsbehovOgÅrsak(
                vurderingsbehov = listOf(VurderingsbehovMedPeriode(Vurderingsbehov.MOTTATT_SØKNAD)),
                årsak = ÅrsakTilOpprettelse.SØKNAD
            )
        )

    private fun sak(person: Person): Sak =
        sakRepository.finnEllerOpprett(person, Periode(LocalDate.now(), LocalDate.now().plusYears(1)))

    private fun person(): Person =
        Person(PersonId(Random(1235123).nextLong()), UUID.randomUUID(), listOf(genererIdent(LocalDate.now().minusYears(23))))


}