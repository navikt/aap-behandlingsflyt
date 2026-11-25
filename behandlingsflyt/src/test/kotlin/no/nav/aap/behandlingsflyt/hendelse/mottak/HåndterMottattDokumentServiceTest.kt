package no.nav.aap.behandlingsflyt.hendelse.mottak

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.ManuellRevurderingV0
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.NyÅrsakTilBehandlingV0
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingService
import no.nav.aap.behandlingsflyt.sakogbehandling.lås.BehandlingSkrivelås
import no.nav.aap.behandlingsflyt.sakogbehandling.lås.TaSkriveLåsRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.komponenter.verdityper.Bruker
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.time.LocalDateTime

class HåndterMottattDokumentServiceTest {
    private val sakId = SakId(1)
    private val saksnummer = Saksnummer("123")
    private val behandlingId = BehandlingId(2)
    private val behandlingsreferanse = BehandlingReferanse()
    private val referanse = InnsendingReferanse(
        InnsendingReferanse.Type.SAKSBEHANDLER_KELVIN_REFERANSE,
        "innsending"
    )
    private val behandlingService = mockk<BehandlingService>(relaxed = true)
    private val behandlingRepository = mockk<BehandlingRepository>()
    private val låsRepository = mockk<TaSkriveLåsRepository>(relaxed = true)
    private val sakService = mockk<SakService>(relaxed = true)
    private val behandling = mockk<Behandling> {
        every { id } returns behandlingId
    }
    private val service = HåndterMottattDokumentService(
        sakService = sakService,
        behandlingService = behandlingService,
        låsRepository = låsRepository,
        prosesserBehandling = mockk(relaxed = true),
        mottaDokumentService = mockk(relaxed = true),
        behandlingRepository = behandlingRepository,
        flytJobbRepository = mockk(relaxed = true),
        unleashGateway = mockk(relaxed = true)
    )

    @ParameterizedTest
    @CsvSource("ny,legacy,ny", "null,legacy,legacy", "null,null,null", nullValues = ["null"])
    fun `opprettelse bruker felt opprettetAv fra innsending med kun manuell revurdering som fallback`(
        opprettetAv: String?,
        legacy: String?,
        forventet: String?
    ) {
        every { sakService.hent(sakId) } returns mockk<Sak> {
            every { saksnummer } returns this@HåndterMottattDokumentServiceTest.saksnummer
        }
        every { behandlingService.finnEllerOpprettBehandling(saksnummer, any()) } returns
                BehandlingService.Ordinær(behandling)

        service.håndterMottatteDokumenter(
            sakId,
            referanse,
            LocalDateTime.of(2026, 1, 1, 12, 0),
            InnsendingType.MANUELL_REVURDERING,
            ManuellRevurderingV0(
                listOf(Vurderingsbehov.SYKDOM_ARBEVNE_BEHOV_FOR_BISTAND),
                "Revurdering",
                legacy
            ),
            opprettetAv = opprettetAv
        )

        verify(exactly = 1) {
            behandlingService.finnEllerOpprettBehandling(saksnummer, match {
                it.opprettetAv == forventet?.let(::Bruker)
            })
        }
    }

    @ParameterizedTest
    @CsvSource("ny", "null", nullValues = ["null"])
    fun `endring av eksisterende behandling bevarer felt opprettetAv`(opprettetAv: String?) {
        every { behandlingRepository.hent(behandlingsreferanse) } returns behandling
        every { låsRepository.withLåstBehandling(behandlingId, any()) } answers {
            secondArg<(BehandlingSkrivelås) -> Unit>().invoke(mockk())
        }

        service.oppdaterÅrsakerTilBehandlingPåEksisterendeÅpenBehandling(
            sakId,
            behandlingsreferanse,
            InnsendingType.NY_ÅRSAK_TIL_BEHANDLING,
            NyÅrsakTilBehandlingV0(
                årsakerTilBehandling = listOf(Vurderingsbehov.SYKDOM_ARBEVNE_BEHOV_FOR_BISTAND),
                behandlingReferanse = behandlingsreferanse.referanse.toString(),
                reserverTilBruker = null,
                beskrivelse = "Ny årsak"
            ),
            referanse,
            opprettetAv = opprettetAv
        )

        verify(exactly = 1) {
            behandlingService.oppdaterVurderingsbehovOgÅrsak(behandling, match {
                it.opprettetAv == opprettetAv?.let(::Bruker)
            })
        }
    }
}
