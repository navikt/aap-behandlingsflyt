package no.nav.aap.behandlingsflyt.prosessering

import no.nav.aap.behandlingsflyt.flyt.FlytOrkestrator
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.lås.TaSkriveLåsRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.motor.ProviderJobbSpesifikasjon
import org.slf4j.LoggerFactory


class ProsesserBehandlingJobbUtfører(
    private val låsRepository: TaSkriveLåsRepository,
    private val kontroller: FlytOrkestrator
) : JobbUtfører {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun utfør(input: JobbInput) {
        val sakId = SakId(input.sakId())
        val behandlingId = BehandlingId(input.behandlingId())
        val skrivelås = låsRepository.lås(sakId, behandlingId)

        val kontekst = kontroller.opprettKontekst(sakId, behandlingId)

        kontroller.forberedOgProsesserBehandling(kontekst)

        log.info("Prosesserer behandling for jobb ${input.type()} med behandlingId ${behandlingId}")

        låsRepository.verifiserSkrivelås(skrivelås)
    }

    companion object : ProviderJobbSpesifikasjon {
        override fun konstruer(repositoryProvider: RepositoryProvider): JobbUtfører {
            return ProsesserBehandlingJobbUtfører(
                låsRepository = repositoryProvider.provide(),
                kontroller = FlytOrkestrator(repositoryProvider)
            )
        }

        override val type = "flyt.prosesserBehandling"
        override val navn = "Prosesser behandling"
        override val beskrivelse = "Ansvarlig for å drive prosessen på en gitt behandling"

        fun FlytJobbRepository.skjedulerProsesserBehandling(behandling: Behandling) {
            skjedulerProsesserBehandling(behandling.sakId, behandling.id)
        }

        fun FlytJobbRepository.skjedulerProsesserBehandling(sakId: SakId, behandlingId: BehandlingId) {
            val jobberPåBehandling = hentJobberForBehandling(behandlingId.toLong())

            if (jobberPåBehandling.none { it.type() == type }) {
                leggTil(
                    JobbInput(ProsesserBehandlingJobbUtfører).forBehandling(
                        sakID = sakId.toLong(),
                        behandlingId = behandlingId.toLong()
                    )
                )
            }
        }
    }
}
