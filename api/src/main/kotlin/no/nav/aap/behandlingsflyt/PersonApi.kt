package no.nav.aap.behandlingsflyt

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import io.ktor.http.*
import no.nav.aap.behandlingsflyt.utils.KryptertString
import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException
import no.nav.aap.komponenter.httpklient.exception.VerdiIkkeFunnetException
import org.slf4j.LoggerFactory
import javax.crypto.AEADBadTagException


data class PersonIdentRequest (
    val kryptertIdent: String,
)
data class PersonIdentResponse(
    val ident: String
)

private val log = LoggerFactory.getLogger("PersonApi")
fun NormalOpenAPIRoute.personApi(
    codec: KryptertString,
) {
    route("/api/person/ident") {
        post<Unit, PersonIdentResponse, PersonIdentRequest> { _, request ->
            try {
                val identifikator = codec.decode(request.kryptertIdent)
                respond(PersonIdentResponse(identifikator), HttpStatusCode.OK)
            } catch (e: AEADBadTagException) {
                log.warn("Kunne ikke dekode kryptertIdent", e)
                throw VerdiIkkeFunnetException("Ugyldig kryptertIdent")
            } catch (e: IllegalArgumentException) {
                log.warn("Ugyldig kryptertIdent format", e)
                throw UgyldigForespørselException("Ugyldig kryptertIdent")
            }
        }
    }
}