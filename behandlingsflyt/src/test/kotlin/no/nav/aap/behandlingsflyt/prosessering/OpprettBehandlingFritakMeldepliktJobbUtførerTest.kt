package no.nav.aap.behandlingsflyt.prosessering

import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.MeldepliktStatus
import no.nav.aap.behandlingsflyt.faktagrunnlag.SakOgBehandlingService
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.ArbeidsGradering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.Underveisperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisperiodeId
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
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
import no.nav.aap.komponenter.verdityper.Dagsatser
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.komponenter.verdityper.TimerArbeid
import no.nav.aap.motor.JobbInput
import org.junit.jupiter.api.extension.ExtendWith
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.Test

@ExtendWith(MockKExtension::class)
class OpprettBehandlingFritakMeldepliktJobbUtførerTest {

    private val sakId = SakId(123L)

    @Test
    fun `Skal opprette revurdering pga fritak meldeplikt`() {
        val sakOgBehandlingServiceMock = mockk<SakOgBehandlingService>()
        val utfører = mockAvhengigheterForOpprettBehandlingFritakMeldepliktJobbUtfører(sakOgBehandlingServiceMock)

        utfører.utfør(JobbInput(OpprettBehandlingFritakMeldepliktJobbUtfører).forSak(sakId.id))

        verify { sakOgBehandlingServiceMock.finnEllerOpprettBehandlingFasttrack(any<SakId>(), any(), any()) }
    }

    @Test
    fun `Skal ikke opprette revurdering dersom det ikke finnes fritak for meldeplikt`() {
        val sakOgBehandlingServiceMock = mockk<SakOgBehandlingService>()
        val utfører =
            mockAvhengigheterForOpprettBehandlingFritakMeldepliktJobbUtfører(sakOgBehandlingServiceMock, fritak = false)

        utfører.utfør(JobbInput(OpprettBehandlingFritakMeldepliktJobbUtfører).forSak(sakId.id))

        verify(exactly = 0) { sakOgBehandlingServiceMock.finnEllerOpprettBehandlingFasttrack(any<SakId>(), any(), any()) }
    }

    @Test
    fun `Skal ikke opprette revurdering dersom det finnes andre åpne behandlinger med årsak fritak meldeplikt`() {
        val sakOgBehandlingServiceMock = mockk<SakOgBehandlingService>()
        val utfører = mockAvhengigheterForOpprettBehandlingFritakMeldepliktJobbUtfører(
            sakOgBehandlingServiceMock,
            årsakerPåTidligereBehandling = listOf(VurderingsbehovMedPeriode(Vurderingsbehov.FRITAK_MELDEPLIKT))
        )

        utfører.utfør(JobbInput(OpprettBehandlingFritakMeldepliktJobbUtfører).forSak(sakId.id))

        verify(exactly = 0) { sakOgBehandlingServiceMock.finnEllerOpprettBehandling(any<SakId>(), any(), any()) }
    }

    private fun mockAvhengigheterForOpprettBehandlingFritakMeldepliktJobbUtfører(
        sakOgBehandlingServiceMock: SakOgBehandlingService,
        fritak: Boolean = true,
        årsakerPåTidligereBehandling: List<VurderingsbehovMedPeriode> = emptyList(),
    ): OpprettBehandlingFritakMeldepliktJobbUtfører {
        val sakServiceMock = mockk<SakService>()
        val underveisRepositoryMock = mockk<UnderveisRepository>()

        every { sakServiceMock.hent(any<SakId>()) } returns Sak(
            id = sakId,
            saksnummer = Saksnummer("BLABLA"),
            person = Person(
                id = 456L.let(::PersonId),
                identifikator = UUID.randomUUID(),
                identer = listOf()
            ),
            rettighetsperiode = Periode(LocalDate.now().minusDays(14), LocalDate.now().plusDays(14)),
            status = no.nav.aap.behandlingsflyt.kontrakt.sak.Status.OPPRETTET,
            opprettetTidspunkt = LocalDateTime.now(),
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
            sakOgBehandlingServiceMock.finnEllerOpprettBehandlingFasttrack(
                sakId,
                any(),
                any()
            )
        } returns SakOgBehandlingService.Ordinær(fakeBehandling)

        every { underveisRepositoryMock.hentHvisEksisterer(any()) } returns UnderveisGrunnlag(
            id = 1L,
            perioder = listOf(
                Underveisperiode(
                    periode = Periode(fom = LocalDate.now().minusDays(10), tom = LocalDate.now().minusDays(10)),
                    meldePeriode = Periode(fom = LocalDate.now().minusDays(10), tom = LocalDate.now().minusDays(10)),
                    utfall = Utfall.OPPFYLT,
                    rettighetsType = RettighetsType.VURDERES_FOR_UFØRETRYGD,
                    avslagsårsak = null,
                    grenseverdi = Prosent(50),
                    institusjonsoppholdReduksjon = Prosent(0),
                    arbeidsgradering = ArbeidsGradering(
                        TimerArbeid(BigDecimal.ZERO),
                        Prosent(0),
                        Prosent(0),
                        Prosent(0),
                        null
                    ),
                    trekk = Dagsatser(0),
                    brukerAvKvoter = setOf(),
                    bruddAktivitetspliktId = null,
                    meldepliktStatus = if (fritak) MeldepliktStatus.FRITAK else MeldepliktStatus.IKKE_MELDT_SEG,
                    id = UnderveisperiodeId(3)
                )
            )
        )

        every { sakOgBehandlingServiceMock.finnEllerOpprettBehandling(any<SakId>(), any(), any()) } returns fakeBehandling

        return OpprettBehandlingFritakMeldepliktJobbUtfører(
            sakService = sakServiceMock,
            underveisRepository = underveisRepositoryMock,
            sakOgBehandlingService = sakOgBehandlingServiceMock,
            prosesserBehandlingService = mockk<ProsesserBehandlingService>(relaxed = true)
        )
    }
}