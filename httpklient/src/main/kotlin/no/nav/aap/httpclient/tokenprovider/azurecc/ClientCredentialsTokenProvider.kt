package no.nav.aap.httpclient.tokenprovider.azurecc

import no.nav.aap.httpclient.ClientConfig
import no.nav.aap.httpclient.RestClient
import no.nav.aap.httpclient.request.ContentType
import no.nav.aap.httpclient.request.PostRequest
import no.nav.aap.httpclient.tokenprovider.NoTokenTokenProvider
import no.nav.aap.httpclient.tokenprovider.OidcToken
import no.nav.aap.httpclient.tokenprovider.OidcTokenResponse
import no.nav.aap.httpclient.tokenprovider.TokenProvider
import java.net.URLEncoder
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.text.Charsets.UTF_8

private const val BUFFER = 150

object ClientCredentialsTokenProvider : TokenProvider {

    private val client = RestClient(
        config = ClientConfig(),
        tokenProvider = NoTokenTokenProvider()
    )
    private val config = AzureConfig() // Laster config on-demand

    private val cache = HashMap<String, OidcToken>()

    override fun getToken(scope: String?): OidcToken? {
        if (scope == null ) {
            throw IllegalArgumentException("Kan ikke be om token uten å be om hvilket scope det skal gjelde for")
        }
        if (cache.contains(scope) && cache.getValue(scope).isNotExpired()) {
            return cache.getValue(scope)
        }
        val postRequest = PostRequest(
            body = formPost(scope),
            contentType = ContentType.APPLICATION_FORM_URLENCODED,
            responseClazz = OidcTokenResponse::class.java,
            timeout = Duration.ofSeconds(10),
            additionalHeaders = listOf("Cache-Control" to "no-cache")
        )

        val response = client.post(uri = config.tokenEndpoint, request = postRequest)

        if (response == null) {
            return null
        }

        val oidcToken = OidcToken(response.access_token, calculateExpiresTime(response.expires_in))
        cache[scope] = oidcToken
        return oidcToken
    }

    private fun formPost(scope: String): String {
        val encodedScope = URLEncoder.encode(scope, UTF_8)
        return "grant_type=client_credentials" + "&client_id=" + config.clientId + "&client_secret=" + config.clientSecret + "&scope=" + encodedScope
    }
}

internal fun calculateExpiresTime(expiresInSec: Int): LocalDateTime {
    val expiresAtMillis = System.currentTimeMillis() + (expiresInSec * 1000) - BUFFER

    return LocalDateTime.ofInstant(Instant.ofEpochMilli(expiresAtMillis), ZoneId.systemDefault());
}