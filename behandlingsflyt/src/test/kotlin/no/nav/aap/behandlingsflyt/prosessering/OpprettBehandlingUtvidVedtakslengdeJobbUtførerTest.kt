package no.nav.aap.behandlingsflyt.prosessering

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import no.nav.aap.behandlingsflyt.behandling.vedtakslengde.VedtakslengdeService
import no.nav.aap.behandlingsflyt.faktagrunnlag.SakOgBehandlingService
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.Underveisperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Avslagsårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkår
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsresultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.vedtakslengde.VedtakslengdeRepository
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingMedVedtak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.test.desember
import no.nav.aap.behandlingsflyt.test.fixedClock
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Tid
import no.nav.aap.motor.JobbInput
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID.randomUUID

class OpprettBehandlingUtvidVedtakslengdeJobbUtførerTest {

    private val dagensDato = 1 desember 2025
    private val clock = fixedClock(dagensDato)
    private val sakId = SakId(1L)
    private val behandlingId = BehandlingId(1L)
    private val jobbInput = JobbInput(OpprettBehandlingUtvidVedtakslengdeJobbUtfører).forSak(sakId.id)

    private val prosesserBehandlingService = mockk<ProsesserBehandlingService>()
    private val vedtakslengdeRepository = mockk<VedtakslengdeRepository>()
    private val underveisRepository = mockk<UnderveisRepository>()
    private val sakOgBehandlingService = mockk<SakOgBehandlingService>()
    private val vilkårsresultatRepository = mockk<VilkårsresultatRepository>()
    private val opprettBehandlingUtvidVedtakslengdeJobbUtfører =
        OpprettBehandlingUtvidVedtakslengdeJobbUtfører(
            prosesserBehandlingService = prosesserBehandlingService,
            sakOgBehandlingService = sakOgBehandlingService,
            vedtakslengdeService = VedtakslengdeService(
                vedtakslengdeRepository = vedtakslengdeRepository,
                underveisRepository = underveisRepository,
                vilkårsresultatRepository = vilkårsresultatRepository,
                clock = clock
            ),
            clock = clock
        )


    @Test
    fun `skal opprette og sette i gang prosessering av behandling hvis sluttdato er innenfor dagens dato + 28 dager`() {
        val sak = sak()
        val behandling = behandlingMedVedtak()

        every { underveisRepository.hentHvisEksisterer(behandling.id)} returns underveisGrunnlag()
        every { sakOgBehandlingService.finnBehandlingMedSisteFattedeVedtak(sakId) } returns behandlingMedVedtak()
        every { sakOgBehandlingService.finnEllerOpprettBehandling(sak.id, any()) } returns opprettetBehandling()
        every { prosesserBehandlingService.triggProsesserBehandling(any<SakOgBehandlingService.OpprettetBehandling>()) } just Runs
        every { vilkårsresultatRepository.hent(behandlingId) } returns genererVilkårsresultat(sak.rettighetsperiode)
        every { vedtakslengdeRepository.hentHvisEksisterer(behandling.id) } returns null

        opprettBehandlingUtvidVedtakslengdeJobbUtfører.utfør(jobbInput)

        verify(exactly = 1) { prosesserBehandlingService.triggProsesserBehandling(any<SakOgBehandlingService.OpprettetBehandling>()) }
    }

    @Test
    fun `skal ikke opprette og sette i gang prosessering av behandling hvis sluttdato er lenger frem enn dagens dato + 28 dager`() {
        val behandling = behandlingMedVedtak()

        every { underveisRepository.hentHvisEksisterer(behandling.id)} returns underveisGrunnlag()
        every { sakOgBehandlingService.finnBehandlingMedSisteFattedeVedtak(sakId) } returns null

        opprettBehandlingUtvidVedtakslengdeJobbUtfører.utfør(jobbInput)

        verify(exactly = 0) { prosesserBehandlingService.triggProsesserBehandling(any<SakOgBehandlingService.OpprettetBehandling>()) }
    }

    @Test
    fun `skal ikke opprette og sette i gang prosessering av behandling hvis hvis det ikke finnes noen siste gjeldende behandling`() {
        val sak = sak()
        val behandling = behandlingMedVedtak()

        every { underveisRepository.hentHvisEksisterer(behandling.id)} returns underveisGrunnlag(perioder = underveisPerioderIkkeUtløpt())
        every { sakOgBehandlingService.finnBehandlingMedSisteFattedeVedtak(sakId) } returns null
        every { sakOgBehandlingService.finnEllerOpprettBehandling(sak.id, any()) } returns opprettetBehandling()
        every { vilkårsresultatRepository.hent(behandlingId) } returns genererVilkårsresultat(sak.rettighetsperiode)

        opprettBehandlingUtvidVedtakslengdeJobbUtfører.utfør(jobbInput)

        verify(exactly = 0) { prosesserBehandlingService.triggProsesserBehandling(any<SakOgBehandlingService.OpprettetBehandling>()) }
    }

    @Test
    fun `skal ikke opprette og sette i gang prosessering av behandling bistandsvilkåret ikke er oppfylt frem i tid`() {
        val sak = sak()
        val behandling = behandlingMedVedtak()

        every { underveisRepository.hentHvisEksisterer(behandling.id)} returns underveisGrunnlag()
        every { sakOgBehandlingService.finnBehandlingMedSisteFattedeVedtak(sakId) } returns behandlingMedVedtak()
        every { sakOgBehandlingService.finnEllerOpprettBehandling(sak.id, any()) } returns opprettetBehandling()
        every { prosesserBehandlingService.triggProsesserBehandling(any<SakOgBehandlingService.OpprettetBehandling>()) } just Runs
        every { vilkårsresultatRepository.hent(behandlingId) } returns genererVilkårsresultat(sak.rettighetsperiode, oppfyltBistand = false)
        every { vedtakslengdeRepository.hentHvisEksisterer(behandling.id) } returns null

        opprettBehandlingUtvidVedtakslengdeJobbUtfører.utfør(jobbInput)

        verify(exactly = 0) { prosesserBehandlingService.triggProsesserBehandling(any<SakOgBehandlingService.OpprettetBehandling>()) }
    }

