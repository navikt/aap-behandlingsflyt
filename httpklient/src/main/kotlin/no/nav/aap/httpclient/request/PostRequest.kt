package no.nav.aap.httpclient.request

import no.nav.aap.json.DefaultJsonMapper
import java.net.URLEncoder
import java.nio.charset.Charset
import java.time.Duration

class PostRequest<T : Any, R>(
    val body: T,
    private val contentType: ContentType = ContentType.APPLICATION_JSON,
    val responseClazz: Class<R>,
    private val additionalHeaders: List<Pair<String, String>> = emptyList(),
    private val timeout: Duration = Duration.ofSeconds(60)
) : Request {
    fun contentType(): String {
        return contentType.toString()
    }

    fun convertBodyToString(): String {
        if (contentType == ContentType.APPLICATION_JSON) {
            return DefaultJsonMapper.toJson(body)
        }
        if (contentType == ContentType.APPLICATION_FORM_URLENCODED) {
            if (body is String) {
                return URLEncoder.encode(body, Charset.forName("UTF-8"))
            } else {
                throw IllegalArgumentException("Definert '${contentType()}' men body er ikke av type String")
            }
        }
        throw IllegalArgumentException("Ikke supportert content-type definert '${contentType()}'")
    }

    override fun aditionalHeaders(): List<Pair<String, String>> {
        return additionalHeaders
    }

    override fun timeout(): Duration {
        return timeout
    }
}