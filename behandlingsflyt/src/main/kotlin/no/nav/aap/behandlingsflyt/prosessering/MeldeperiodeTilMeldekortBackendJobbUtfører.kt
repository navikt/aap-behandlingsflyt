package no.nav.aap.behandlingsflyt.prosessering

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.meldeperiode.MeldeperiodeRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.motor.Jobb
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import org.slf4j.LoggerFactory

class MeldeperiodeTilMeldekortBackendJobbUtfører(
    private val sakService: SakService,
    private val meldeperiodeRepository: MeldeperiodeRepository,
): JobbUtfører {
    private val log = LoggerFactory.getLogger(javaClass)!!

    override fun utfør(input: JobbInput) {
        val sakId = SakId(input.sakId())
        val behandlingId = BehandlingId(input.behandlingId())

        val sak = sakService.hent(sakId)
        val meldeperioder = meldeperiodeRepository.hent(behandlingId)

        // TODO
        log.info("Skal pushe informasjon til meldekort-backend om meldeperioder")
    }

    companion object: Jobb {
        override fun beskrivelse(): String {
            return """
                Push informasjon til meldekort-backend slik at vi kan åpne for
                innsending av meldekort før vedtak er fattet.
                """.trimIndent()
        }

        override fun konstruer(connection: DBConnection): JobbUtfører {
            val repositoryProvider = RepositoryProvider(connection)

            return MeldeperiodeTilMeldekortBackendJobbUtfører(
                sakService = SakService(
                    sakRepository = repositoryProvider.provide(),
                ),
                meldeperiodeRepository = repositoryProvider.provide(),
            )
        }

        override fun navn(): String {
            return "MeldeperiodeTilMeldekortBackend"
        }

        override fun type(): String {
            return "flyt.meldeperiodeTilMeldekortBackend"
        }

        fun nyJobb(
            sakId: SakId,
            behandlingId: BehandlingId,
        ) = JobbInput(MeldeperiodeTilMeldekortBackendJobbUtfører)
            .apply {
                forBehandling(sakId.toLong(), behandlingId.toLong())
            }
    }
}