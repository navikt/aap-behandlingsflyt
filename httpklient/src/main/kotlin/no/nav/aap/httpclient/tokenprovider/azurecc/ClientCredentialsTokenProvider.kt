package no.nav.aap.httpclient.tokenprovider.azurecc

import no.nav.aap.httpclient.ClientConfig
import no.nav.aap.httpclient.Header
import no.nav.aap.httpclient.RestClient
import no.nav.aap.httpclient.request.ContentType
import no.nav.aap.httpclient.request.PostRequest
import no.nav.aap.httpclient.tokenprovider.NoTokenTokenProvider
import no.nav.aap.httpclient.tokenprovider.OidcToken
import no.nav.aap.httpclient.tokenprovider.OidcTokenResponse
import no.nav.aap.httpclient.tokenprovider.TokenProvider
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URLEncoder
import java.time.Duration
import java.time.LocalDateTime
import kotlin.text.Charsets.UTF_8

object ClientCredentialsTokenProvider : TokenProvider {

    private val log: Logger = LoggerFactory.getLogger(ClientCredentialsTokenProvider::class.java)

    private val client = RestClient(
        config = ClientConfig(),
        tokenProvider = NoTokenTokenProvider()
    )
    private val config = AzureConfig() // Laster config on-demand

    private val cache = HashMap<String, OidcToken>()

    override fun getToken(scope: String?): OidcToken? {
        if (scope == null) {
            throw IllegalArgumentException("Kan ikke be om token uten å be om hvilket scope det skal gjelde for")
        }

        if (cache.contains(scope) && cache.getValue(scope).isNotExpired()) {
            val oidcToken = cache.getValue(scope)
            log.info("Fant token for $scope som ikke har utløpt. Utløper ${oidcToken.expires()}")
            return oidcToken
        }
        val postRequest = PostRequest(
            body = formPost(scope),
            contentType = ContentType.APPLICATION_FORM_URLENCODED,
            timeout = Duration.ofSeconds(10),
            additionalHeaders = listOf(Header("Cache-Control", "no-cache"))
        )

        val response: OidcTokenResponse? = client.post(uri = config.tokenEndpoint, request = postRequest)

        if (response == null) {
            return null
        }

        val oidcToken = OidcToken(response.access_token)
        log.info("Hentet nytt token for $scope. Utløper ${oidcToken.expires()}")
        cache[scope] = oidcToken

        return oidcToken
    }

    private fun formPost(scope: String): String {
        val encodedScope = URLEncoder.encode(scope, UTF_8)
        return "client_id=" + config.clientId + "&client_secret=" + config.clientSecret + "&scope=" + encodedScope + "&grant_type=client_credentials"
    }
}

internal fun calculateExpiresTime(expiresInSec: Int): LocalDateTime {
    val expiresIn =
        Duration.ofSeconds(expiresInSec.toLong()).minus(Duration.ofSeconds(30))

    return LocalDateTime.now().plus(expiresIn);
}