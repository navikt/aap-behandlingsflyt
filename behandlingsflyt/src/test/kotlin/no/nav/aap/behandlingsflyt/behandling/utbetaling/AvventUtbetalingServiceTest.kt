package no.nav.aap.behandlingsflyt.behandling.utbetaling

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import no.nav.aap.behandlingsflyt.behandling.vedtak.Vedtak
import no.nav.aap.behandlingsflyt.behandling.vedtak.VedtakService
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.andrestatligeytelservurdering.AndreStatligeYtelser
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.andrestatligeytelservurdering.SamordningAndreStatligeYtelserGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.andrestatligeytelservurdering.SamordningAndreStatligeYtelserRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.andrestatligeytelservurdering.SamordningAndreStatligeYtelserVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.andrestatligeytelservurdering.SamordningAndreStatligeYtelserVurderingPeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.arbeidsgiver.SamordningArbeidsgiverGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.arbeidsgiver.SamordningArbeidsgiverRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.arbeidsgiver.SamordningArbeidsgiverVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.refusjonkrav.RefusjonkravRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.refusjonkrav.RefusjonkravVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.samordning.refusjonskrav.TjenestepensjonRefusjonsKravVurderingRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.samordning.refusjonskrav.TjenestepensjonRefusjonskravVurdering
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.test.AlleAvskruddUnleash
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.utbetal.kodeverk.AvventÅrsak
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import java.time.LocalDate
import kotlin.test.Test

@ExtendWith(MockKExtension::class)
@MockKExtension.CheckUnnecessaryStub
@MockKExtension.RequireParallelTesting
@Execution(ExecutionMode.SAME_THREAD)
class AvventUtbetalingServiceTest {

    val vedtak = Vedtak(
        behandlingId = BehandlingId(123L),
        vedtakstidspunkt = LocalDate.parse("2025-01-15").atStartOfDay(),
        virkningstidspunkt = LocalDate.parse("2025-01-10"),
    )


    val behandling = Behandling(
        BehandlingId(123L),
        sakId = SakId(1),
        typeBehandling = TypeBehandling.Førstegangsbehandling,
        årsakTilOpprettelse = ÅrsakTilOpprettelse.SØKNAD,
        forrigeBehandlingId = null,
        versjon = 1
    )

    private lateinit var refusjonkravRepositoryMock: RefusjonkravRepository
    private lateinit var tjenestepensjonRefusjonsKravVurderingRepositoryMock: TjenestepensjonRefusjonsKravVurderingRepository
    private lateinit var samordningAndreStatligeYtelserRepositoryMock: SamordningAndreStatligeYtelserRepository
    private lateinit var samordningArbeidsgiverRepositoryMock: SamordningArbeidsgiverRepository
    private lateinit var vedtakServiceMock: VedtakService
    private lateinit var behandlingRepositoryMock: BehandlingRepository
    private lateinit var service: AvventUtbetalingService

    @BeforeEach
    fun setup() {
        refusjonkravRepositoryMock = mockk<RefusjonkravRepository>()
        tjenestepensjonRefusjonsKravVurderingRepositoryMock = mockk<TjenestepensjonRefusjonsKravVurderingRepository>()
        samordningAndreStatligeYtelserRepositoryMock = mockk<SamordningAndreStatligeYtelserRepository>()
        samordningArbeidsgiverRepositoryMock = mockk<SamordningArbeidsgiverRepository>()
        vedtakServiceMock = mockk<VedtakService>()
        behandlingRepositoryMock = mockk<BehandlingRepository>()

        service = AvventUtbetalingService(
            refusjonkravRepositoryMock,
            tjenestepensjonRefusjonsKravVurderingRepositoryMock,
            samordningAndreStatligeYtelserRepositoryMock,
            samordningArbeidsgiverRepositoryMock,
            vedtakServiceMock,
            behandlingRepositoryMock,
        )
    }


    @AfterEach
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `Ingen refusjonskrav skal føre til ingen avvent utbetaling`() {

        every { vedtakServiceMock.hentVedtak(any()) } returns vedtak
        every { refusjonkravRepositoryMock.hentHvisEksisterer(any()) } returns null
        every { tjenestepensjonRefusjonsKravVurderingRepositoryMock.hentHvisEksisterer(any()) } returns null
        every { samordningAndreStatligeYtelserRepositoryMock.hentHvisEksisterer(any()) } returns null
        every { samordningArbeidsgiverRepositoryMock.hentHvisEksisterer(any()) } returns null

        val avventUtbetaling = service.finnEventuellAvventUtbetaling(
            førsteVedtak = vedtak,
            behandling = behandling,
            tilkjentYtelseHelePerioden = Periode(LocalDate.parse("2025-01-01"), LocalDate.parse("2025-01-31"))
        )

        assertNull(avventUtbetaling)
    }

