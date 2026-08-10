package no.nav.aap.behandlingsflyt.prosessering

import no.nav.aap.behandlingsflyt.behandling.journalføring.JournalføringService
import no.nav.aap.behandlingsflyt.behandling.vedtak.VedtakRepository
import no.nav.aap.behandlingsflyt.behandling.vilkår.innsikt.PdfGeneratorGateway
import no.nav.aap.behandlingsflyt.dokumentasjon.VedtakDokumentGenerator
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.motor.ProvidersJobbSpesifikasjon

class GenererVilkårsvurderingOppsummeringJobbUtfører(
    private val behandlingRepository: BehandlingRepository,
    private val sakRepository: SakRepository,
    private val vedtakRepository: VedtakRepository,
    private val vedtakDokumentGenerator: VedtakDokumentGenerator,
    private val pdfGeneratorGateway: PdfGeneratorGateway,
    private val journalføringService: JournalføringService,
) : JobbUtfører {

    override fun utfør(input: JobbInput) {
        val behandlingId = input.payload<BehandlingId>()
        val behandling = behandlingRepository.hent(behandlingId)
        val sak = sakRepository.hent(behandling.sakId)
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

        journalføringService.journalførVilkårsvurderingOppsummering(
            sak = sak,
            pdf = pdf
        )
    }

    companion object : ProvidersJobbSpesifikasjon {
        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider,
        ): JobbUtfører = GenererVilkårsvurderingOppsummeringJobbUtfører(
            behandlingRepository = repositoryProvider.provide(),
            sakRepository = repositoryProvider.provide(),
            vedtakRepository = repositoryProvider.provide(),
            vedtakDokumentGenerator = VedtakDokumentGenerator(repositoryProvider),
            pdfGeneratorGateway = gatewayProvider.provide(),
            journalføringService = JournalføringService(gatewayProvider),
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
