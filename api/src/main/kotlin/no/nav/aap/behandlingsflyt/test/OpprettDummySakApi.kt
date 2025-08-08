package no.nav.aap.behandlingsflyt.test

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.respondWithStatus
import com.papsign.ktor.openapigen.route.route
import io.ktor.http.HttpStatusCode
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.miljo.Miljø
import no.nav.aap.komponenter.repository.RepositoryRegistry
import javax.sql.DataSource

fun NormalOpenAPIRoute.opprettDummySakApi(dataSource: DataSource, repositoryRegistry: RepositoryRegistry) {
    route("/api/test/opprettDummySak") {
        @Suppress("UnauthorizedPost") // bare tilgjengelig i DEV og lokalt
        post<Unit, String, OpprettDummySakDto> { _, req ->
            if (Miljø.erProd()) {
                respondWithStatus(HttpStatusCode.Unauthorized)
            }

            dataSource.transaction(readOnly = false) { connection ->
                val sakService = TestSakService(repositoryRegistry.provider(connection), GatewayProvider)
                sakService.opprettTestSak(
                    ident = Ident(req.ident),
                    erStudent = req.erStudent,
                    harYrkesskade =  req.harYrkesskade,
                    harMedlemskap = req.harMedlemskap
                )
            }

            respondWithStatus(HttpStatusCode.Accepted)
        }
    }
}

data class OpprettDummySakDto(
    val ident: String,
    val erStudent: Boolean,
    val harYrkesskade: Boolean,
    val harMedlemskap: Boolean,
)