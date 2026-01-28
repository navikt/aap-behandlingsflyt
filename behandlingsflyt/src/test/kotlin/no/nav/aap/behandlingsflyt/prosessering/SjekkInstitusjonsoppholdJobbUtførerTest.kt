package no.nav.aap.behandlingsflyt.prosessering

import io.mockk.Runs
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import no.nav.aap.behandlingsflyt.faktagrunnlag.SakOgBehandlingService
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.Institusjonsopphold
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.InstitusjonsoppholdGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.InstitusjonsoppholdRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.Institusjonstype
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.Oppholdene
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.Oppholdstype
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
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.motor.JobbInput
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import kotlin.test.Test

@ExtendWith(MockKExtension::class)
class `SjekkInstitusjonsoppholdJobbUtførerTest` {

    private val sakId = SakId(123L)

    @Test
    fun `Skal opprette revurdering pga institusjonsopphold`() {
        val (utfører, sakOgBehandlingServiceMock) =
            mockAvhengigheterForInstitusjonsoppholdJobbUtfører(
                hentInstitusjonsoppholdReturn = kortvarigInstitusjonsopphold
            )

        utfører.utfør(JobbInput(SjekkInstitusjonsOppholdJobbUtfører).forSak(sakId.id))

        verify { sakOgBehandlingServiceMock.finnEllerOpprettOrdinærBehandling(any<SakId>(), any()) }
    }

    @Test
    fun `Skal ikke opprette revurdering dersom det ikke finnes institusjonsopphold`() {
        val (utfører, sakOgBehandlingServiceMock) =
            mockAvhengigheterForInstitusjonsoppholdJobbUtfører(
                hentInstitusjonsoppholdReturn = langvarigInstitusjonsopphold
            )

        utfører.utfør(JobbInput(SjekkInstitusjonsOppholdJobbUtfører).forSak(sakId.id))

        verify(exactly = 0) { sakOgBehandlingServiceMock.finnEllerOpprettOrdinærBehandling(any<SakId>(), any()) }
    }

    private fun mockAvhengigheterForInstitusjonsoppholdJobbUtfører(
        årsakerPåTidligereBehandling: List<VurderingsbehovMedPeriode> = emptyList(),
        hentInstitusjonsoppholdReturn: InstitusjonsoppholdGrunnlag
    ): Pair<SjekkInstitusjonsOppholdJobbUtfører, SakOgBehandlingService>  {
        val sakServiceMock = mockk<SakService>()
        val sakOgBehandlingServiceMock = mockk<SakOgBehandlingService>()
        val sakRepositoryMock = mockk<SakRepository>()
        val behandlingRepositoryMock = mockk<BehandlingRepository>()
        val prosesserBehandlingServiceMock = mockk<ProsesserBehandlingService>()
        val institusjonsoppholdRepositoryMock = mockk<InstitusjonsoppholdRepository>()
        val unleashGateway = mockk<UnleashGateway> {
            every { isEnabled(BehandlingsflytFeature.InstitusjonsoppholdJobb) } returns true
        }

        every { institusjonsoppholdRepositoryMock.hentHvisEksisterer(any()) } returns hentInstitusjonsoppholdReturn

        every { sakServiceMock.hent(any<SakId>()) } returns Sak(
            id = sakId,
            saksnummer = Saksnummer("DUMMYSAKSNR"),
            person = Person(
                id = 456L.let(::PersonId),
                identifikator = UUID.randomUUID(),
                identer = emptyList()
            ),
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

        val fakeOpprettetBehandling = Behandling(
            id = BehandlingId(457L),
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

        every { sakOgBehandlingServiceMock.finnEllerOpprettOrdinærBehandling(any<SakId>(), any()) } returns fakeOpprettetBehandling

        every {
            prosesserBehandlingServiceMock.triggProsesserBehandling(
                fakeOpprettetBehandling,
                any(),
                any()
            )
        } just Runs

        every { sakOgBehandlingServiceMock.finnBehandlingMedSisteFattedeVedtak(any()) } returns BehandlingMedVedtak(
            saksnummer = Saksnummer("DUMMYSAKSNR"),
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

        every { sakRepositoryMock.hent(SakId(123)) } returns
                Sak(
                    id = sakId,
                    saksnummer = Saksnummer("DUMMYSAKSNR"),
                    person = Person(
                        id = 456L.let(::PersonId),
                        identifikator = UUID.randomUUID(),
                        identer = emptyList()
                    ),
                    rettighetsperiode = Periode(LocalDate.now().minusDays(14), LocalDate.now().plusDays(14)),
                    status = no.nav.aap.behandlingsflyt.kontrakt.sak.Status.OPPRETTET,
                    opprettetTidspunkt = LocalDateTime.now(),
                )

        every { sakRepositoryMock.finnSakerMedInstitusjonsOpphold() } returns
                listOf(
                    Sak(
                        id = sakId,
                        saksnummer = Saksnummer("DUMMYSAKSNR"),
                        person = Person(
                            id = 456L.let(::PersonId),
                            identifikator = UUID.randomUUID(),
                            identer = emptyList()
                        ),
                        rettighetsperiode = Periode(LocalDate.now().minusDays(14), LocalDate.now().plusDays(14)),
                        status = no.nav.aap.behandlingsflyt.kontrakt.sak.Status.OPPRETTET,
                        opprettetTidspunkt = LocalDateTime.now(),
                    )
                )

        return SjekkInstitusjonsOppholdJobbUtfører(
            prosesserBehandlingService = prosesserBehandlingServiceMock,
            sakRepository = sakRepositoryMock,
            institusjonsOppholdRepository = institusjonsoppholdRepositoryMock,
            sakOgBehandlingService = sakOgBehandlingServiceMock,
            unleashGateway = unleashGateway,
        ) to sakOgBehandlingServiceMock
    }


    val kortvarigInstitusjonsopphold = InstitusjonsoppholdGrunnlag(
        Oppholdene(
            1, listOf(
                Institusjonsopphold(
                    Institusjonstype.HS,
                    Oppholdstype.H,
                    LocalDate.now().minusMonths(1),
                    LocalDate.now().plusMonths(2),
                    orgnr = "123",
                    institusjonsnavn = "Det er institusjonen sin, det",
                ),
            ).map { it.tilInstitusjonSegment() })
    )

    val langvarigInstitusjonsopphold = InstitusjonsoppholdGrunnlag(
        Oppholdene(
            1, listOf(
                Institusjonsopphold(
                    Institusjonstype.HS,
                    Oppholdstype.H,
                    LocalDate.now().minusMonths(1),
                    LocalDate.now().plusYears(4),
                    orgnr = "123",
                    institusjonsnavn = "Det er institusjonen sin, det",
                ),
            ).map { it.tilInstitusjonSegment() })
    )

}