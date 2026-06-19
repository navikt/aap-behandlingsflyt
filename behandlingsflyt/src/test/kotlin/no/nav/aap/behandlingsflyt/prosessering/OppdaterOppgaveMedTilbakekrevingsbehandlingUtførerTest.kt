package no.nav.aap.behandlingsflyt.prosessering

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.aap.behandlingsflyt.behandling.tilbakekrevingsbehandling.TilbakekrevingBehandlingsstatus
import no.nav.aap.behandlingsflyt.behandling.tilbakekrevingsbehandling.TilbakekrevingRepository
import no.nav.aap.behandlingsflyt.behandling.tilbakekrevingsbehandling.Tilbakekrevingsbehandling
import no.nav.aap.behandlingsflyt.hendelse.oppgavestyring.OppgavestyringGateway
import no.nav.aap.behandlingsflyt.hendelse.statistikk.StatistikkGateway
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.TilbakekrevingsbehandlingOppdatertHendelse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.TilbakekrevingVenteGrunn
import no.nav.aap.behandlingsflyt.kontrakt.oppgave.EnhetNrDto
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.motor.JobbInput
import no.nav.aap.oppgave.enhet.OppgaveEnhetResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.net.URI
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import kotlin.random.Random

class OppdaterOppgaveMedTilbakekrevingsbehandlingUtførerTest {

    private val tilbakekrevingRepository = mockk<TilbakekrevingRepository>()
    private val oppgavestyringGateway = mockk<OppgavestyringGateway>()

    private val sakId = SakId(Random.nextLong())
    private val behandlingId = UUID.randomUUID()

    private lateinit var utfører: OppdaterOppgaveMedTilbakekrevingsbehandlingUtfører

    @BeforeEach
    fun setUp() {
        val sak = stubSak()
        val sakRepository = mockk<SakRepository>()
        val statistikkGateway = mockk<StatistikkGateway>(relaxed = true)
        val unleashGateway = mockk<UnleashGateway>(relaxed = true)
        every { sakRepository.hent(sakId) } returns sak
        every { oppgavestyringGateway.finnNayEnhetForPerson(any(), any()) } returns EnhetNrDto("1234")
        every { oppgavestyringGateway.hentOppgaveEnhet(any()) } returns OppgaveEnhetResponse(emptyList())
        utfører = OppdaterOppgaveMedTilbakekrevingsbehandlingUtfører(
            oppgaveStyringGateway = oppgavestyringGateway,
            statistikkGateway = statistikkGateway,
            tilbakekrevingsbehandlingRepository = tilbakekrevingRepository,
            unleashGateway = unleashGateway,
            sakRepository = sakRepository,
        )
    }

    @Test
    fun `tilbakekrevingshendelse inkluderer ikke vente-info når behandlingen ikke er på vent`() {
        val behandlingIkkePåVent = stubTilbakekrevingsbehandling().copy(
            venteGrunn = null,
            gjenopptas = null,
        )
        val hendelse = slot<TilbakekrevingsbehandlingOppdatertHendelse>()
        every { oppgavestyringGateway.varsleTilbakekrevingHendelse(capture(hendelse)) } returns Unit
        every { tilbakekrevingRepository.hent(behandlingId) } returns behandlingIkkePåVent

        utfør()

        assertThat(hendelse.captured.gjenopptas).isNull()
        assertThat(hendelse.captured.venteGrunn).isNull()
    }

    @Test
    fun `tilbakekrevingshendelse inkluderer vente-info når behandlingen er på vent`() {
        val gjenopptas = LocalDate.now().plusMonths(2)
        val behandlingPåVent = stubTilbakekrevingsbehandling().copy(
            venteGrunn = TilbakekrevingVenteGrunn.AVVENTER_BRUKERUTTALELSE,
            gjenopptas = gjenopptas,
        )
        val hendelse = slot<TilbakekrevingsbehandlingOppdatertHendelse>()
        every { oppgavestyringGateway.varsleTilbakekrevingHendelse(capture(hendelse)) } returns Unit
        every { tilbakekrevingRepository.hent(behandlingId) } returns behandlingPåVent

        utfør()

        assertThat(hendelse.captured.gjenopptas).isEqualTo(gjenopptas)
        assertThat(hendelse.captured.venteGrunn).isEqualTo(TilbakekrevingVenteGrunn.AVVENTER_BRUKERUTTALELSE)
    }

    private fun utfør() {
        val jobbInput = JobbInput(OppdaterOppgaveMedTilbakekrevingsbehandlingUtfører)
            .medParameter("tilbakekrevingBehandlingId", behandlingId.toString())
            .forSak(sakId.toLong())
        utfører.utfør(jobbInput)
    }

    private fun stubTilbakekrevingsbehandling() : Tilbakekrevingsbehandling {
        return Tilbakekrevingsbehandling(
            tilbakekrevingBehandlingId = behandlingId,
            eksternFagsakId = "EKS$sakId",
            hendelseOpprettet = LocalDateTime.now(),
            eksternBehandlingId = UUID.randomUUID().toString(),
            sakOpprettet = LocalDateTime.now(),
            varselSendt = null,
            venteGrunn = null,
            gjenopptas = null,
            behandlingsstatus = TilbakekrevingBehandlingsstatus.TIL_BEHANDLING,
            totaltFeilutbetaltBeløp = Beløp(BigDecimal("10000")),
            saksbehandlingURL = URI.create("https://nav.no/behandling/$behandlingId"),
            fullstendigPeriode = Periode(LocalDate.now().minusYears(1), LocalDate.now()),
        )
    }

    private fun stubSak(): Sak = Sak(
        id = sakId,
        saksnummer = Saksnummer("SAK$sakId"),
        person = Person(
            id = PersonId(1L),
            referanse = UUID.randomUUID(),
            identer = listOf(Ident("12345678901", aktivIdent = true)),
        ),
        rettighetsperiode = Periode(LocalDate.now(), LocalDate.now().plusYears(1)),
    )
}
