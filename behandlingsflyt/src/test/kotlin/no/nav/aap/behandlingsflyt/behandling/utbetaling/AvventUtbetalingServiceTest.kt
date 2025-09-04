package no.nav.aap.behandlingsflyt.behandling.utbetaling

import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
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
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.test.FakeUnleash
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.utbetal.kodeverk.AvventÅrsak
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.test.Test

@ExtendWith(MockKExtension::class)
@MockKExtension.CheckUnnecessaryStub
class AvventUtbetalingServiceTest {

    @Test
    fun `Ingen refusjonskrav skal føre til ingen avvent utbetaling`() {
        val refusjonkravRepositoryMock = mockk<RefusjonkravRepository>()
        val tjenestepensjonRefusjonsKravVurderingRepositoryMock =
            mockk<TjenestepensjonRefusjonsKravVurderingRepository>()
        val samordningAndreStatligeYtelserRepositoryMock = mockk<SamordningAndreStatligeYtelserRepository>()
        val samordningArbeidsgiverRepositoryMock = mockk<SamordningArbeidsgiverRepository>()
        every { refusjonkravRepositoryMock.hentHvisEksisterer(any()) } returns null
        every { tjenestepensjonRefusjonsKravVurderingRepositoryMock.hentHvisEksisterer(any()) } returns null
        every { samordningAndreStatligeYtelserRepositoryMock.hentHvisEksisterer(any()) } returns null
        every { samordningArbeidsgiverRepositoryMock.hentHvisEksisterer(any()) } returns null
        val service = AvventUtbetalingService(
            refusjonkravRepositoryMock,
            tjenestepensjonRefusjonsKravVurderingRepositoryMock,
            samordningAndreStatligeYtelserRepositoryMock,
            samordningArbeidsgiverRepositoryMock,
            FakeUnleash
        )

        val avventUtbetaling = service.finnEventuellAvventUtbetaling(
            behandlingId = BehandlingId(123L),
            førsteVedtaksdato = LocalDate.parse("2025-01-15"),
            tilkjentYtelseHelePerioden = Periode(LocalDate.parse("2025-01-01"), LocalDate.parse("2025-01-31"))
        )

        assertNull(avventUtbetaling)
    }

    @Test
    fun `Refusjonskrav utenfor ytelsesperioden skal føre til ingen avvent utbetaling`() {
        val refusjonkravRepositoryMock = mockk<RefusjonkravRepository>()
        val tjenestepensjonRefusjonsKravVurderingRepositoryMock =
            mockk<TjenestepensjonRefusjonsKravVurderingRepository>()
        val samordningAndreStatligeYtelserRepositoryMock = mockk<SamordningAndreStatligeYtelserRepository>()
        val samordningArbeidsgiverRepositoryMock = mockk<SamordningArbeidsgiverRepository>()
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
        val service = AvventUtbetalingService(
            refusjonkravRepositoryMock,
            tjenestepensjonRefusjonsKravVurderingRepositoryMock,
            samordningAndreStatligeYtelserRepositoryMock,
            samordningArbeidsgiverRepositoryMock,
            FakeUnleash
        )

        val avventUtbetaling = service.finnEventuellAvventUtbetaling(
            behandlingId = BehandlingId(123L),
            førsteVedtaksdato = LocalDate.parse("2025-01-15"),
            tilkjentYtelseHelePerioden = Periode(LocalDate.parse("2025-01-01"), LocalDate.parse("2025-01-31"))
        )

        assertNull(avventUtbetaling)
    }

