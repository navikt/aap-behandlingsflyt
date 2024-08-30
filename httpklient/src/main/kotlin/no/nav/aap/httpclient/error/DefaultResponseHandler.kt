package no.nav.aap.httpclient.error

import no.nav.aap.httpclient.håndterStatus
import no.nav.aap.komponenter.dbflyway.Miljø
import no.nav.aap.komponenter.dbflyway.MiljøKode
import org.slf4j.LoggerFactory
import java.net.http.HttpHeaders
import java.net.http.HttpRequest
import java.net.http.HttpResponse


class DefaultResponseHandler() : RestResponseHandler<String> {
    private val log = LoggerFactory.getLogger(DefaultResponseHandler::class.java)

    override fun <R> håndter(
        request: HttpRequest,
        response: HttpResponse<String>,
        mapper: (String, HttpHeaders) -> R
    ): R? {
        return håndterStatus(response, block = {
            val value = response.body()
            if (value == null || value.isEmpty()) {
                return@håndterStatus null
            } else {
                loggRespons(value)
                return@håndterStatus mapper(value, response.headers())
            }
        })
    }

    private fun loggRespons(value: String?) {
        // TODO: Temp
        val miljø = Miljø.er()
        if (miljø in listOf(MiljøKode.LOKALT)) {
            log.info(value)
        }
    }

    override fun bodyHandler(): HttpResponse.BodyHandler<String> {
        return HttpResponse.BodyHandlers.ofString()
    }
}