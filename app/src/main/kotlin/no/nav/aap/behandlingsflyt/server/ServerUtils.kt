package no.nav.aap.behandlingsflyt.server

import com.papsign.ktor.openapigen.OpenAPIGen
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.OpenAPIPipelineResponseContext
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.util.*


/**
 * Triks for å få NormalOpenAPIRoute til å virke med auth
 */
@KtorDsl
fun Route.apiRoute(config: NormalOpenAPIRoute.() -> Unit) {
    NormalOpenAPIRoute(
        this,
        application.plugin(OpenAPIGen).globalModuleProvider
    ).apply(config)
}

suspend inline fun <reified TResponse : Any> OpenAPIPipelineResponseContext<TResponse>.respondWithStatus(
    statusCode: HttpStatusCode = HttpStatusCode.OK,
    response: TResponse
) {
    responder.respond(statusCode, response, pipeline)
}

suspend inline fun <reified TResponse : Any> OpenAPIPipelineResponseContext<TResponse>.respondWithStatus(
    statusCode: HttpStatusCode
) {
    responder.respond(statusCode, Unit, pipeline)
}
