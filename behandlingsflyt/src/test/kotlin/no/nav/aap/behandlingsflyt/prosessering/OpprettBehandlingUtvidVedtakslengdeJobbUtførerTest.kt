package no.nav.aap.behandlingsflyt.prosessering

import io.mockk.Runs
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
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
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Tid
import no.nav.aap.motor.JobbInput
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID.randomUUID

@ExtendWith(MockKExtension::class)
class OpprettBehandlingUtvidVedtakslengdeJobbUtførerTest {

    private val sakId_1 = SakId(1L)
    private val behandlingId = BehandlingId(1L)

    val prosesserBehandlingService = mockk<ProsesserBehandlingService>()
    val sakRepository = mockk<SakRepository>()
    val underveisRepository = mockk<UnderveisRepository>()
    val sakOgBehandlingService = mockk<SakOgBehandlingService>()
    val vilkårsresultatRepository = mockk<VilkårsresultatRepository>()
    val unleashGateway = mockk<UnleashGateway> {
        every { isEnabled(BehandlingsflytFeature.UtvidVedtakslengdeJobb) } returns true
    }
    val opprettBehandlingUtvidVedtakslengdeJobbUtfører =
        OpprettBehandlingUtvidVedtakslengdeJobbUtfører(prosesserBehandlingService, sakRepository, underveisRepository, sakOgBehandlingService, vilkårsresultatRepository, unleashGateway)
    val jobbInput = JobbInput(OpprettBehandlingUtvidVedtakslengdeJobbUtfører)

    @Test
    fun `skal oppdatere rettighetsperiode og opprette og sette i gang prosessering av behandling hvis sluttdato er innenfor dagens dato + 28 dager`() {
        val sak = sak()
        val behandling = behandlingMedVedtak()

        every { underveisRepository.hentSakerMedSisteUnderveisperiodeFørDato(any()) } returns setOf(sakId_1)
        every { underveisRepository.hentHvisEksisterer(behandling.id)} returns underveisGrunnlag()
        every { sakOgBehandlingService.finnSisteYtelsesbehandlingFor(sakId_1) } returns behandling()
        every { sakOgBehandlingService.finnBehandlingMedSisteFattedeVedtak(sakId_1) } returns behandlingMedVedtak()
        every { sakOgBehandlingService.finnEllerOpprettBehandling(sak.id, any()) } returns opprettetBehandling()
        every { sakRepository.hent(sakId_1) } returns sak
        every { sakRepository.oppdaterRettighetsperiode(sak.id, any()) } just Runs
        every { prosesserBehandlingService.triggProsesserBehandling(any<SakOgBehandlingService.OpprettetBehandling>()) } just Runs
        every { vilkårsresultatRepository.hent(behandlingId) } returns genererVilkårsresultat(sak.rettighetsperiode)

        opprettBehandlingUtvidVedtakslengdeJobbUtfører.utfør(jobbInput)

        verify(exactly = 1) { sakRepository.oppdaterRettighetsperiode(sakId_1, Periode(sak.rettighetsperiode.fom, Tid.MAKS)) }
        verify(exactly = 1) { prosesserBehandlingService.triggProsesserBehandling(any<SakOgBehandlingService.OpprettetBehandling>()) }
    }

