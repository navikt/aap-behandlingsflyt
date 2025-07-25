package no.nav.aap.behandlingsflyt.integrasjon.pdl

import no.nav.aap.behandlingsflyt.integrasjon.util.GraphQLError
import no.nav.aap.komponenter.httpklient.httpclient.error.DefaultResponseHandler
import no.nav.aap.komponenter.httpklient.httpclient.error.RestResponseHandler
import java.io.InputStream
import java.net.http.HttpHeaders
import java.net.http.HttpRequest
import java.net.http.HttpResponse

class PdlResponseHandler() : RestResponseHandler<InputStream> {

    private val defaultErrorHandler = DefaultResponseHandler()

    override fun <R> håndter(
        request: HttpRequest,
        response: HttpResponse<InputStream>,
        mapper: (InputStream, HttpHeaders) -> R
    ): R? {
        val respons = defaultErrorHandler.håndter(request, response, mapper)

        if (respons != null && respons is PdlResponse) {
            if (respons.errors?.isNotEmpty() == true) {
                throw PdlQueryException(
                    String.format(
                        "Feil %s ved GraphQL oppslag mot %s",
                        respons.errors.joinToString(transform = GraphQLError::message), request.uri()
                    )
                )
            }
        }

        return respons
    }

    override fun bodyHandler(): HttpResponse.BodyHandler<InputStream> {
        return defaultErrorHandler.bodyHandler()
    }
}

class PdlQueryException(msg: String) : RuntimeException(msg)
