package no.nav.aap.behandlingsflyt.prosessering

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.aap.behandlingsflyt.behandling.vedtak.Vedtak
import no.nav.aap.behandlingsflyt.behandling.vedtak.VedtakId
import no.nav.aap.behandlingsflyt.behandling.vedtak.VedtakRepository
import no.nav.aap.behandlingsflyt.behandling.vilkår.innsikt.Dokument
import no.nav.aap.behandlingsflyt.behandling.vilkår.innsikt.PdfGeneratorGateway
import no.nav.aap.behandlingsflyt.dokumentasjon.VedtakDokumentGenerator
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.time.LocalDate
import java.time.LocalDateTime

class GenererVilkårsvurderingOppsummeringJobbUtførerTest {
    private val behandlingRepository = mockk<BehandlingRepository>()
    private val vedtakRepository = mockk<VedtakRepository>()
    private val vedtakDokumentGenerator = mockk<VedtakDokumentGenerator>()
    private val pdfGeneratorGateway = mockk<PdfGeneratorGateway>()

    @TempDir
    lateinit var utskriftsmappe: Path

    @AfterEach
    fun tearDown() {
        System.clearProperty("aap.vedtak.lokal.utskrift.sti")
    }

    @Test
    fun `genererer pdf fra lagret vedtak`() {
        System.setProperty("aap.vedtak.lokal.utskrift.sti", utskriftsmappe.toString())
        val behandlingId = BehandlingId(1)
        val sakId = SakId(2)
        val forrigeBehandlingId = BehandlingId(3)
        val vedtakstidspunkt = LocalDateTime.of(2026, 8, 7, 12, 0)
        val behandling = mockk<Behandling>()
        val vedtak = Vedtak(
            id = VedtakId(4),
            behandlingId = behandlingId,
            vedtakstidspunkt = vedtakstidspunkt,
            virkningstidspunkt = LocalDate.of(2026, 8, 1),
        )
        val dokument = mockk<Dokument>()
        val pdf = "%PDF-1.4 test".encodeToByteArray()
        every { behandling.sakId } returns sakId
        every { behandling.forrigeBehandlingId } returns forrigeBehandlingId
        every { behandlingRepository.hent(behandlingId) } returns behandling
        every { vedtakRepository.hent(behandlingId) } returns vedtak
        every {
            vedtakDokumentGenerator.genererDokument(
                behandlingId = behandlingId,
                sakId = sakId,
                vedtakstidspunkt = vedtakstidspunkt,
                forrigeBehandlingId = forrigeBehandlingId,
            )
        } returns dokument
        every { pdfGeneratorGateway.genererVurderingerOppsummeringDokument(dokument) } returns pdf

        GenererVilkårsvurderingOppsummeringJobbUtfører(
            behandlingRepository = behandlingRepository,
            vedtakRepository = vedtakRepository,
            vedtakDokumentGenerator = vedtakDokumentGenerator,
            pdfGeneratorGateway = pdfGeneratorGateway,
        ).utfør(GenererVilkårsvurderingOppsummeringJobbUtfører.nyJobb(behandlingId, sakId))

        assertThat(utskriftsmappe.resolve("1.pdf")).hasBinaryContent(pdf)
        verify(exactly = 1) { pdfGeneratorGateway.genererVurderingerOppsummeringDokument(dokument) }
    }

}