    @Test
    fun `skal oppdatere rettighetsperiode og opprette og sette i gang prosessering av behandling hvis rettighetsperiode ikke har fom satt til Tid_MAX`() {
        val sak = sak()
        val behandling = behandlingMedVedtak()

        every { underveisRepository.hentSakerMedSisteUnderveisperiodeFørDato(any()) } returns setOf(sakId_1)
        every { underveisRepository.hentHvisEksisterer(behandling.id)} returns underveisGrunnlag(perioder = underveisPerioderIkkeUtløpt())
        every { sakOgBehandlingService.finnSisteYtelsesbehandlingFor(sakId_1) } returns behandling()
        every { sakOgBehandlingService.finnBehandlingMedSisteFattedeVedtak(sakId_1) } returns behandlingMedVedtak()
        every { sakOgBehandlingService.finnEllerOpprettBehandling(sak.id, any()) } returns opprettetBehandling()
        every { sakRepository.hent(sakId_1) } returns sak
        every { sakRepository.oppdaterRettighetsperiode(sak.id, any()) } just Runs
        every { prosesserBehandlingService.triggProsesserBehandling(any<SakOgBehandlingService.OpprettetBehandling>()) } just Runs
        every { vilkårsresultatRepository.hent(behandlingId) } returns genererVilkårsresultat(sak.rettighetsperiode)

        opprettBehandlingUtvidVedtakslengdeJobbUtfører.utfør(jobbInput)

        verify(exactly = 1) { sakRepository.oppdaterRettighetsperiode(sakId_1, Periode(sak.rettighetsperiode.fom, Tid.MAKS)) }
        verify(exactly = 1) { prosesserBehandlingService.triggProsesserBehandling(any<SakOgBehandlingService.OpprettetBehandling>()) }
    }

    @Test
    fun `skal ikke opprette og sette i gang prosessering av behandling hvis sluttdato er lenger frem enn dagens dato + 28 dager`() {
        val sak = sak(Periode(LocalDate.now().minusDays(180), Tid.MAKS))
        val behandling = behandlingMedVedtak()

        every { underveisRepository.hentSakerMedSisteUnderveisperiodeFørDato(any()) } returns setOf(sakId_1)
        every { underveisRepository.hentHvisEksisterer(behandling.id)} returns underveisGrunnlag(perioder = underveisPerioderIkkeUtløpt())
        every { sakOgBehandlingService.finnSisteYtelsesbehandlingFor(sakId_1) } returns behandling()
        every { sakOgBehandlingService.finnBehandlingMedSisteFattedeVedtak(sakId_1) } returns behandlingMedVedtak()
        every { sakOgBehandlingService.finnEllerOpprettBehandling(sak.id, any()) } returns opprettetBehandling()
        every { sakRepository.hent(sakId_1) } returns sak
        every { sakRepository.oppdaterRettighetsperiode(sak.id, any()) } just Runs
        every { prosesserBehandlingService.triggProsesserBehandling(any<SakOgBehandlingService.OpprettetBehandling>()) } just Runs
        every { vilkårsresultatRepository.hent(behandlingId) } returns genererVilkårsresultat(sak.rettighetsperiode)

        opprettBehandlingUtvidVedtakslengdeJobbUtfører.utfør(jobbInput)

        verify(exactly = 0) { sakRepository.oppdaterRettighetsperiode(sakId_1, Periode(sak.rettighetsperiode.fom, Tid.MAKS)) }
        verify(exactly = 0) { prosesserBehandlingService.triggProsesserBehandling(any<SakOgBehandlingService.OpprettetBehandling>()) }
    }

    @Test
    fun `skal ikke sette i gang noen behandlinger hvis ingen kandidater`() {
        every { underveisRepository.hentSakerMedSisteUnderveisperiodeFørDato(any()) } returns emptySet()

        opprettBehandlingUtvidVedtakslengdeJobbUtfører.utfør(jobbInput)

        verify(exactly = 0) { prosesserBehandlingService.triggProsesserBehandling(any<SakOgBehandlingService.OpprettetBehandling>()) }
    }

