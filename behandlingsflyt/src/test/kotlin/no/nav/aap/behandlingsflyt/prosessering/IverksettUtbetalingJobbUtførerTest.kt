package no.nav.aap.behandlingsflyt.prosessering

import io.mockk.every
import io.mockk.mockk
import no.nav.aap.behandlingsflyt.behandling.utbetaling.UtbetalingGateway
import no.nav.aap.behandlingsflyt.behandling.utbetaling.UtbetalingService
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingMedVedtak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingService
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.motor.JobbInput
import no.nav.aap.utbetal.tilkjentytelse.TilkjentYtelseDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime

class IverksettUtbetalingJobbUtførerTest {

    private val sakId = SakId(123L)
    private val behandlingId_1 = BehandlingId(1L)
    private val behandlingId_2 = BehandlingId(2L)

    val behandlingService = mockk<BehandlingService>()
    val utbetalingService = mockk<UtbetalingService>()
    val utbetalingGateway = mockk<UtbetalingGateway>()
    val iverksettUtbetalingJobbUtfører =
        IverksettUtbetalingJobbUtfører(utbetalingGateway, utbetalingService, behandlingService)
    val jobbInput = JobbInput(IverksettUtbetalingJobbUtfører).forSak(sakId.toLong())

    @Test
    fun `skal feile dersom det ikke finnes fattet behandling`() {
        every { behandlingService.finnBehandlingMedSisteFattedeVedtak(sakId) } returns null
        assertThrows<IllegalStateException> {
            iverksettUtbetalingJobbUtfører.utfør(jobbInput.medPayload(behandlingId_1))
        }
    }

    @Test
    fun `skal iverksette nyeste fattede vedtak til utbetaling`() {
        every { behandlingService.finnBehandlingMedSisteFattedeVedtak(sakId) } returns opprettBehandlingMedVedtak(
            behandlingId_1
        )
        var capturedBehandlingId: BehandlingId? = null
        every {
            utbetalingService.lagTilkjentYtelseForUtbetaling(
                sakId, any(), any()
            )
        } answers {
            capturedBehandlingId = secondArg()
            mockk<TilkjentYtelseDto>()
        }
        every { utbetalingGateway.utbetal(any()) } answers { }

        iverksettUtbetalingJobbUtfører.utfør(jobbInput.medPayload(behandlingId_1))

        assertThat(capturedBehandlingId).isEqualTo(behandlingId_1)
    }

    @Test
    fun `skal iverksette nyeste fattede vedtak fra en annen behandling`() {
        every { behandlingService.finnBehandlingMedSisteFattedeVedtak(sakId) } returns opprettBehandlingMedVedtak(
            behandlingId_2
        )
        var capturedBehandlingId: BehandlingId? = null
        every {
            utbetalingService.lagTilkjentYtelseForUtbetaling(
                sakId, any(), any()
            )
        } answers {
            capturedBehandlingId = secondArg()
            mockk<TilkjentYtelseDto>()
        }
        every { utbetalingGateway.utbetal(any()) } answers { }

        iverksettUtbetalingJobbUtfører.utfør(jobbInput.medPayload(behandlingId_1))

        assertThat(capturedBehandlingId).isEqualTo(behandlingId_2)
    }

    private fun opprettBehandlingMedVedtak(behandlingId: BehandlingId): BehandlingMedVedtak = BehandlingMedVedtak(
        saksnummer = Saksnummer("1234"),
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
}