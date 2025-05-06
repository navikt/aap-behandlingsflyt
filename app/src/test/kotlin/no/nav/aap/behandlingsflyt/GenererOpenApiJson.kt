package no.nav.aap.behandlingsflyt

import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.runBlocking
import no.nav.aap.behandlingsflyt.test.FakeServers
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.error.DefaultResponseHandler
import no.nav.aap.komponenter.httpklient.httpclient.post
import no.nav.aap.komponenter.httpklient.httpclient.request.GetRequest
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.NoTokenTokenProvider
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.OidcToken
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.OnBehalfOfTokenProvider
import java.io.BufferedWriter
import java.io.FileWriter
import java.io.InputStream
import java.net.URI
import java.nio.charset.StandardCharsets

fun getToken(): OidcToken {
    val token: OidcToken? = null
    val client = RestClient(
        config = ClientConfig(scope = "behandlingsflyt"),
        tokenProvider = NoTokenTokenProvider(),
        responseHandler = DefaultResponseHandler()
    )
    return token ?: OidcToken(
        client.post<Unit, FakeServers.TestToken>(
            URI.create(requiredConfigForKey("azure.openid.config.token.endpoint")),
            PostRequest(Unit)
        )!!.access_token
    )
}

fun main() {
    FakeServers.start()
    val postgres = postgreSQLContainer()
    lateinit var port: Number

    val dbConfig = DbConfig(
        url = postgres.jdbcUrl,
        username = postgres.username,
        password = postgres.password
    )

    val client: RestClient<InputStream> = RestClient(
        config = ClientConfig(scope = "behandlingsflyt"),
        tokenProvider = OnBehalfOfTokenProvider,
        responseHandler = DefaultResponseHandler()
    )

//    System.setProperty("unleash.server.api.url", "http://localhost:8080")
//    System.setProperty("unleash.server.api.token", "xxxx")
    // Starter server
    val server = embeddedServer(Netty, port = 0) {
        server(dbConfig = dbConfig)
    }.start()

    port = runBlocking { server.engine.resolvedConnectors().first { it.type == ConnectorType.HTTP }.port }

    val openApiDoc =
        requireNotNull(
            client.get(
                URI.create("http://localhost:$port/openapi.json"),
                GetRequest(currentToken = getToken())
            ) { body, _ ->
                String(body.readAllBytes(), StandardCharsets.UTF_8)
            }
        )

    val writer = BufferedWriter(FileWriter("../openapi.json"))
    writer.use { it ->
        it.write(openApiDoc)
    }

    server.stop()

}