    @Test
    fun `Refusjonskrav utenfor ytelsesperioden skal føre til ingen avvent utbetaling`() {
        every { vedtakServiceMock.hentVedtak(any()) } returns vedtak

        every { refusjonkravRepositoryMock.hentHvisEksisterer(any()) } returns listOf(
            RefusjonkravVurdering(
                true,
                LocalDate.parse("2024-01-01"),
                LocalDate.parse("2024-01-31"),
                "Nav Løten",
                "saksbehandler"
            )
        )
        every { tjenestepensjonRefusjonsKravVurderingRepositoryMock.hentHvisEksisterer(any()) } returns null
        every { samordningAndreStatligeYtelserRepositoryMock.hentHvisEksisterer(any()) } returns null
        every { samordningArbeidsgiverRepositoryMock.hentHvisEksisterer(any()) } returns null


        val avventUtbetaling = service.finnEventuellAvventUtbetaling(
            førsteVedtak = vedtak,
            behandling = behandling,
            tilkjentYtelseHelePerioden = Periode(LocalDate.parse("2025-01-01"), LocalDate.parse("2025-01-31"))
        )

        assertNull(avventUtbetaling)
    }


    @Test
    fun `Refusjonskrav i revrudering setter riktig refusjons datoer`(){

        /**
         * Skal ha refusjonskrav i perioden virking og vedtak i førstegang behandling
         */

        val førstegangBehandling = Behandling(
            BehandlingId(1L),
            sakId = SakId(1),
            typeBehandling = TypeBehandling.Førstegangsbehandling,
            årsakTilOpprettelse = ÅrsakTilOpprettelse.SØKNAD,
            forrigeBehandlingId = null,
            versjon = 1
        )

        val førstegangVedtak = Vedtak(
            behandlingId = BehandlingId(1L),
            vedtakstidspunkt = LocalDate.parse("2025-01-15").atStartOfDay(),
            virkningstidspunkt = LocalDate.parse("2025-01-10"),
        )

        val revurderingVedtak = Vedtak(
            behandlingId = BehandlingId(2L),
            vedtakstidspunkt = LocalDate.parse("2025-01-20").atStartOfDay(),
            virkningstidspunkt = LocalDate.parse("2025-01-16"),
        )

        val revurderingBehandling = Behandling(
            BehandlingId(2L),
            sakId = SakId(1),
            typeBehandling = TypeBehandling.Revurdering,
            årsakTilOpprettelse = ÅrsakTilOpprettelse.KLAGE,
            forrigeBehandlingId = BehandlingId(1L),
            versjon = 1
        )


        every { behandlingRepositoryMock.hent(BehandlingId(1L)) } returns førstegangBehandling
        every { vedtakServiceMock.hentVedtak(any()) } returns vedtak
        every { refusjonkravRepositoryMock.hentHvisEksisterer(any()) } returns
            listOf(RefusjonkravVurdering(true, null, null, "Nav Løten", "saksbehandler"))
        every { tjenestepensjonRefusjonsKravVurderingRepositoryMock.hentHvisEksisterer(any()) } returns null
        every { samordningAndreStatligeYtelserRepositoryMock.hentHvisEksisterer(any()) } returns null
        every { samordningArbeidsgiverRepositoryMock.hentHvisEksisterer(any()) } returns null



        val avventUtbetaling = service.finnEventuellAvventUtbetaling(
            førsteVedtak = førstegangVedtak,
            behandling = revurderingBehandling,
            tilkjentYtelseHelePerioden = Periode(LocalDate.parse("2025-01-01"), LocalDate.parse("2025-01-31"))
        )

        assertNotNull(avventUtbetaling)
        assertThat(avventUtbetaling?.fom).isEqualTo(LocalDate.parse("2025-01-10"))
        assertThat(avventUtbetaling?.tom).isEqualTo(LocalDate.parse("2025-01-14"))
        assertThat(avventUtbetaling?.overføres).isEqualTo(LocalDate.parse("2025-01-15").plusDays(21))
        assertThat(avventUtbetaling?.årsak).isEqualTo(AvventÅrsak.AVVENT_REFUSJONSKRAV)
        assertThat(avventUtbetaling?.feilregistrering).isFalse()


    }




