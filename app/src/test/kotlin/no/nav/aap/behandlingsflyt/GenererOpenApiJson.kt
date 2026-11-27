package no.nav.aap.behandlingsflyt

import com.papsign.ktor.openapigen.route.apiRouting
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import kotlinx.coroutines.runBlocking
import no.nav.aap.behandlingsflyt.integrasjon.defaultGatewayProvider
import no.nav.aap.behandlingsflyt.prosessering.ProsesseringsJobber
import no.nav.aap.behandlingsflyt.test.FakeServers
import no.nav.aap.behandlingsflyt.test.MockDataSource
import no.nav.aap.behandlingsflyt.test.fakes.TestToken
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.inMemoryRepositoryRegistry
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.error.DefaultResponseHandler
import no.nav.aap.komponenter.httpklient.httpclient.post
import no.nav.aap.komponenter.httpklient.httpclient.request.GetRequest
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.NoTokenTokenProvider
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.OidcToken
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.AzureOBOTokenProvider
import no.nav.aap.motor.Motor
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
        client.post<Unit, TestToken>(
            URI.create(requiredConfigForKey("NAIS_TOKEN_ENDPOINT")),
            PostRequest(Unit)
        )!!.access_token
    )
}

fun main() {
    FakeServers.start()
    lateinit var port: Number

    val client: RestClient<InputStream> = RestClient(
        config = ClientConfig(scope = "behandlingsflyt"),
        tokenProvider = AzureOBOTokenProvider,
        responseHandler = DefaultResponseHandler()
    )

    val repositoryRegistry = inMemoryRepositoryRegistry
    val gatewayProvider = defaultGatewayProvider()
    // Trenger ikke en ekte database for å generere openapi-skjemaet, siden rutene aldri kalles.
    val dataSource = MockDataSource()
    val prometheus = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    val motor = Motor(
        dataSource = dataSource,
        jobber = ProsesseringsJobber.alle(),
        prometheus = prometheus,
        repositoryRegistry = repositoryRegistry,
        gatewayProvider = gatewayProvider,
    )

    // Starter server
    val server = embeddedServer(Netty, port = 0) {
        configureCommonModules(prometheus)
        registerApiRoutes(dataSource, dataSource, repositoryRegistry, gatewayProvider, motor, prometheus)

        // Tar med /test-rutene (kun brukt lokalt/i tester) slik at de også blir med i det genererte openapi.json-skjemaet.
        apiRouting {
            testRoutes(gatewayProvider, repositoryRegistry)
        }
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
    writer.use {
        it.write(openApiDoc)
    }

    server.stop()

}
