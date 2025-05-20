package no.nav.aap.behandlingsflyt.integrasjon.util

import no.nav.aap.komponenter.httpklient.httpclient.error.DefaultResponseHandler
import no.nav.aap.komponenter.httpklient.httpclient.error.RestResponseHandler
import java.io.InputStream
import java.net.http.HttpHeaders
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import kotlin.collections.isNotEmpty
import kotlin.collections.joinToString
import kotlin.text.format


class GraphQLResponseHandler() : RestResponseHandler<InputStream> {

    private val defaultResponseHandler = DefaultResponseHandler()

    override fun <R> håndter(
        request: HttpRequest,
        response: HttpResponse<InputStream>,
        mapper: (InputStream, HttpHeaders) -> R
    ): R? {
        val håndtertResponse = defaultResponseHandler.håndter(request, response, mapper)

        if (håndtertResponse != null && håndtertResponse is GraphQLResponse<*>) {
            if (håndtertResponse.errors?.isNotEmpty() == true) {
                throw GraphQLQueryException(
                    String.format(
                        "Feil %s ved GraphQL oppslag mot %s",
                        håndtertResponse.errors.joinToString(transform = GraphQLError::message), request.uri()
                    )
                )
            }
        }

        return håndtertResponse
    }

    override fun bodyHandler(): HttpResponse.BodyHandler<InputStream> {
        return defaultResponseHandler.bodyHandler()
    }
}

class GraphQLQueryException(msg: String) : RuntimeException(msg)
