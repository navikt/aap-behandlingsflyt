package no.nav.aap.httpclient.tokenprovider.azurecc

import no.nav.aap.requiredConfigForKey
import java.net.URI

class AzureConfig(
    val tokenEndpoint: URI = URI.create(requiredConfigForKey("AZURE_OPENID_CONFIG_TOKEN_ENDPOINT")),
    val clientId: String = requiredConfigForKey("AZURE_APP_CLIENT_ID"),
    val clientSecret: String = requiredConfigForKey("AZURE_APP_CLIENT_SECRET"),
    val jwksUri: String = requiredConfigForKey("AZURE_OPENID_CONFIG_JWKS_URI"),
    val issuer: String = requiredConfigForKey("AZURE_OPENID_CONFIG_ISSUER")
)
