package no.nav.aap.behandlingsflyt.behandling.samordning

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway.ForeldrepengerGateway
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway.ForeldrepengerRequest
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway.ForeldrepengerResponse
import no.nav.aap.komponenter.dbconnect.transaction
import javax.sql.DataSource

// TODO: FJERN DENNE
fun NormalOpenAPIRoute.samordningApi(dataSource: DataSource) {
    route("/api/samordning") {
        route("/fp-test").post<Unit, ForeldrepengerResponse, ForeldrepengerRequest> { _, req ->
            val saker = dataSource.transaction { connection ->
                val fpGateway = ForeldrepengerGateway()
                fpGateway.hentVedtakYtelseForPerson(req)
            }
            respond(saker)
        }
    }
}