package no.nav.aap.behandlingsflyt.hendelse.mottak

import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingService
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottaDokumentService
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.Melding
import no.nav.aap.behandlingsflyt.prosessering.ProsesserBehandlingService
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import org.slf4j.LoggerFactory

class HåndterDialogMeldingService(
    private val sakService: SakService,
    private val behandlingService: BehandlingService,
    private val prosesserBehandling: ProsesserBehandlingService,
    private val mottaDokumentService: MottaDokumentService,
) {
    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        sakService = SakService(repositoryProvider, gatewayProvider),
        behandlingService = BehandlingService(repositoryProvider, gatewayProvider),
        prosesserBehandling = ProsesserBehandlingService(repositoryProvider, gatewayProvider),
        mottaDokumentService = MottaDokumentService(repositoryProvider)
    )
    private val log = LoggerFactory.getLogger(javaClass)

    fun håndterMottattDialogMelding(
        sakId: SakId,
        referanse: InnsendingReferanse,
        brevkategori: InnsendingType,
        melding: Melding?,
    ) {
        val sak = sakService.hent(sakId)
        log.info("Håndterer dialogmelding for ${sak.id}")
        val vurderingsbehov = MottattHendelseUtleder.utledVurderingsbehov(brevkategori, melding)
        val sisteYtelsesBehandling = behandlingService.finnSisteYtelsesbehandlingFor(sak.id)

        if (sisteYtelsesBehandling != null) {
            mottaDokumentService.markerSomBehandlet(sakId, sisteYtelsesBehandling.id, referanse)
            log.info("Markerer dialogmelding som behandlet ${sisteYtelsesBehandling.id}")
            if (sisteYtelsesBehandling.status().erÅpen()) {
                prosesserBehandling.triggProsesserBehandling(
                    sisteYtelsesBehandling,
                    vurderingsbehov = vurderingsbehov.filter { it.type == Vurderingsbehov.MOTTATT_DIALOGMELDING }
                        .map { it.type }
                )
                log.info("Prosessert behandling etter mottatt dialogmelding ${sisteYtelsesBehandling.id}")
            }

        }
    }
}


