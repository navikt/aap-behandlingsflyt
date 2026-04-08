package no.nav.aap.behandlingsflyt

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import io.ktor.http.*
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.db.PersonRepository
import no.nav.aap.behandlingsflyt.tilgang.TilgangGateway
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.httpklient.exception.IkkeTillattException
import no.nav.aap.komponenter.httpklient.exception.VerdiIkkeFunnetException
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.komponenter.server.auth.token
import no.nav.aap.tilgang.Operasjon
import org.slf4j.LoggerFactory
import java.util.UUID
import javax.sql.DataSource


data class PersonIdentRequest (
    val identifikator: UUID,
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
        post<Unit, PersonIdentResponse, PersonIdentRequest> { _, request ->
            try {
                val personIdent = dataSource.transaction(readOnly = true) { connection ->
                    val repositoryProvider = repositoryRegistry.provider(connection)
                    repositoryProvider.provide<PersonRepository>().hent(request.identifikator)
                }.aktivIdent()
                val tilgangGateway = gatewayProvider.provide<TilgangGateway>()
                val harTilgang = tilgangGateway.sjekkTilgangTilPerson(personIdent.identifikator, token(), Operasjon.SE )
                if (!harTilgang) {
                    throw IkkeTillattException("Har ikke tilgang til person")
                }
                respond(PersonIdentResponse(personIdent.identifikator), HttpStatusCode.OK)
            } catch (e: Exception) {
                log.warn("Fant ikke ident for identifikator: ${request.identifikator}", e)
                throw VerdiIkkeFunnetException("Fant ingen person for identifikator: ${request.identifikator}")
            }
        }
    }
}