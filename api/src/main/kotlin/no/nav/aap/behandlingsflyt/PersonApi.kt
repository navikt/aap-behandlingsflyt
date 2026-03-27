package no.nav.aap.behandlingsflyt

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import io.ktor.http.*
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.db.PersonRepository
import no.nav.aap.behandlingsflyt.tilgang.TilgangGateway
import no.nav.aap.behandlingsflyt.tilgang.relevanteIdenterForSakResolver
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.httpklient.exception.IkkeTillattException
import no.nav.aap.komponenter.httpklient.exception.VerdiIkkeFunnetException
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.komponenter.server.auth.token
import no.nav.aap.tilgang.AuthorizationBodyPathConfig
import no.nav.aap.tilgang.Operasjon
import no.nav.aap.tilgang.authorizedPost
import org.slf4j.LoggerFactory
import javax.sql.DataSource


data class PersonIdentRequest (
    val personId: Long,
)
data class PersonIdentResponse(
    val ident: String
)

private val log = LoggerFactory.getLogger("PersonApi")
fun NormalOpenAPIRoute.personApi(
    dataSource: DataSource,
    repositoryRegistry: RepositoryRegistry,
    gatewayProvider: GatewayProvider,
) {
    route("/api/person") {
        authorizedPost<Unit, PersonIdentResponse, PersonIdentRequest>(
            AuthorizationBodyPathConfig(
                operasjon = Operasjon.SE,
                relevanteIdenterResolver = relevanteIdenterForSakResolver(repositoryRegistry, dataSource),
                applicationsOnly = false
            )
        ) { _, request ->
            try {
                val personIdent = dataSource.transaction(readOnly = true) { connection ->
                    val repositoryProvider = repositoryRegistry.provider(connection)
                    repositoryProvider.provide<PersonRepository>().hent(PersonId(request.personId))
                }.aktivIdent()
                val tilgangGateway = gatewayProvider.provide<TilgangGateway>()
                val harTilgang = tilgangGateway.sjekkTilgangTilPerson(personIdent.identifikator, token(), Operasjon.SE )
                if (!harTilgang) {
                    throw IkkeTillattException("Har ikke tilgang til person")
                }
                respond(PersonIdentResponse(personIdent.identifikator), HttpStatusCode.OK)
            } catch (e: Exception) {
                log.warn("Fant ikke ident for personId: ${request.personId}", e)
                throw VerdiIkkeFunnetException("Fant ingen person for personId: ${request.personId}")
            }
        }
    }
}