package no.nav.aap.httpclient

import no.nav.aap.httpclient.error.DefaultResponseHandler
import no.nav.aap.httpclient.error.RestResponseHandler
import no.nav.aap.httpclient.request.GetRequest
import no.nav.aap.httpclient.request.PostRequest
import no.nav.aap.httpclient.request.Request
import no.nav.aap.httpclient.tokenprovider.OidcToken
import no.nav.aap.httpclient.tokenprovider.TokenProvider
import no.nav.aap.json.DefaultJsonMapper
import org.slf4j.MDC
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.*

class RestClient(
    private val config: ClientConfig,
    private val tokenProvider: TokenProvider,
    private val errorHandler: RestResponseHandler = DefaultResponseHandler(config)
) {

    private val client = HttpClient.newBuilder()
        .connectTimeout(config.connectionTimeout)
        .proxy(HttpClient.Builder.NO_PROXY)
        .followRedirects(HttpClient.Redirect.NEVER)
        .build()

    inline fun <T : Any, reified R> post(uri: URI, request: PostRequest<T>): R? {
        return post(uri, request, DefaultJsonMapper::fromJson)
    }

    fun <T : Any, R> post(uri: URI, request: PostRequest<T>, mapper: (String) -> R): R? {
        val httpRequest = HttpRequest.newBuilder(uri)
            .timeout(request.timeout())
            .header("Content-Type", request.contentType())
            .addHeaders(request)
            .addHeaders(config, tokenProvider, config.scope, request.currentToken())
            .POST(HttpRequest.BodyPublishers.ofString(request.convertBodyToString()))
            .build()

        return executeRequestAndHandleResponse(httpRequest, mapper)
    }

    inline fun <reified R> get(uri: URI, request: GetRequest): R? {
        return get(uri, request, DefaultJsonMapper::fromJson)
    }

    fun <R> get(uri: URI, request: GetRequest, mapper: (String) -> R): R? {
        val httpRequest = HttpRequest.newBuilder(uri)
            .addHeaders(request)
            .addHeaders(config, tokenProvider, config.scope, request.currentToken())
            .timeout(request.timeout())
            .GET()
            .build()

        return executeRequestAndHandleResponse(httpRequest, mapper)
    }

    private fun <R> executeRequestAndHandleResponse(request: HttpRequest, mapper: (String) -> R): R? {
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())

        log.info("Request: ${request.uri()}. Respons: ${response.body()}")
        return errorHandler.håndter(request, response, mapper)
    }
}

private fun HttpRequest.Builder.addHeaders(restRequest: Request): HttpRequest.Builder {
    restRequest.additionalHeaders().forEach(this::addHeader)
    return this
}

private fun HttpRequest.Builder.addHeaders(
    clientConfig: ClientConfig,
    tokenProvider: TokenProvider,
    scope: String?,
    currentToken: OidcToken?
): HttpRequest.Builder {
    clientConfig.additionalHeaders.forEach(this::addHeader)
    clientConfig.additionalFunctionalHeaders.forEach(this::addHeader)

    val token = tokenProvider.getToken(scope, currentToken)
    if (token != null) {
        this.header("Authorization", "Bearer ${token.token()}")
    }
    val callId = sikreCorrelationId()
    this.header("X-Correlation-ID", callId)
    this.header("Nav-Call-Id", callId)
    return this
}

private fun HttpRequest.Builder.addHeader(header: Header) {
    this.header(header.key, header.value)
}

private fun HttpRequest.Builder.addHeader(header: FunctionalHeader) {
    this.header(header.key, header.supplier())
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
