package no.nav.aap.httpclient.request

import java.time.Duration

class GetRequest<R>(
    val responseClazz: Class<R>,
    private val additionalHeaders: List<Pair<String, String>> = emptyList(),
    private val timeout: Duration = Duration.ofSeconds(60),
) : Request {
    override fun aditionalHeaders(): List<Pair<String, String>> {
        return additionalHeaders
    }

    override fun timeout(): Duration {
        return timeout
    }
}