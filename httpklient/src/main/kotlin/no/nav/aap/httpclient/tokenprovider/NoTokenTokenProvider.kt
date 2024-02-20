package no.nav.aap.httpclient.tokenprovider

class NoTokenTokenProvider : TokenProvider {
    override fun getToken(scope: String?): OidcToken? {
        return null
    }
}