package no.nav.aap.behandlingsflyt.prosessering

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.MeldepliktStatus
import no.nav.aap.behandlingsflyt.behandling.vedtak.VedtakId
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.meldeperiode.MeldeperiodeRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.ArbeidsGradering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.Underveisperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.help.StubTaSkrivelåsRepository
import no.nav.aap.behandlingsflyt.help.person
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingMedVedtak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingService
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.StegTilstand
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.StegStatus
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Dagsatser
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.komponenter.verdityper.TimerArbeid
import no.nav.aap.motor.JobbInput
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import kotlin.test.Test

class OpprettBehandlingFritakMeldepliktJobbUtførerTest {
    private val sakId = SakId(123L)

    @Test
    fun `Skal opprette revurdering pga fritak meldeplikt`() {
        val behandlingServiceMock = mockk<BehandlingService>()
        val utfører = mockAvhengigheterForOpprettBehandlingFritakMeldepliktJobbUtfører(behandlingServiceMock)

        utfører.utfør(JobbInput(OpprettBehandlingFritakMeldepliktJobbUtfører).forSak(sakId.id))

        verify { behandlingServiceMock.finnEllerOpprettBehandling(any<SakId>(), any()) }
    }

    @Test
    fun `Skal ikke opprette revurdering dersom det ikke finnes fritak for meldeplikt`() {
        val behandlingServiceMock = mockk<BehandlingService>()
        val utfører =
            mockAvhengigheterForOpprettBehandlingFritakMeldepliktJobbUtfører(behandlingServiceMock, fritak = false)

        utfører.utfør(JobbInput(OpprettBehandlingFritakMeldepliktJobbUtfører).forSak(sakId.id))

        verify(exactly = 0) { behandlingServiceMock.finnEllerOpprettBehandling(any<SakId>(), any()) }
    }

    @Test
    fun `Skal ikke opprette revurdering dersom det finnes andre åpne behandlinger med årsak fritak meldeplikt`() {
        val behandlingServiceMock = mockk<BehandlingService>()
        val utfører = mockAvhengigheterForOpprettBehandlingFritakMeldepliktJobbUtfører(
            behandlingServiceMock,
            årsakerPåTidligereBehandling = listOf(VurderingsbehovMedPeriode(Vurderingsbehov.FRITAK_MELDEPLIKT))
        )

        utfører.utfør(JobbInput(OpprettBehandlingFritakMeldepliktJobbUtfører).forSak(sakId.id))

        verify(exactly = 0) { behandlingServiceMock.finnEllerOpprettOrdinærBehandling(any<SakId>(), any()) }
    }

