package no.nav.aap.behandlingsflyt.behandling.utbetaling

import io.mockk.every
import io.mockk.mockk
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.refusjonkrav.RefusjonkravRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.refusjonkrav.RefusjonkravVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.samordning.refusjonskrav.TjenestepensjonRefusjonsKravVurderingRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.samordning.refusjonskrav.TjenestepensjonRefusjonskravVurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.utbetal.kodeverk.AvventÅrsak
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.*
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.test.Test

class AvventUtbetalingServiceTest {

    @Test
    fun `Ingen refusjonskrav skal føre til ingen avvent utbetaling`() {
        val refusjonkravRepositoryMock = mockk<RefusjonkravRepository>()
        val tjenestepensjonRefusjonsKravVurderingRepositoryMock = mockk<TjenestepensjonRefusjonsKravVurderingRepository>()
        every { refusjonkravRepositoryMock.hentHvisEksisterer(any()) } returns null
        every { tjenestepensjonRefusjonsKravVurderingRepositoryMock.hentHvisEksisterer(any()) } returns null
        val service = AvventUtbetalingService(refusjonkravRepositoryMock, tjenestepensjonRefusjonsKravVurderingRepositoryMock)

        val avventUtbetaling = service.finnEventuellAvventUtbetaling(
            behandlingId =  BehandlingId(123L),
            vedtakstidspunkt = LocalDateTime.parse("2025-01-15T00:00:00.000"),
            tilkjentYtelseHelePerioden = Periode(LocalDate.parse("2025-01-01"), LocalDate.parse("2025-01-31"))
        )

        assertNull(avventUtbetaling)
    }

    @Test
    fun `Refusjonskrav utenfor ytelsesperiodenmskal føre til ingen avvent utbetaling`() {
        val refusjonkravRepositoryMock = mockk<RefusjonkravRepository>()
        val tjenestepensjonRefusjonsKravVurderingRepositoryMock = mockk<TjenestepensjonRefusjonsKravVurderingRepository>()
        every { refusjonkravRepositoryMock.hentHvisEksisterer(any()) } returns RefusjonkravVurdering(true, LocalDate.parse("2024-01-01"), LocalDate.parse("2024-01-31"))
        every { tjenestepensjonRefusjonsKravVurderingRepositoryMock.hentHvisEksisterer(any()) } returns null
        val service = AvventUtbetalingService(refusjonkravRepositoryMock, tjenestepensjonRefusjonsKravVurderingRepositoryMock)

        val avventUtbetaling = service.finnEventuellAvventUtbetaling(
            behandlingId =  BehandlingId(123L),
            vedtakstidspunkt = LocalDateTime.parse("2025-01-15T00:00:00.000"),
            tilkjentYtelseHelePerioden = Periode(LocalDate.parse("2025-01-01"), LocalDate.parse("2025-01-31"))
        )

        assertNull(avventUtbetaling)
    }

    @Test
    fun `Refusjonskrav med åpen tom overlapper med tilkjent ytelse skal føre til avvent utbetaling`() {
        val refusjonkravRepositoryMock = mockk<RefusjonkravRepository>()
        val tjenestepensjonRefusjonsKravVurderingRepositoryMock = mockk<TjenestepensjonRefusjonsKravVurderingRepository>()
        every { refusjonkravRepositoryMock.hentHvisEksisterer(any()) } returns
                RefusjonkravVurdering(true, LocalDate.parse("2025-01-10"), null)
        every { tjenestepensjonRefusjonsKravVurderingRepositoryMock.hentHvisEksisterer(any()) } returns null
        val service = AvventUtbetalingService(refusjonkravRepositoryMock, tjenestepensjonRefusjonsKravVurderingRepositoryMock)

        val avventUtbetaling = service.finnEventuellAvventUtbetaling(
            behandlingId =  BehandlingId(123L),
            vedtakstidspunkt = LocalDateTime.parse("2025-01-15T00:00:00.000"),
            tilkjentYtelseHelePerioden = Periode(LocalDate.parse("2025-01-01"), LocalDate.parse("2025-01-31"))
        )

        assertNotNull(avventUtbetaling)
        assertThat(avventUtbetaling?.fom).isEqualTo(LocalDate.parse("2025-01-10"))
        assertThat(avventUtbetaling?.tom).isEqualTo(LocalDate.parse("2025-01-14"))
        assertThat(avventUtbetaling?.overføres).isEqualTo(LocalDate.parse("2025-01-14").plusDays(21))
        assertThat(avventUtbetaling?.årsak).isEqualTo(AvventÅrsak.AVVENT_REFUSJONSKRAV)
        assertThat(avventUtbetaling?.feilregistrering).isFalse()
    }

    @Test
    fun `Tjenestepensjon refusjonskrav overlapper med tilkjent ytelse skal føre til avvent utbetaling`() {
        val refusjonkravRepositoryMock = mockk<RefusjonkravRepository>()
        val tjenestepensjonRefusjonsKravVurderingRepositoryMock = mockk<TjenestepensjonRefusjonsKravVurderingRepository>()
        every { refusjonkravRepositoryMock.hentHvisEksisterer(any()) } returns null
        every { tjenestepensjonRefusjonsKravVurderingRepositoryMock.hentHvisEksisterer(any()) } returns
                TjenestepensjonRefusjonskravVurdering(true, LocalDate.parse("2025-01-04"), LocalDate.parse("2025-01-12"), "bla bla")
        val service = AvventUtbetalingService(refusjonkravRepositoryMock, tjenestepensjonRefusjonsKravVurderingRepositoryMock)

        val avventUtbetaling = service.finnEventuellAvventUtbetaling(
            behandlingId =  BehandlingId(123L),
            vedtakstidspunkt = LocalDateTime.parse("2025-01-15T00:00:00.000"),
            tilkjentYtelseHelePerioden = Periode(LocalDate.parse("2025-01-01"), LocalDate.parse("2025-01-31"))
        )

        assertNotNull(avventUtbetaling)
        assertThat(avventUtbetaling?.fom).isEqualTo(LocalDate.parse("2025-01-04"))
        assertThat(avventUtbetaling?.tom).isEqualTo(LocalDate.parse("2025-01-12"))
        assertThat(avventUtbetaling?.overføres).isEqualTo(LocalDate.parse("2025-01-12").plusDays(42))
        assertThat(avventUtbetaling?.årsak).isEqualTo(AvventÅrsak.AVVENT_REFUSJONSKRAV)
        assertThat(avventUtbetaling?.feilregistrering).isFalse()
    }

}