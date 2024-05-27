package no.nav.aap.httpclient.request

import no.nav.aap.httpclient.Header
import no.nav.aap.httpclient.tokenprovider.OidcToken
import java.time.Duration

class GetRequest(
    private val additionalHeaders: List<Header> = emptyList(),
    private val timeout: Duration = Duration.ofSeconds(60),
    private val currentToken: OidcToken? = null
) : Request {
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