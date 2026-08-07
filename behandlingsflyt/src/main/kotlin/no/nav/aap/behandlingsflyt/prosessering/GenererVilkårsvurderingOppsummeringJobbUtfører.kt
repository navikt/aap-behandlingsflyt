package no.nav.aap.behandlingsflyt.prosessering

import no.nav.aap.behandlingsflyt.behandling.vedtak.VedtakRepository
import no.nav.aap.behandlingsflyt.behandling.vilkår.innsikt.PdfGeneratorGateway
import no.nav.aap.behandlingsflyt.dokumentasjon.VedtakDokumentGenerator
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.motor.ProvidersJobbSpesifikasjon
import java.io.File
import java.io.FileOutputStream

class GenererVilkårsvurderingOppsummeringJobbUtfører(
    private val behandlingRepository: BehandlingRepository,
    private val vedtakRepository: VedtakRepository,
    private val vedtakDokumentGenerator: VedtakDokumentGenerator,
    private val pdfGeneratorGateway: PdfGeneratorGateway,
) : JobbUtfører {

    override fun utfør(input: JobbInput) {
        val utskriftsmappe = lokalUtskriftsmappe() ?: return
        val behandlingId = input.payload<BehandlingId>()
        val behandling = behandlingRepository.hent(behandlingId)
        val vedtak = requireNotNull(vedtakRepository.hent(behandlingId)) {
            "Forventet å finne vedtak for behandling $behandlingId"
        }
        val dokument = vedtakDokumentGenerator.genererDokument(
            behandlingId = behandlingId,
            sakId = behandling.sakId,
            vedtakstidspunkt = vedtak.vedtakstidspunkt,
            forrigeBehandlingId = behandling.forrigeBehandlingId,
        )
        val pdf = pdfGeneratorGateway.genererVurderingerOppsummeringDokument(dokument)

        FileOutputStream(File(utskriftsmappe, "${behandlingId.id}.pdf")).use { it.write(pdf) }
    }

    private fun lokalUtskriftsmappe(): File? {
        val utskriftssti = System.getenv("AAP_VEDTAK_LOKAL_UTSKRIFT_STI")
            ?: System.getProperty("aap.vedtak.lokal.utskrift.sti")
            ?: return null
        return File(utskriftssti).also { it.mkdirs() }
    }

    companion object : ProvidersJobbSpesifikasjon {
        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider,
        ): JobbUtfører = GenererVilkårsvurderingOppsummeringJobbUtfører(
            behandlingRepository = repositoryProvider.provide(),
            vedtakRepository = repositoryProvider.provide(),
            vedtakDokumentGenerator = VedtakDokumentGenerator(repositoryProvider),
            pdfGeneratorGateway = gatewayProvider.provide(),
        )

        override val type = "flyt.GenererVilkårsvurderingOppsummering"
        override val navn = "Generer vilkårsvurderingsoppsummering"
        override val beskrivelse = "Genererer PDF med vilkårsvurderingene i et vedtak"

        fun nyJobb(behandlingId: BehandlingId, sakId: SakId) =
            JobbInput(this)
                .medPayload(behandlingId)
                .forSak(sakId.toLong())
    }
}
