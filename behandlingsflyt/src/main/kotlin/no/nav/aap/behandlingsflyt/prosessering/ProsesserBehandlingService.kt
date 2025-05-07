package no.nav.aap.behandlingsflyt.prosessering

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import org.slf4j.LoggerFactory

class ProsesserBehandlingService(private val flytJobbRepository: FlytJobbRepository) {
    constructor(repositoryProvider: RepositoryProvider): this(
        flytJobbRepository = repositoryProvider.provide(),
    )

    private val log = LoggerFactory.getLogger(javaClass)

    fun triggProsesserBehandling(
        sakId: SakId,
        behandlingId: BehandlingId,
        parameters: List<Pair<String, String>> = emptyList()
    ) {
        val eksisterendeJobber = flytJobbRepository
            .hentJobberForBehandling(behandlingId.toLong())
            .filter { it.type() == ProsesserBehandlingJobbUtfører.type() }

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
}