    @Test
    fun `Refusjonskrav med åpen tom overlapper med tilkjent ytelse skal føre til avvent utbetaling`() {
        val refusjonkravRepositoryMock = mockk<RefusjonkravRepository>()
        val tjenestepensjonRefusjonsKravVurderingRepositoryMock =
            mockk<TjenestepensjonRefusjonsKravVurderingRepository>()
        val samordningAndreStatligeYtelserRepositoryMock = mockk<SamordningAndreStatligeYtelserRepository>()
        val samordningArbeidsgiverRepositoryMock = mockk<SamordningArbeidsgiverRepository>()
        every { refusjonkravRepositoryMock.hentHvisEksisterer(any()) } returns
                listOf(RefusjonkravVurdering(true, LocalDate.parse("2025-01-10"), null, "Nav Løten", "saksbehandler"))
        every { tjenestepensjonRefusjonsKravVurderingRepositoryMock.hentHvisEksisterer(any()) } returns null
        every { samordningAndreStatligeYtelserRepositoryMock.hentHvisEksisterer(any()) } returns null
        every { samordningArbeidsgiverRepositoryMock.hentHvisEksisterer(any()) } returns null
        val service = AvventUtbetalingService(
            refusjonkravRepositoryMock,
            tjenestepensjonRefusjonsKravVurderingRepositoryMock,
            samordningAndreStatligeYtelserRepositoryMock,
            samordningArbeidsgiverRepositoryMock,
            FakeUnleash
        )

        val avventUtbetaling = service.finnEventuellAvventUtbetaling(
            behandlingId = BehandlingId(123L),
            førsteVedtaksdato = LocalDate.parse("2025-01-15"),
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
        val refusjonkravRepositoryMock = mockk<RefusjonkravRepository>()
        val tjenestepensjonRefusjonsKravVurderingRepositoryMock =
            mockk<TjenestepensjonRefusjonsKravVurderingRepository>()
        val samordningAndreStatligeYtelserRepositoryMock = mockk<SamordningAndreStatligeYtelserRepository>()
        val samordningArbeidsgiverRepositoryMock = mockk<SamordningArbeidsgiverRepository>()
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
        val service = AvventUtbetalingService(
            refusjonkravRepositoryMock,
            tjenestepensjonRefusjonsKravVurderingRepositoryMock,
            samordningAndreStatligeYtelserRepositoryMock,
            samordningArbeidsgiverRepositoryMock,
            FakeUnleash
        )

        val avventUtbetaling = service.finnEventuellAvventUtbetaling(
            behandlingId = BehandlingId(123L),
            førsteVedtaksdato = LocalDate.parse("2025-01-15"),
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
    fun `Samordning med andre statlige ytelser overlapper med tilkjent ytelse skal føre til avvent utbetaling`() {
        val refusjonkravRepositoryMock = mockk<RefusjonkravRepository>()
        val tjenestepensjonRefusjonsKravVurderingRepositoryMock =
            mockk<TjenestepensjonRefusjonsKravVurderingRepository>()
        val samordningAndreStatligeYtelserRepositoryMock = mockk<SamordningAndreStatligeYtelserRepository>()
        val samordningArbeidsgiverRepositoryMock = mockk<SamordningArbeidsgiverRepository>()
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
        val service = AvventUtbetalingService(
            refusjonkravRepositoryMock,
            tjenestepensjonRefusjonsKravVurderingRepositoryMock,
            samordningAndreStatligeYtelserRepositoryMock,
            samordningArbeidsgiverRepositoryMock,
            FakeUnleash
        )

        val avventUtbetaling = service.finnEventuellAvventUtbetaling(
            behandlingId = BehandlingId(123L),
            førsteVedtaksdato = LocalDate.parse("2025-01-15"),
            tilkjentYtelseHelePerioden = Periode(LocalDate.parse("2025-01-01"), LocalDate.parse("2025-01-31"))
        )

        assertNotNull(avventUtbetaling)
        assertThat(avventUtbetaling?.fom).isEqualTo(LocalDate.parse("2025-01-04"))
        assertThat(avventUtbetaling?.tom).isEqualTo(LocalDate.parse("2025-01-12"))
        assertThat(avventUtbetaling?.overføres).isNull()
        assertThat(avventUtbetaling?.årsak).isEqualTo(AvventÅrsak.AVVENT_AVREGNING)
        assertThat(avventUtbetaling?.feilregistrering).isFalse()
    }

    @Test
    fun `Samordning med sluttpakke fra arbeidsgiver overlapper med tilkjent ytelse skal føre til avvent utbetaling`() {
        val refusjonkravRepositoryMock = mockk<RefusjonkravRepository>()
        val tjenestepensjonRefusjonsKravVurderingRepositoryMock =
            mockk<TjenestepensjonRefusjonsKravVurderingRepository>()
        val samordningAndreStatligeYtelserRepositoryMock = mockk<SamordningAndreStatligeYtelserRepository>()
        val samordningArbeidsgiverRepositoryMock = mockk<SamordningArbeidsgiverRepository>()
        every { refusjonkravRepositoryMock.hentHvisEksisterer(any()) } returns null
        every { tjenestepensjonRefusjonsKravVurderingRepositoryMock.hentHvisEksisterer(any()) } returns null
        every { samordningAndreStatligeYtelserRepositoryMock.hentHvisEksisterer(any()) } returns null
        every { samordningArbeidsgiverRepositoryMock.hentHvisEksisterer(any()) } returns
                SamordningArbeidsgiverGrunnlag(
                    vurdering = SamordningArbeidsgiverVurdering(
                        "Har fått sluttpakke",
                        LocalDate.of(2025, 1, 4), LocalDate.of(2025, 1, 12), vurdertAv = "ident"
                    )
                )
        val service = AvventUtbetalingService(
            refusjonkravRepositoryMock,
            tjenestepensjonRefusjonsKravVurderingRepositoryMock,
            samordningAndreStatligeYtelserRepositoryMock,
            samordningArbeidsgiverRepositoryMock,
            FakeUnleash
        )

        val avventUtbetaling = service.finnEventuellAvventUtbetaling(
            behandlingId = BehandlingId(123L),
            førsteVedtaksdato = LocalDate.parse("2025-01-15"),
            tilkjentYtelseHelePerioden = Periode(LocalDate.parse("2025-01-01"), LocalDate.parse("2025-01-31"))
        )

        assertNotNull(avventUtbetaling)
        assertThat(avventUtbetaling?.fom).isEqualTo(LocalDate.parse("2025-01-04"))
        assertThat(avventUtbetaling?.tom).isEqualTo(LocalDate.parse("2025-01-12"))
        assertThat(avventUtbetaling?.overføres).isNull()
        assertThat(avventUtbetaling?.årsak).isEqualTo(AvventÅrsak.AVVENT_AVREGNING)
        assertThat(avventUtbetaling?.feilregistrering).isFalse()
    }


}