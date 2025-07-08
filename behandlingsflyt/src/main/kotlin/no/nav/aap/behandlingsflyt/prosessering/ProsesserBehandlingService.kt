package no.nav.aap.behandlingsflyt.prosessering

import no.nav.aap.behandlingsflyt.faktagrunnlag.SakOgBehandlingService
import no.nav.aap.behandlingsflyt.flyt.FlytOrkestrator
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import org.slf4j.LoggerFactory

class ProsesserBehandlingService(
    private val flytJobbRepository: FlytJobbRepository,
    private val behandlingRepository: BehandlingRepository,
    private val atomærFlytOrkestrator: FlytOrkestrator,
) {
    constructor(repositoryProvider: RepositoryProvider) : this(
        flytJobbRepository = repositoryProvider.provide(),
        behandlingRepository = repositoryProvider.provide(),
        atomærFlytOrkestrator = FlytOrkestrator(
            repositoryProvider,
            stoppNårStatus = setOf(Status.IVERKSETTES, Status.AVSLUTTET),
            markSavepointAt = emptySet(),
        )
    )

    private val log = LoggerFactory.getLogger(javaClass)

    fun triggProsesserBehandling(
        opprettetBehandling: SakOgBehandlingService.OpprettetBehandling,
        parameters: List<Pair<String, String>> = emptyList()
    ) {

        when (opprettetBehandling) {
            is SakOgBehandlingService.Ordinær -> triggProsesserBehandling(
                opprettetBehandling.åpenBehandling,
                parameters
            )

            is SakOgBehandlingService.MåBehandlesAtomært -> kjørAtomærBehandling(opprettetBehandling)
        }
    }

    fun triggProsesserBehandling(behandling: Behandling, parameters: List<Pair<String, String>>) {
        triggProsesserBehandling(behandling.sakId, behandling.id, parameters)
    }

    fun triggProsesserBehandling(
        sakId: SakId,
        behandlingId: BehandlingId,
        parameters: List<Pair<String, String>> = emptyList()
    ) {
        val eksisterendeJobber = flytJobbRepository
            .hentJobberForBehandling(behandlingId.toLong())
            .filter { it.type() == ProsesserBehandlingJobbUtfører.type }

        if (eksisterendeJobber.isNotEmpty()) {
            log.info("Har planlagt eksisterende kjøring, planlegger ikke en ny. {}", eksisterendeJobber)
            return
        }

        val jobbInput = JobbInput(jobb = ProsesserBehandlingJobbUtfører).forBehandling(
            sakId.toLong(),
            behandlingId.toLong()
        ).medCallId()

        parameters.forEach {
            jobbInput.medParameter(it.first, it.second)
        }

        flytJobbRepository.leggTil(jobbInput)
    }

    private fun kjørAtomærBehandling(opprettetBehandling: SakOgBehandlingService.MåBehandlesAtomært) {
        val behandling = opprettetBehandling.nyBehandling

        val kontekst = atomærFlytOrkestrator.opprettKontekst(behandling.sakId, behandling.id)
        atomærFlytOrkestrator.forberedOgProsesserBehandling(kontekst)

        behandlingRepository.hent(behandling.id).also {
            check(it.status().erAvsluttet()) {
                "Behandling ${behandling.referanse} må kjøres atomært, men er ikke i avsluttet tilstand"
            }
        }

        // TODO: Hva trenger vi denne til? 
        // Forårsaker at iverksett_vedtak kjøres på nytt og kræsjer fordi den prøver å lagre ned vedtaket på nytt
        // Kommenterer ut enn så lenge
        //triggProsesserBehandling(behandling.sakId, behandling.id)
        log.info("Prosessererte behandling ${behandling.referanse} atomært")

        val åpenBehandling = opprettetBehandling.åpenBehandling
        if (åpenBehandling != null) {
            val kontekst = atomærFlytOrkestrator.opprettKontekst(åpenBehandling.sakId, åpenBehandling.id)
            atomærFlytOrkestrator.tilbakeførEtterAtomærBehandling(kontekst)
        } else {
            // Må opprette behandling dersom informasjonskravene har endret seg.
            // Den åpne behandlingen vil sjekke informasjonskrav uansett?
        }
    }
}