    @Test
    fun `skal ikke oppdatere rettighetsperiode dersom tom allerede er Tid_MAKS`() {
        val sak = sak(
            rettighetsperiode = Periode(LocalDate.now().minusDays(180), Tid.MAKS)
        )
        val behandling = behandlingMedVedtak()

        every { underveisRepository.hentSakerMedSisteUnderveisperiodeFørDato(any()) } returns setOf(sakId_1)
        every { underveisRepository.hentHvisEksisterer(behandling.id)} returns underveisGrunnlag()
        every { sakOgBehandlingService.finnSisteYtelsesbehandlingFor(sakId_1) } returns behandling()
        every { sakOgBehandlingService.finnBehandlingMedSisteFattedeVedtak(sakId_1) } returns behandlingMedVedtak()
        every { sakOgBehandlingService.finnEllerOpprettBehandling(sak.id, any()) } returns opprettetBehandling()
        every { sakRepository.hent(sakId_1) } returns sak
        every { prosesserBehandlingService.triggProsesserBehandling(any<SakOgBehandlingService.OpprettetBehandling>()) } just Runs
        every { vilkårsresultatRepository.hent(behandlingId) } returns genererVilkårsresultat(sak.rettighetsperiode)

        opprettBehandlingUtvidVedtakslengdeJobbUtfører.utfør(jobbInput)

        verify(exactly = 0) { sakRepository.oppdaterRettighetsperiode(sakId_1, any()) }
        verify(exactly = 1) { prosesserBehandlingService.triggProsesserBehandling(any<SakOgBehandlingService.OpprettetBehandling>()) }
    }

    @Test
    fun `skal ikke kjøre jobb for behandlinger hvor siste behandling er åpen`() {
        val sak = sak()

        every { underveisRepository.hentSakerMedSisteUnderveisperiodeFørDato(any()) } returns setOf(sakId_1)
        every { sakOgBehandlingService.finnSisteYtelsesbehandlingFor(sakId_1) } returns behandling(status = Status.UTREDES)

        opprettBehandlingUtvidVedtakslengdeJobbUtfører.utfør(jobbInput)

        verify(exactly = 0) { sakRepository.oppdaterRettighetsperiode(sakId_1, Periode(sak.rettighetsperiode.fom, Tid.MAKS)) }
        verify(exactly = 0) { prosesserBehandlingService.triggProsesserBehandling(any<SakOgBehandlingService.OpprettetBehandling>()) }
    }

    private fun behandling(status: Status = Status.IVERKSETTES) =
        Behandling(
            sakId = sakId_1,
            id = behandlingId,
            referanse = BehandlingReferanse(),
            typeBehandling = TypeBehandling.Førstegangsbehandling,
            status = status,
            opprettetTidspunkt = LocalDateTime.now(),
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
            opprettetTidspunkt = LocalDateTime.now(),
            vedtakstidspunkt = LocalDateTime.now(),
            virkningstidspunkt = null,
            vurderingsbehov = setOf(),
            årsakTilOpprettelse = ÅrsakTilOpprettelse.SØKNAD
    )

    private fun sak(rettighetsperiode: Periode = Periode(LocalDate.now().minusDays(180), LocalDate.now().plusDays(10))) =
        Sak(
            id = sakId_1,
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
                    fom = LocalDate.now(),
                    tom = LocalDate.now().plusDays(10)
                )
            },
            mockk<Underveisperiode> {
                every { periode } returns Periode(
                    fom = LocalDate.now().minusYears(1),
                    tom = LocalDate.now().minusDays(1)
                )
            },
        )

    private fun underveisPerioderIkkeUtløpt() =
        listOf(
            mockk<Underveisperiode> {
                every { periode } returns Periode(
                    fom = LocalDate.now(),
                    tom = LocalDate.now().plusDays(28)
                )
            },
            mockk<Underveisperiode> {
                every { periode } returns Periode(
                    fom = LocalDate.now().minusYears(1),
                    tom = LocalDate.now().minusDays(1)
                )
            },
        )

    private fun opprettetBehandling() =
        SakOgBehandlingService.MåBehandlesAtomært(
            nyBehandling = behandling(),
            åpenBehandling = null
        )

    private fun genererVilkårsresultat(periode: Periode): Vilkårsresultat {
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
                        Utfall.OPPFYLT,
                        false,
                        null,
                        faktagrunnlag = null
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