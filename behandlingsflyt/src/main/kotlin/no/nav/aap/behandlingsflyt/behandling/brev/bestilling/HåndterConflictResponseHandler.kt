package no.nav.aap.behandlingsflyt.behandling.brev.bestilling

import no.nav.aap.komponenter.httpklient.httpclient.error.DefaultResponseHandler
import no.nav.aap.komponenter.httpklient.httpclient.error.RestResponseHandler
import org.slf4j.LoggerFactory
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.http.HttpHeaders
import java.net.http.HttpRequest
import java.net.http.HttpResponse

class HåndterConflictResponseHandler : RestResponseHandler<InputStream> {

    private val log = LoggerFactory.getLogger(javaClass)

    private val defaultResponseHandler = DefaultResponseHandler()

    override fun bodyHandler(): HttpResponse.BodyHandler<InputStream> {
        return defaultResponseHandler.bodyHandler()
    }

    override fun <R> håndter(
        request: HttpRequest,
        response: HttpResponse<InputStream>,
        mapper: (InputStream, HttpHeaders) -> R
    ): R? {
        if (response.statusCode() == HttpURLConnection.HTTP_CONFLICT) {
            log.warn("Fikk http status kode ${HttpURLConnection.HTTP_CONFLICT}. Forsøker å tolke response som forventet.")
            return mapper(response.body(), response.headers())
        }

        return defaultResponseHandler.håndter(request, response, mapper)
    }
}