    private fun behandling(status: Status = Status.IVERKSETTES) =
        Behandling(
            sakId = sakId,
            id = behandlingId,
            referanse = BehandlingReferanse(),
            typeBehandling = TypeBehandling.Førstegangsbehandling,
            status = status,
            opprettetTidspunkt = LocalDateTime.now(clock),
            årsakTilOpprettelse = ÅrsakTilOpprettelse.SØKNAD,
            forrigeBehandlingId = null,
            versjon = 1L
        )

    private fun behandlingMedVedtak(): BehandlingMedVedtak =
        BehandlingMedVedtak(
            saksnummer = Saksnummer("123"),
            id = behandlingId,
            referanse = BehandlingReferanse(),
            typeBehandling = TypeBehandling.Førstegangsbehandling,
            status = Status.IVERKSETTES,
            opprettetTidspunkt = LocalDateTime.now(clock),
            vedtakstidspunkt = LocalDateTime.now(clock),
            virkningstidspunkt = null,
            vurderingsbehov = setOf(),
            årsakTilOpprettelse = ÅrsakTilOpprettelse.SØKNAD,
            forrigeBehandlingId = null,
        )

    private fun sak(rettighetsperiode: Periode = Periode(LocalDate.now(clock).minusDays(180), Tid.MAKS)) =
        Sak(
            id = sakId,
            saksnummer = Saksnummer("123"),
            person = Person(randomUUID(), emptyList()),
            rettighetsperiode = rettighetsperiode,
        )

    private fun underveisGrunnlag(perioder: List<Underveisperiode> = underveisPerioderUtløpt()) =
        UnderveisGrunnlag(
            id = 1L,
            perioder = perioder
        )

    private fun underveisPerioderUtløpt() =
        listOf(
            mockk<Underveisperiode> {
                every { periode } returns Periode(
                    fom = dagensDato,
                    tom = dagensDato.plusDays(10)
                )
            },
            mockk<Underveisperiode> {
                every { periode } returns Periode(
                    fom = dagensDato.minusYears(1),
                    tom = dagensDato.minusDays(1)
                )
            },
        )

    private fun underveisPerioderIkkeUtløpt() =
        listOf(
            mockk<Underveisperiode> {
                every { periode } returns Periode(
                    fom = dagensDato,
                    tom = dagensDato.plusDays(28)
                )
            },
            mockk<Underveisperiode> {
                every { periode } returns Periode(
                    fom = dagensDato.minusYears(1),
                    tom = dagensDato.minusDays(1)
                )
            },
        )

    private fun opprettetBehandling() =
        SakOgBehandlingService.MåBehandlesAtomært(
            nyBehandling = behandling(),
            åpenBehandling = null
        )

    private fun genererVilkårsresultat(periode: Periode, oppfyltBistand: Boolean = true): Vilkårsresultat {
        val aldersVilkåret =
            Vilkår(
                Vilkårtype.ALDERSVILKÅRET, setOf(
                    Vilkårsperiode(
                        periode,
                        Utfall.OPPFYLT,
                        false,
                        null,
                        faktagrunnlag = null
                    )
                )
            )
        val sykdomsVilkåret =
            Vilkår(
                Vilkårtype.SYKDOMSVILKÅRET, setOf(
                    Vilkårsperiode(
                        periode,
                        Utfall.OPPFYLT,
                        false,
                        null,
                        faktagrunnlag = null
                    )
                )
            )
        val lovvalgsVilkåret =
            Vilkår(
                Vilkårtype.LOVVALG, setOf(
                    Vilkårsperiode(
                        periode,
                        Utfall.OPPFYLT,
                        false,
                        null,
                        faktagrunnlag = null
                    )
                )
            )
        val medlemskapVilkåret =
            Vilkår(
                Vilkårtype.MEDLEMSKAP, setOf(
                    Vilkårsperiode(
                        periode,
                        Utfall.OPPFYLT,
                        false,
                        null,
                        faktagrunnlag = null
                    )
                )
            )
        val bistandVilkåret =
            Vilkår(
                Vilkårtype.BISTANDSVILKÅRET, setOf(
                    Vilkårsperiode(
                        periode,
                        if (oppfyltBistand) Utfall.OPPFYLT else Utfall.IKKE_OPPFYLT,
                        false,
                        null,
                        faktagrunnlag = null,
                        avslagsårsak = if (oppfyltBistand) null else Avslagsårsak.IKKE_BEHOV_FOR_OPPFOLGING
                    )
                )
            )
        val grunnlagVilkåret = Vilkår(
            Vilkårtype.GRUNNLAGET, setOf(
                Vilkårsperiode(
                    periode,
                    Utfall.OPPFYLT,
                    false,
                    null,
                    faktagrunnlag = null
                )
            )
        )

        return Vilkårsresultat(
            vilkår = listOf(
                aldersVilkåret,
                lovvalgsVilkåret,
                sykdomsVilkåret,
                medlemskapVilkåret,
                bistandVilkåret,
                grunnlagVilkåret,
            )
        )
    }
}