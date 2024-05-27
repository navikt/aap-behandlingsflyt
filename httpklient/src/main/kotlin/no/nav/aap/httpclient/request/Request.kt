package no.nav.aap.httpclient.request

import no.nav.aap.httpclient.Header
import no.nav.aap.httpclient.tokenprovider.OidcToken
import java.time.Duration

interface Request {

    fun additionalHeaders(): List<Header>

    fun timeout(): Duration

    fun currentToken(): OidcToken?
}