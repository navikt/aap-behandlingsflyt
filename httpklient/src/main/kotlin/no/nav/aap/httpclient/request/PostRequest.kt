package no.nav.aap.httpclient.request

import no.nav.aap.httpclient.Header
import no.nav.aap.httpclient.tokenprovider.OidcToken
import no.nav.aap.json.DefaultJsonMapper
import java.time.Duration

class PostRequest<T : Any>(
    val body: T,
    private val contentType: ContentType = ContentType.APPLICATION_JSON,
    private val additionalHeaders: List<Header> = emptyList(),
    private val timeout: Duration = Duration.ofSeconds(60),
    private val currentToken: OidcToken? = null
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
                return body
            } else {
                throw IllegalArgumentException("Definert '${contentType()}' men body er ikke av type String")
            }
        }
        throw IllegalArgumentException("Ikke supportert content-type definert '${contentType()}'")
    }

    override fun additionalHeaders(): List<Header> {
        return additionalHeaders
    }

    override fun timeout(): Duration {
        return timeout
    }

    override fun currentToken(): OidcToken? {
        return currentToken
    }
}