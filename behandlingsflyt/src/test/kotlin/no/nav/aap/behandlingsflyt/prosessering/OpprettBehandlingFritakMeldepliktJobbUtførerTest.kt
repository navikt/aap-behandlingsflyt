package no.nav.aap.behandlingsflyt.prosessering

import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import no.nav.aap.behandlingsflyt.faktagrunnlag.SakOgBehandlingService
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.meldeperiode.MeldeperiodeRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.Fritaksvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.MeldepliktGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.MeldepliktRepository
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingMedVedtak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.StegTilstand
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.StegStatus
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.motor.JobbInput
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import kotlin.test.Test

@ExtendWith(MockKExtension::class)
class OpprettBehandlingFritakMeldepliktJobbUtførerTest {

    private val sakId = SakId(123L)

    @Test
    fun `Skal opprette revurdering pga fritak meldeplikt`() {
        val sakOgBehandlingServiceMock = mockk<SakOgBehandlingService>()
        val utfører = mockAvhengigheterForOpprettBehandlingFritakMeldepliktJobbUtfører(sakOgBehandlingServiceMock)

        utfører.utfør(JobbInput(OpprettBehandlingFritakMeldepliktJobbUtfører).forSak(sakId.id))

        verify { sakOgBehandlingServiceMock.finnEllerOpprettBehandling(any<SakId>(), any()) }
    }

    @Test
    fun `Skal ikke opprette revurdering dersom det ikke finnes fritak for meldeplikt`() {
        val sakOgBehandlingServiceMock = mockk<SakOgBehandlingService>()
        val utfører =
            mockAvhengigheterForOpprettBehandlingFritakMeldepliktJobbUtfører(sakOgBehandlingServiceMock, fritak = false)

        utfører.utfør(JobbInput(OpprettBehandlingFritakMeldepliktJobbUtfører).forSak(sakId.id))

        verify(exactly = 0) { sakOgBehandlingServiceMock.finnEllerOpprettBehandling(any<SakId>(), any()) }
    }

    @Test
    fun `Skal ikke opprette revurdering dersom det finnes andre åpne behandlinger med årsak fritak meldeplikt`() {
        val sakOgBehandlingServiceMock = mockk<SakOgBehandlingService>()
        val utfører = mockAvhengigheterForOpprettBehandlingFritakMeldepliktJobbUtfører(
            sakOgBehandlingServiceMock,
            årsakerPåTidligereBehandling = listOf(VurderingsbehovMedPeriode(Vurderingsbehov.FRITAK_MELDEPLIKT))
        )

        utfører.utfør(JobbInput(OpprettBehandlingFritakMeldepliktJobbUtfører).forSak(sakId.id))

        verify(exactly = 0) { sakOgBehandlingServiceMock.finnEllerOpprettOrdinærBehandling(any<SakId>(), any()) }
    }

    private fun mockAvhengigheterForOpprettBehandlingFritakMeldepliktJobbUtfører(
        sakOgBehandlingServiceMock: SakOgBehandlingService,
        fritak: Boolean = true,
        årsakerPåTidligereBehandling: List<VurderingsbehovMedPeriode> = emptyList(),
    ): OpprettBehandlingFritakMeldepliktJobbUtfører {
        val sakServiceMock = mockk<SakService>()
        val behandlingRepositoryMock = mockk<BehandlingRepository>()
        val meldeperiodeRepositoryMock = mockk<MeldeperiodeRepository>()
        val meldepliktRepositoryMock = mockk<MeldepliktRepository>()

        every { sakServiceMock.hent(any<SakId>()) } returns Sak(
            id = sakId,
            saksnummer = Saksnummer("BLABLA"),
            person = Person(
                id = 456L.let(::PersonId),
                identifikator = UUID.randomUUID(),
                identer = emptyList()
            ),
            rettighetsperiode = Periode(LocalDate.now().minusDays(14), LocalDate.now().plusDays(14)),
            status = no.nav.aap.behandlingsflyt.kontrakt.sak.Status.OPPRETTET,
            opprettetTidspunkt = LocalDateTime.now(),
        )

        every { behandlingRepositoryMock.finnSisteOpprettedeBehandlingFor(any(), any())} returns Behandling(
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
                aktiv = true),
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

        every { sakOgBehandlingServiceMock.finnSisteYtelsesbehandlingFor(sakId) } returns fakeBehandling
        every {
            sakOgBehandlingServiceMock.finnEllerOpprettBehandling(
                sakId,
                any()
            )
        } returns SakOgBehandlingService.Ordinær(fakeBehandling)

        every { sakOgBehandlingServiceMock.finnEllerOpprettOrdinærBehandling(any<SakId>(), any()) } returns fakeBehandling


        every { sakOgBehandlingServiceMock.finnBehandlingMedSisteFattedeVedtak(any()) } returns BehandlingMedVedtak(
            saksnummer = Saksnummer("123"),
            id = BehandlingId(456L),
            referanse = BehandlingReferanse(UUID.randomUUID()),
            typeBehandling = TypeBehandling.Revurdering,
            status = Status.AVSLUTTET,
            opprettetTidspunkt = LocalDateTime.now(),
            vedtakstidspunkt = LocalDateTime.now(),
            virkningstidspunkt = LocalDate.now(),
            vurderingsbehov = setOf(),
            årsakTilOpprettelse = ÅrsakTilOpprettelse.SØKNAD
        )


        every { meldeperiodeRepositoryMock.hentMeldeperioder(any(), any())} returns listOf(
            Periode(fom = LocalDate.now().minusDays(10), tom = LocalDate.now()),
        )

        every { meldepliktRepositoryMock.hentHvisEksisterer(any()) } returns MeldepliktGrunnlag(
            vurderinger = listOf(Fritaksvurdering(
                harFritak = fritak,
                fraDato = LocalDate.now(),
                begrunnelse = "bla bla",
                vurdertAv = "saksbehandler1",
                opprettetTid = LocalDateTime.now(),
            ))
        )


        return OpprettBehandlingFritakMeldepliktJobbUtfører(
            sakService = sakServiceMock,
            behandlingRepository = behandlingRepositoryMock,
            meldeperiodeRepository = meldeperiodeRepositoryMock,
            meldepliktRepository = meldepliktRepositoryMock,
            sakOgBehandlingService = sakOgBehandlingServiceMock,
            prosesserBehandlingService = mockk<ProsesserBehandlingService>(relaxed = true),
            underveisRepository = mockk(relaxed = true),
        )
    }
}