package no.nav.aap.behandlingsflyt.hendelse.mottak

import no.nav.aap.behandlingsflyt.behandling.søknad.TrukketSøknadService
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingReferanse
import no.nav.aap.behandlingsflyt.prosessering.OppdagEndretInformasjonskravJobbUtfører
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottaDokumentService
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput

class HåndterSykepengevedtakService(
    private val behandlingService: BehandlingService,
    private val trukketSøknadService: TrukketSøknadService,
    private val flytJobbRepository: FlytJobbRepository,
    private val mottaDokumentService: MottaDokumentService,
) {

    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        behandlingService = BehandlingService(repositoryProvider, gatewayProvider),
        trukketSøknadService = TrukketSøknadService(repositoryProvider),
        flytJobbRepository = repositoryProvider.provide(),
        mottaDokumentService = MottaDokumentService(repositoryProvider),
    )

    fun håndterMottattSykepengevedtakHendelse(
        sakId: SakId,
        referanse: InnsendingReferanse,
    ) {
        val sisteYtelsesBehandling = behandlingService.finnSisteYtelsesbehandlingFor(sakId)
            ?: error("Finnes ingen ytelsesbehandling for sakId $sakId")
        if (!trukketSøknadService.søknadErTrukket(sisteYtelsesBehandling.id)) {
            flytJobbRepository.leggTil(
                JobbInput(jobb = OppdagEndretInformasjonskravJobbUtfører).forSak(sakId.toLong()).medCallId()
            )
        }
        mottaDokumentService.markerSomBehandlet(sakId, sisteYtelsesBehandling.id, referanse)
    }
}