    @Test
    fun `Refusjonskrav med åpen tom overlapper med tilkjent ytelse skal føre til avvent utbetaling`() {

        every { vedtakServiceMock.hentVedtak(any()) } returns vedtak

        every { refusjonkravRepositoryMock.hentHvisEksisterer(any()) } returns
                listOf(RefusjonkravVurdering(true, null, null, "Nav Løten", "saksbehandler"))
        every { tjenestepensjonRefusjonsKravVurderingRepositoryMock.hentHvisEksisterer(any()) } returns null
        every { samordningAndreStatligeYtelserRepositoryMock.hentHvisEksisterer(any()) } returns null
        every { samordningArbeidsgiverRepositoryMock.hentHvisEksisterer(any()) } returns null


        val avventUtbetaling = service.finnEventuellAvventUtbetaling(
            førsteVedtak = vedtak,
            behandling = behandling,
            tilkjentYtelseHelePerioden = Periode(LocalDate.parse("2025-01-01"), LocalDate.parse("2025-01-31"))
        )

        assertNotNull(avventUtbetaling)
        assertThat(avventUtbetaling?.fom).isEqualTo(LocalDate.parse("2025-01-10"))
        assertThat(avventUtbetaling?.tom).isEqualTo(LocalDate.parse("2025-01-14"))
        assertThat(avventUtbetaling?.overføres).isEqualTo(LocalDate.parse("2025-01-15").plusDays(21))
        assertThat(avventUtbetaling?.årsak).isEqualTo(AvventÅrsak.AVVENT_REFUSJONSKRAV)
        assertThat(avventUtbetaling?.feilregistrering).isFalse()
    }

    @Test
    fun `Tjenestepensjon refusjonskrav overlapper med tilkjent ytelse skal føre til avvent utbetaling`() {

        every { vedtakServiceMock.hentVedtak(any()) } returns vedtak

        every { refusjonkravRepositoryMock.hentHvisEksisterer(any()) } returns null
        every { tjenestepensjonRefusjonsKravVurderingRepositoryMock.hentHvisEksisterer(any()) } returns
                TjenestepensjonRefusjonskravVurdering(
                    true,
                    LocalDate.parse("2025-01-04"),
                    LocalDate.parse("2025-01-12"),
                    "bla bla"
                )
        every { samordningAndreStatligeYtelserRepositoryMock.hentHvisEksisterer(any()) } returns null
        every { samordningArbeidsgiverRepositoryMock.hentHvisEksisterer(any()) } returns null


        val avventUtbetaling = service.finnEventuellAvventUtbetaling(
            førsteVedtak = vedtak,
            behandling = behandling,
            tilkjentYtelseHelePerioden = Periode(LocalDate.parse("2025-01-01"), LocalDate.parse("2025-01-31"))
        )

        assertNotNull(avventUtbetaling)
        assertThat(avventUtbetaling?.fom).isEqualTo(LocalDate.parse("2025-01-04"))
        assertThat(avventUtbetaling?.tom).isEqualTo(LocalDate.parse("2025-01-12"))
        assertThat(avventUtbetaling?.overføres).isEqualTo(LocalDate.parse("2025-01-15").plusDays(42))
        assertThat(avventUtbetaling?.årsak).isEqualTo(AvventÅrsak.AVVENT_REFUSJONSKRAV)
        assertThat(avventUtbetaling?.feilregistrering).isFalse()
    }

    @Test
    fun `Tjenestepensjon refusjonskrav utenover vedtaksdato fører til at vedtaksdato - 1 blir satt som tom`() {

        every { vedtakServiceMock.hentVedtak(any()) } returns vedtak

        every { refusjonkravRepositoryMock.hentHvisEksisterer(any()) } returns null
        every { tjenestepensjonRefusjonsKravVurderingRepositoryMock.hentHvisEksisterer(any()) } returns
                TjenestepensjonRefusjonskravVurdering(
                    true,
                    LocalDate.parse("2025-01-04"),
                    LocalDate.parse("2025-01-20"),
                    "bla bla"
                )
        every { samordningAndreStatligeYtelserRepositoryMock.hentHvisEksisterer(any()) } returns null
        every { samordningArbeidsgiverRepositoryMock.hentHvisEksisterer(any()) } returns null


        val avventUtbetaling = service.finnEventuellAvventUtbetaling(
            førsteVedtak = vedtak,
            behandling = behandling,
            tilkjentYtelseHelePerioden = Periode(LocalDate.parse("2025-01-01"), LocalDate.parse("2025-01-31"))
        )

        assertNotNull(avventUtbetaling)
        assertThat(avventUtbetaling?.fom).isEqualTo(LocalDate.parse("2025-01-04"))
        assertThat(avventUtbetaling?.tom).isEqualTo(LocalDate.parse("2025-01-14")) //første vedtaksdato - 1
        assertThat(avventUtbetaling?.overføres).isEqualTo(LocalDate.parse("2025-01-15").plusDays(42))
        assertThat(avventUtbetaling?.årsak).isEqualTo(AvventÅrsak.AVVENT_REFUSJONSKRAV)
        assertThat(avventUtbetaling?.feilregistrering).isFalse()
    }




