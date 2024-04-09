package no.nav.aap.pdl

import no.nav.aap.httpclient.ClientConfig
import no.nav.aap.httpclient.error.DefaultErrorHandler
import no.nav.aap.httpclient.error.RestErrorHandler
import java.net.http.HttpRequest
import java.net.http.HttpResponse

class PdlErrorHandler(config: ClientConfig) : RestErrorHandler {

    private val defaultErrorHandler = DefaultErrorHandler(config)

    override fun <R> håndter(request: HttpRequest, response: HttpResponse<String>, clazz: Class<R>): R? {
        val respons = defaultErrorHandler.håndter(request, response, clazz)

        if (respons != null && respons is PdlResponse) {
            if (respons.errors?.isNotEmpty() == true) {
                throw PdlQueryException(
                    String.format(
                        "Feil %s ved GraphQL oppslag mot %s",
                        respons.errors.map(GraphQLError::message).joinToString(), request.uri()
                    )
                )
            }
        }

        return respons
    }
}

class PdlQueryException(msg: String) : RuntimeException(msg)