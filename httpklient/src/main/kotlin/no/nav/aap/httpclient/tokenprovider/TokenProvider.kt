package no.nav.aap.httpclient.tokenprovider

interface TokenProvider {

    fun getToken(scope: String?, currentToken: OidcToken?): OidcToken? {
        return null
    }
}
