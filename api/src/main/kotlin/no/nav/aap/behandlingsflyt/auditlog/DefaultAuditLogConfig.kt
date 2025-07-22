package no.nav.aap.behandlingsflyt.auditlog

import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.db.PersonRepository
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.tilgang.auditlog.AuditLogPathParamConfig
import no.nav.aap.tilgang.auditlog.PathBrukerIdentResolver
import org.slf4j.LoggerFactory
import java.util.*
import javax.sql.DataSource

class DefaultAuditLogConfig(
    private val repositoryRegistry: RepositoryRegistry,
) {
    private val auditLogger = LoggerFactory.getLogger("auditLogger")
    private val app = requiredConfigForKey("nais.app.name")

    fun fraBehandlingPathParam(pathParam: String, dataSource: DataSource) =
        AuditLogPathParamConfig(
            logger = auditLogger,
            app = app,
            brukerIdentResolver = PathBrukerIdentResolver(
                resolver = { referanse ->
                    hentIdentForBehandling(BehandlingReferanse(UUID.fromString(referanse)), dataSource)
                },
                param = pathParam
            )
        )

    private fun hentIdentForBehandling(referanse: BehandlingReferanse, dataSource: DataSource) =
        dataSource.transaction(readOnly = true) {
            val behandlingRepository = repositoryRegistry.provider(it).provide<BehandlingRepository>()
            val sakRepository = repositoryRegistry.provider(it).provide<SakRepository>()
            val personRepository = repositoryRegistry.provider(it).provide<PersonRepository>()
            val sakId = behandlingRepository.hentSakId(referanse)
            val personId = sakRepository.finnPersonId(sakId)
            val person = personRepository.hent(personId)
            person.aktivIdent().identifikator
        }
}