    @Test
    fun `Samordning med andre statlige ytelser overlapper med tilkjent ytelse skal føre til avvent utbetaling`() {

        every { vedtakServiceMock.hentVedtak(any()) } returns vedtak

        every { refusjonkravRepositoryMock.hentHvisEksisterer(any()) } returns null
        every { tjenestepensjonRefusjonsKravVurderingRepositoryMock.hentHvisEksisterer(any()) } returns null
        every { samordningAndreStatligeYtelserRepositoryMock.hentHvisEksisterer(any()) } returns
                SamordningAndreStatligeYtelserGrunnlag(
                    vurdering = SamordningAndreStatligeYtelserVurdering(
                        begrunnelse = "bla bla",
                        vurdertAv = "noen",
                        vurderingPerioder = listOf(
                            SamordningAndreStatligeYtelserVurderingPeriode(
                                ytelse = AndreStatligeYtelser.TILTAKSPENGER,
                                periode = Periode(LocalDate.parse("2025-01-04"), LocalDate.parse("2025-01-12")),
                            )
                        ),
                    )
                )
        every { samordningArbeidsgiverRepositoryMock.hentHvisEksisterer(any()) } returns null


        val avventUtbetaling = service.finnEventuellAvventUtbetaling(
            førsteVedtak = vedtak,
            behandling = behandling,
            tilkjentYtelseHelePerioden = Periode(LocalDate.parse("2025-01-01"), LocalDate.parse("2025-01-31"))
        )

        assertNotNull(avventUtbetaling)
        assertThat(avventUtbetaling?.fom).isEqualTo(LocalDate.parse("2025-01-04"))
        assertThat(avventUtbetaling?.tom).isEqualTo(LocalDate.parse("2025-01-12"))
        assertThat(avventUtbetaling?.overføres).isEqualTo(vedtak.vedtakstidspunkt.toLocalDate().plusDays(42))
        assertThat(avventUtbetaling?.årsak).isEqualTo(AvventÅrsak.AVVENT_AVREGNING)
        assertThat(avventUtbetaling?.feilregistrering).isFalse()
    }

    @Test
    fun `Samordning med sluttpakke fra arbeidsgiver overlapper med tilkjent ytelse skal føre til avvent utbetaling`() {
        every { vedtakServiceMock.hentVedtak(any()) } returns vedtak

        every { refusjonkravRepositoryMock.hentHvisEksisterer(any()) } returns null
        every { tjenestepensjonRefusjonsKravVurderingRepositoryMock.hentHvisEksisterer(any()) } returns null
        every { samordningAndreStatligeYtelserRepositoryMock.hentHvisEksisterer(any()) } returns null
        every { samordningArbeidsgiverRepositoryMock.hentHvisEksisterer(any()) } returns
                SamordningArbeidsgiverGrunnlag(
                    vurdering = SamordningArbeidsgiverVurdering(
                        "Har fått sluttpakke",
                        listOf(Periode(LocalDate.of(2025, 1, 4), LocalDate.of(2025, 1, 12))),
                         vurdertAv = "ident"
                    )
                )


        val avventUtbetaling = service.finnEventuellAvventUtbetaling(
            førsteVedtak = vedtak,
            behandling = behandling,
            tilkjentYtelseHelePerioden = Periode(LocalDate.parse("2025-01-01"), LocalDate.parse("2025-01-31"))
        )

        assertNotNull(avventUtbetaling)
        assertThat(avventUtbetaling?.fom).isEqualTo(LocalDate.parse("2025-01-04"))
        assertThat(avventUtbetaling?.tom).isEqualTo(LocalDate.parse("2025-01-12"))
        assertThat(avventUtbetaling?.overføres).isEqualTo(vedtak.vedtakstidspunkt.toLocalDate().plusDays(42))
        assertThat(avventUtbetaling?.årsak).isEqualTo(AvventÅrsak.AVVENT_AVREGNING)
        assertThat(avventUtbetaling?.feilregistrering).isFalse()
    }


}