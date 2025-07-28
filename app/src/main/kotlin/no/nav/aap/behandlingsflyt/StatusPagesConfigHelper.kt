package no.nav.aap.behandlingsflyt

import com.fasterxml.jackson.core.JacksonException
import io.ktor.http.*
import io.ktor.serialization.*
import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import no.nav.aap.behandlingsflyt.exception.BehandlingUnderProsesseringException
import no.nav.aap.behandlingsflyt.exception.FlytOperasjonException
import no.nav.aap.behandlingsflyt.exception.KanIkkeVurdereEgneVurderingerException
import no.nav.aap.behandlingsflyt.exception.OutdatedBehandlingException
import no.nav.aap.komponenter.httpklient.exception.ApiErrorCode
import no.nav.aap.komponenter.httpklient.exception.ApiException
import no.nav.aap.komponenter.httpklient.exception.IkkeTillattException
import no.nav.aap.komponenter.httpklient.exception.InternfeilException
import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException
import no.nav.aap.komponenter.httpklient.httpclient.error.IkkeFunnetException
import no.nav.aap.komponenter.httpklient.httpclient.error.ManglerTilgangException
import no.nav.aap.komponenter.json.DeserializationException
import org.slf4j.LoggerFactory
import java.net.http.HttpTimeoutException
import java.sql.SQLException

object StatusPagesConfigHelper {
    fun setup(): StatusPagesConfig.() -> Unit = {
        exception<Throwable> { call, cause ->
            val logger = LoggerFactory.getLogger(javaClass)
            val secureLogger = LoggerFactory.getLogger("secureLog")
            val uri = call.request.local.uri

            when (cause) {
                is InternfeilException -> {
                    logger.error(cause.cause?.message ?: cause.message)
                    call.respondWithError(cause)
                }

                is ApiException -> {
                    logger.warn(cause.message, cause)
                    call.respondWithError(cause)
                }

                is FlytOperasjonException -> {
                    call.respondWithError(
                        ApiException(
                            status = when (cause) {
                                is BehandlingUnderProsesseringException -> HttpStatusCode.Conflict
                                is KanIkkeVurdereEgneVurderingerException -> HttpStatusCode.Forbidden
                                is OutdatedBehandlingException -> HttpStatusCode.Conflict
                            },
                            message = cause.body().message ?: "Ukjent feil i behandlingsflyt"
                        )
                    )
                }

                is ManglerTilgangException -> {
                    logger.warn("Mangler tilgang til å vise route: '$uri'", cause)
                    call.respondWithError(IkkeTillattException(message = "Mangler tilgang"))
                }

                is IkkeFunnetException -> {
                    logger.error("Fikk 404 fra ekstern integrasjon", cause)
                    call.respondWithError(
                        ApiException(
                            status = HttpStatusCode.NotFound,
                            message = "Fikk 404 fra ekstern integrasjon. Dette er mest sannsynlig en systemfeil."
                        )
                    )
                }

                is JacksonException,
                is JsonConvertException,
                is DeserializationException -> {
                    logger.error("Deserialiseringsfeil ved kall til '$uri': ", cause)

                    call.respondWithError(
                        UgyldigForespørselException(message = "Deserialiseringsfeil ved kall til '$uri'")
                    )
                }

                is SQLException -> {
                    logger.error("SQL-feil ved kall til '$uri' av type ${cause.javaClass.name}. Se sikker logs for flere detaljer.")
                    secureLogger.error("SQL-feil ved kall til '$uri'.", cause)

                    call.respondWithError(InternfeilException("Feil ved kall til '$uri'"))
                }

                is HttpTimeoutException -> {
                    logger.warn("Timeout ved kall til '$uri'", cause)
                    call.respondWithError(
                        ApiException(
                            status = HttpStatusCode.RequestTimeout,
                            message = "Forespørselen tok for lang tid. Prøv igjen om litt."
                        )
                    )
                }

                else -> {
                    logger.error("Ukjent/uhåndtert feil ved kall til '$uri' av type ${cause.javaClass}.", cause)

                    call.respondWithError(InternfeilException("En ukjent feil oppsto"))
                }
            }
        }

        status(HttpStatusCode.NotFound) { call, _ ->
            call.application.log.error("Fikk kall mot endepunkt som ikke finnes: ${call.request.local.uri}")

            call.respondWithError(
                ApiException(
                    status = HttpStatusCode.NotFound,
                    message = "Kunne ikke nå endepunkt: ${call.request.local.uri}",
                    code = ApiErrorCode.ENDEPUNKT_IKKE_FUNNET
                )
            )
        }
    }

    private suspend fun ApplicationCall.respondWithError(exception: ApiException) {
        respond(
            exception.status,
            exception.tilApiErrorResponse()
        )
    }
}