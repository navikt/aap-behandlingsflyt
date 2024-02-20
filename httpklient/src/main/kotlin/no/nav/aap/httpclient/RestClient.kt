package no.nav.aap.httpclient

import no.nav.aap.httpclient.request.GetRequest
import no.nav.aap.httpclient.request.PostRequest
import no.nav.aap.httpclient.request.Request
import no.nav.aap.httpclient.tokenprovider.TokenProvider
import no.nav.aap.json.DefaultJsonMapper
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import java.net.HttpURLConnection
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.*

class RestClient(private val config: ClientConfig, private val tokenProvider: TokenProvider) {

    private val SECURE_LOGGER = LoggerFactory.getLogger("secureLog")

    private val client = HttpClient.newBuilder()
        .connectTimeout(config.connectionTimeout)
        .proxy(HttpClient.Builder.NO_PROXY)
        .followRedirects(HttpClient.Redirect.NEVER)
        .build()

    fun <T : Any, R> post(uri: URI, request: PostRequest<T, R>): R? {
        val httpRequest = HttpRequest.newBuilder(uri)
            .timeout(request.timeout())
            .header("Content-Type", request.contentType())
            .addHeaders(request)
            .addHeaders(config, tokenProvider, config.scope)
            .POST(HttpRequest.BodyPublishers.ofString(request.convertBodyToString()))
            .build()

        return executeRequestAndHandleResponse(httpRequest, request.responseClazz)
    }

    fun <R> get(uri: URI, request: GetRequest<R>): R? {
        val httpRequest = HttpRequest.newBuilder(uri)
            .addHeaders(request)
            .addHeaders(config, tokenProvider, config.scope)
            .timeout(request.timeout())
            .GET()
            .build()

        return executeRequestAndHandleResponse(httpRequest, request.responseClazz)
    }

    private fun <R> executeRequestAndHandleResponse(request: HttpRequest, clazz: Class<R>): R? {
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())

        val status: Int = response.statusCode()
        if (status == HttpURLConnection.HTTP_NO_CONTENT) {
            return null
        }
        if ((status >= HttpURLConnection.HTTP_OK && status < HttpURLConnection.HTTP_MULT_CHOICE)
            || config.isParseableStatus(status)
        ) {
            val value = response.body()
            SECURE_LOGGER.info(value)
            return DefaultJsonMapper.fromJson(value, clazz)
        }
        if (status == HttpURLConnection.HTTP_FORBIDDEN) {
            throw ManglerTilgangException("Feilet mot ${request.uri()}")
        }

        throw UhåndtertHttpResponsException("Uventet httprespons kode $status")
    }
}

private fun HttpRequest.Builder.addHeaders(restRequest: Request): HttpRequest.Builder {
    for (additionalHeader in restRequest.aditionalHeaders()) {
        this.header(additionalHeader.first, additionalHeader.second)
    }
    return this
}

private fun HttpRequest.Builder.addHeaders(
    clientConfig: ClientConfig,
    tokenProvider: TokenProvider,
    scope: String?
): HttpRequest.Builder {
    for (additionalHeader in clientConfig.additionalHeaders) {
        this.header(additionalHeader.first, additionalHeader.second)
    }
    for (additionalHeader in clientConfig.additionalFunctionalHeaders) {
        this.header(additionalHeader.first, additionalHeader.second.get())
    }

    val token = tokenProvider.getToken(scope)
    if (token != null) {
        this.header("Authorization", "Bearer $token")
    }
    val callId = sikreCorrelationId()
    this.header("X-Correlation-ID", callId)
    return this
}

fun sikreCorrelationId(): String {
    var callid = MDC.get("callId")
    if (callid == null) {
        val uuid = UUID.randomUUID()
        callid = uuid.toString()
        MDC.put("callId", callid)
    }
    return callid
}
