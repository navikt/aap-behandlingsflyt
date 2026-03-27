package no.nav.aap.behandlingsflyt

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import io.ktor.http.*
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.db.PersonRepository
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.repository.RepositoryRegistry
import javax.sql.DataSource


data class PersonIdentRequest (
    val personId: Long,
)
data class PersonIdentResponse(
    val ident: String
)
fun NormalOpenAPIRoute.personApi(
    dataSource: DataSource,
    repositoryRegistry: RepositoryRegistry,
) {
    route("/api/person") {
        route("/ident").post<Unit, PersonIdentResponse, PersonIdentRequest> { _, request ->
            val personIdent = dataSource.transaction(readOnly = true) { connection ->
                val repositoryProvider = repositoryRegistry.provider(connection)
                repositoryProvider.provide<PersonRepository>().hent(PersonId(request.personId))
            }.aktivIdent()
            respond(PersonIdentResponse(personIdent.identifikator), HttpStatusCode.OK)
        }
    }
}