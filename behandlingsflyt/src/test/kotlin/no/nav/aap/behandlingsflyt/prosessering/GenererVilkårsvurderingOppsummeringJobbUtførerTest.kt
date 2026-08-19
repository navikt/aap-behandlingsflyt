package no.nav.aap.behandlingsflyt.prosessering

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.aap.behandlingsflyt.behandling.journalføring.JournalføringService
import no.nav.aap.behandlingsflyt.behandling.vedtak.Vedtak
import no.nav.aap.behandlingsflyt.behandling.vedtak.VedtakId
import no.nav.aap.behandlingsflyt.behandling.vedtak.VedtakRepository
import no.nav.aap.behandlingsflyt.behandling.vilkår.innsikt.PdfGeneratorGateway
import no.nav.aap.behandlingsflyt.behandling.vilkår.innsikt.PdfDokument
import no.nav.aap.behandlingsflyt.dokumentasjon.VedtakDokumentGenerator
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class GenererVilkårsvurderingOppsummeringJobbUtførerTest {
    private val behandlingRepository = mockk<BehandlingRepository>()
    private val sakRepository = mockk<SakRepository>()
    private val vedtakRepository = mockk<VedtakRepository>()
    private val vedtakDokumentGenerator = mockk<VedtakDokumentGenerator>()
    private val pdfGeneratorGateway = mockk<PdfGeneratorGateway>()
    private val journalføringService = mockk<JournalføringService>(relaxed = true)

    @Test
    fun `genererer og journalfører pdf fra lagret vedtak`() {
        val behandlingId = BehandlingId(1)
        val sakId = SakId(2)
        val forrigeBehandlingId = BehandlingId(3)
        val vedtakstidspunkt = LocalDateTime.of(2026, 8, 7, 12, 0)
        val behandling = mockk<Behandling>()
        val sak = mockk<Sak>()
        val vedtak = Vedtak(
            id = VedtakId(4),
            behandlingId = behandlingId,
            vedtakstidspunkt = vedtakstidspunkt,
            virkningstidspunkt = LocalDate.of(2026, 8, 1),
        )
        val dokument = mockk<PdfDokument>()
        val pdf = "%PDF-1.4 test".encodeToByteArray()
        every { behandling.sakId } returns sakId
        every { behandling.forrigeBehandlingId } returns forrigeBehandlingId
        every { behandlingRepository.hent(behandlingId) } returns behandling
        every { sakRepository.hent(sakId) } returns sak
        every { vedtakRepository.hent(behandlingId) } returns vedtak
        every {
            vedtakDokumentGenerator.genererDokument(
                behandlingId = behandlingId,
                sakId = sakId,
                vedtakstidspunkt = vedtakstidspunkt,
                forrigeBehandlingId = forrigeBehandlingId,
            )
        } returns dokument
        every { pdfGeneratorGateway.genererVurderingerOppsummeringPdfDokument(dokument) } returns pdf

        GenererVilkårsvurderingOppsummeringJobbUtfører(
            behandlingRepository = behandlingRepository,
            sakRepository = sakRepository,
            vedtakRepository = vedtakRepository,
            vedtakDokumentGenerator = vedtakDokumentGenerator,
            pdfGeneratorGateway = pdfGeneratorGateway,
            journalføringService = journalføringService,
        ).utfør(GenererVilkårsvurderingOppsummeringJobbUtfører.nyJobb(behandlingId, sakId))

        verify(exactly = 1) { pdfGeneratorGateway.genererVurderingerOppsummeringPdfDokument(dokument) }
        verify(exactly = 1) {
            journalføringService.journalførVilkårsvurderingOppsummering(
                sak = sak,
                pdf = pdf,
            )
        }
    }

}