    private fun mockAvhengigheterForOpprettBehandlingFritakMeldepliktJobbUtfører(
        behandlingServiceMock: BehandlingService,
        fritak: Boolean = true,
        årsakerPåTidligereBehandling: List<VurderingsbehovMedPeriode> = emptyList(),
    ): OpprettBehandlingFritakMeldepliktJobbUtfører {
        val sakServiceMock = mockk<SakService>()
        val behandlingRepositoryMock = mockk<BehandlingRepository>()
        val meldeperiodeRepositoryMock = mockk<MeldeperiodeRepository>()
        val underveisRepositoryMock = mockk<UnderveisRepository>(relaxed = true)

        every { sakServiceMock.hent(any<SakId>()) } returns Sak(
            id = sakId,
            saksnummer = Saksnummer("BLABLA"),
            person = person(),
            rettighetsperiode = Periode(LocalDate.now().minusDays(14), LocalDate.now().plusDays(14)),
            status = no.nav.aap.behandlingsflyt.kontrakt.sak.Status.OPPRETTET,
            opprettetTidspunkt = LocalDateTime.now(),
        )

        every { behandlingRepositoryMock.finnSisteOpprettedeBehandlingFor(any(), any()) } returns Behandling(
            id = BehandlingId(457L),
            forrigeBehandlingId = BehandlingId(456L),
            referanse = BehandlingReferanse(UUID.randomUUID()),
            sakId = sakId,
            typeBehandling = TypeBehandling.Revurdering,
            status = Status.AVSLUTTET,
            vurderingsbehov = årsakerPåTidligereBehandling,
            stegTilstand = StegTilstand(
                stegStatus = StegStatus.AVSLUTTER,
                stegType = StegType.IVERKSETT_VEDTAK,
                aktiv = true
            ),
            årsakTilOpprettelse = ÅrsakTilOpprettelse.SØKNAD,
            opprettetTidspunkt = LocalDateTime.now(),
            versjon = 0L,
        )

        val fakeBehandling = Behandling(
            id = BehandlingId(456L),
            forrigeBehandlingId = BehandlingId(454L),
            referanse = BehandlingReferanse(UUID.randomUUID()),
            sakId = sakId,
            typeBehandling = TypeBehandling.Revurdering,
            status = Status.OPPRETTET,
            årsakTilOpprettelse = ÅrsakTilOpprettelse.SØKNAD,
            vurderingsbehov = årsakerPåTidligereBehandling,
            stegTilstand = StegTilstand(
                stegStatus = StegStatus.AVKLARINGSPUNKT,
                stegType = StegType.FORESLÅ_VEDTAK,
                aktiv = true
            ),
            opprettetTidspunkt = LocalDateTime.now(),
            versjon = 0L
        )

        every { behandlingServiceMock.finnSisteYtelsesbehandlingFor(sakId) } returns fakeBehandling
        every {
            behandlingServiceMock.finnEllerOpprettBehandling(
                sakId,
                any()
            )
        } returns BehandlingService.Ordinær(fakeBehandling)

        every { behandlingServiceMock.finnEllerOpprettOrdinærBehandling(any<SakId>(), any()) } returns fakeBehandling


        every { behandlingServiceMock.finnBehandlingMedSisteFattedeVedtak(any()) } returns BehandlingMedVedtak(
            saksnummer = Saksnummer("123"),
            id = BehandlingId(456L),
            referanse = BehandlingReferanse(UUID.randomUUID()),
            typeBehandling = TypeBehandling.Revurdering,
            status = Status.AVSLUTTET,
            opprettetTidspunkt = LocalDateTime.now(),
            vedtakId = VedtakId(0),
            vedtakstidspunkt = LocalDateTime.now(),
            virkningstidspunkt = LocalDate.now(),
            vurderingsbehov = setOf(),
            årsakTilOpprettelse = ÅrsakTilOpprettelse.SØKNAD,
            forrigeBehandlingId = BehandlingId(455L),
        )


        every { meldeperiodeRepositoryMock.hentMeldeperioder(any(), any()) } returns listOf(
            Periode(fom = LocalDate.now().minusDays(10), tom = LocalDate.now()),
        )

        val nå = LocalDate.now()
        val meldeperiode = Periode(nå, nå.plusDays(13))
        val forrigeMeldeperiode = Periode(nå.minusDays(14), nå.minusDays(1))
        every { underveisRepositoryMock.hentHvisEksisterer(any()) } returns UnderveisGrunnlag(
            id = 0L,
            perioder = listOf(
                Underveisperiode(
                    periode = meldeperiode,
                    meldePeriode = meldeperiode,
                    utfall = Utfall.OPPFYLT,
                    rettighetsType = RettighetsType.BISTANDSBEHOV,
                    avslagsårsak = null,
                    grenseverdi = Prosent.`0_PROSENT`,
                    institusjonsoppholdReduksjon = Prosent.`0_PROSENT`,
                    arbeidsgradering = ArbeidsGradering(
                        totaltAntallTimer = TimerArbeid(BigDecimal.ZERO),
                        andelArbeid = Prosent.`0_PROSENT`,
                        fastsattArbeidsevne = Prosent.`0_PROSENT`,
                        gradering = Prosent.`100_PROSENT`,
                        opplysningerMottatt = nå,
                    ),
                    trekk = Dagsatser(0),
                    brukerAvKvoter = setOf(),
                    meldepliktStatus = if (fritak) MeldepliktStatus.FRITAK else MeldepliktStatus.IKKE_MELDT_SEG,
                    meldepliktGradering = null,
                ),
                Underveisperiode(
                    periode = forrigeMeldeperiode,
                    meldePeriode = forrigeMeldeperiode,
                    utfall = Utfall.OPPFYLT,
                    rettighetsType = RettighetsType.BISTANDSBEHOV,
                    avslagsårsak = null,
                    grenseverdi = Prosent.`0_PROSENT`,
                    institusjonsoppholdReduksjon = Prosent.`0_PROSENT`,
                    arbeidsgradering = ArbeidsGradering(
                        totaltAntallTimer = TimerArbeid(BigDecimal.ZERO),
                        andelArbeid = Prosent.`0_PROSENT`,
                        fastsattArbeidsevne = Prosent.`0_PROSENT`,
                        gradering = Prosent.`100_PROSENT`,
                        opplysningerMottatt = nå,
                    ),
                    trekk = Dagsatser(0),
                    brukerAvKvoter = setOf(),
                    meldepliktStatus = if (fritak) MeldepliktStatus.FRITAK else MeldepliktStatus.IKKE_MELDT_SEG,
                    meldepliktGradering = null,
                )
            ),
        )

        return OpprettBehandlingFritakMeldepliktJobbUtfører(
            sakService = sakServiceMock,
            behandlingRepository = behandlingRepositoryMock,
            meldeperiodeRepository = meldeperiodeRepositoryMock,
            behandlingService = behandlingServiceMock,
            prosesserBehandlingService = mockk<ProsesserBehandlingService>(relaxed = true),
            underveisRepository = underveisRepositoryMock,
            taSkriveLåsRepository = StubTaSkrivelåsRepository,
        )
    }
}