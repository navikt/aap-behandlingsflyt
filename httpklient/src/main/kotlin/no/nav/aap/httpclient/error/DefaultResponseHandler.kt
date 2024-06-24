package no.nav.aap.httpclient.error

import no.nav.aap.httpclient.ClientConfig
import org.slf4j.LoggerFactory
import java.net.HttpURLConnection
import java.net.http.HttpHeaders
import java.net.http.HttpRequest
import java.net.http.HttpResponse


class DefaultResponseHandler(private val config: ClientConfig) : RestResponseHandler {

    private val SECURE_LOGGER = LoggerFactory.getLogger("secureLog")
    private val log = LoggerFactory.getLogger(DefaultResponseHandler::class.java)

    override fun <R> håndter(
        request: HttpRequest,
        response: HttpResponse<String>,
        mapper: (String, HttpHeaders) -> R
    ): R? {
        val status: Int = response.statusCode()
        if (status == HttpURLConnection.HTTP_NO_CONTENT) {
            return null
        }
        if ((status >= HttpURLConnection.HTTP_OK && status < HttpURLConnection.HTTP_MULT_CHOICE)
            || config.isParseableStatus(status)
        ) {
            val value = response.body()
            if (value == null || value.isEmpty()) {
                return null
            }

            loggRespons(value)
            return mapper(value, response.headers())
        }
        if (status == HttpURLConnection.HTTP_BAD_REQUEST) {
            throw UhåndtertHttpResponsException("$response :: ${response.body()}")
        }
        if (status == HttpURLConnection.HTTP_FORBIDDEN) {
            throw ManglerTilgangException("$response :: ${response.body()}")
        }

        throw UhåndtertHttpResponsException("Uventet httprespons kode $response")
    }

    private fun loggRespons(value: String?) {
        // TODO: Temp
        val cluster = System.getenv("NAIS_CLUSTER_NAME")
        if (cluster?.substring(0, cluster.indexOf("-"))?.equals("DEV", ignoreCase = true) == true) {
            log.info(value)
        }
        SECURE_LOGGER.info(value)
    }
}