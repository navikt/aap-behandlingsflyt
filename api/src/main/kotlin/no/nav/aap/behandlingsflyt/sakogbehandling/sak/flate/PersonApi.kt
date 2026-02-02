package no.nav.aap.behandlingsflyt.sakogbehandling.sak.flate

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import com.papsign.ktor.openapigen.route.tag
import no.nav.aap.behandlingsflyt.Tags
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.db.PersonRepository
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.tilgang.AuthorizationMachineToMachineConfig
import no.nav.aap.tilgang.authorizedPost
import org.slf4j.LoggerFactory
import javax.sql.DataSource

private val log = LoggerFactory.getLogger("api.person")

fun NormalOpenAPIRoute.personApi(
    dataSource: DataSource,
    repositoryRegistry: RepositoryRegistry,
    gatewayProvider: GatewayProvider,
) {
    val pdlGateway = gatewayProvider.provide(PdlIdentGateway::class)

    route("/api/person").tag(Tags.Sak) {
        route("/eksisterer").authorizedPost<Unit, PersonEksistererIKelvin, SøkPåPersonDTO>(
            AuthorizationMachineToMachineConfig(authorizedRoles = listOf("hent-personinfo"))
        ) { _, dto ->
            val identMedEttFnr = Ident(dto.ident)
            val identliste = pdlGateway.hentAlleIdenterForPerson(identMedEttFnr)

            val eksisterer = dataSource.transaction(readOnly = true) { connection ->
                val repositoryProvider = repositoryRegistry.provider(connection)
                repositoryProvider.provide<PersonRepository>().eksisterer(identliste.toSet())
            }
            respond(PersonEksistererIKelvin(eksisterer))
        }

    }
}