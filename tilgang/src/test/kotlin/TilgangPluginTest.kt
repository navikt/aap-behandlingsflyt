import com.papsign.ktor.openapigen.OpenAPIGen
import com.papsign.ktor.openapigen.annotations.parameters.PathParam
import com.papsign.ktor.openapigen.route.apiRouting
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import no.nav.aap.behandlingsflyt.test.Fakes
import no.nav.aap.httpclient.ClientConfig
import no.nav.aap.httpclient.RestClient
import no.nav.aap.httpclient.error.DefaultResponseHandler
import no.nav.aap.httpclient.get
import no.nav.aap.httpclient.post
import no.nav.aap.httpclient.request.GetRequest
import no.nav.aap.httpclient.request.PostRequest
import no.nav.aap.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import org.assertj.core.api.Assertions.assertThat
import no.nav.aap.tilgang.Ressurs
import no.nav.aap.tilgang.Operasjon
import no.nav.aap.tilgang.Referanse
import no.nav.aap.tilgang.ReferanseKilde
import no.nav.aap.tilgang.RessursType
import no.nav.aap.tilgang.authorizedGet
import no.nav.aap.tilgang.authorizedPost
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import java.net.URI
import java.util.*

class TilgangPluginTest {
    companion object {
        private val fakes = Fakes(azurePort = 8081)

        private val client = RestClient(
            config = ClientConfig(scope = "behandlingsflyt"),
            tokenProvider = ClientCredentialsTokenProvider,
            errorHandler = DefaultResponseHandler()
        )

        private val log = LoggerFactory.getLogger(TilgangPluginTest::class.java)

        data class Saksinfo(val saksnummer: UUID);

        fun NormalOpenAPIRoute.pathParamsTestRoute() {
            route("testApi/sak/{saksnummer}") {
                authorizedGet<TestReferanse, Saksinfo>(
                    Operasjon.SE,
                    Ressurs(Referanse("saksnummer", ReferanseKilde.PathParams), RessursType.Sak)
                ) { req ->
                    respond(Saksinfo(saksnummer = req.saksnummer))
                }
            }
        }

        val uuid = UUID.randomUUID()
        fun NormalOpenAPIRoute.responseBodyTestRoute() {
            route(
                "testApi/sak",
            ) {
                authorizedPost<Unit, Saksinfo, Saksinfo>(
                    Operasjon.SAKSBEHANDLE,
                    Ressurs(Referanse("saksnummer", ReferanseKilde.RequestBody), RessursType.Sak)
                ) { _, dto ->
                    log.info("responding...")
                    respond(Saksinfo(saksnummer = uuid))

                }
            }
        }

        // Starter server
        private val server = embeddedServer(Netty, port = 8080) {
            install(OpenAPIGen)
            install(ContentNegotiation) {
                jackson()
            }
            apiRouting {
                pathParamsTestRoute()
                responseBodyTestRoute()
            }
            module(fakes)
        }.start()

        @JvmStatic
        @AfterAll
        fun afterAll() {
            fakes.close()
            server.stop()
        }

        private fun Application.module(fakes: Fakes) {
            // Setter opp virtuell sandkasse lokalt
            environment.monitor.subscribe(ApplicationStopped) { application ->
                application.environment.log.info("Server har stoppet")
                fakes.close()
                // Release resources and unsubscribe from events
                application.environment.monitor.unsubscribe(ApplicationStopped) {}
            }
        }

        data class TestReferanse(
            @PathParam(description = "saksnummer") val saksnummer: UUID = UUID.randomUUID()
        )

    }

    @Test
    fun `Skal kunne hente saksnummer fra path params`() {
        val randomUuid = UUID.randomUUID()
        val res = client.get<Saksinfo>(
            URI.create("http://localhost:8080/")
                .resolve("testApi/sak/$randomUuid"),
            GetRequest()
        )

        assertThat(res?.saksnummer).isEqualTo(randomUuid)
    }

    @Test
    fun `Skal kunne hente saksnummer fra request body`() {
        val res = client.post<_,Saksinfo>(
            URI.create("http://localhost:8080/")
                .resolve("testApi/sak"),
            PostRequest(Saksinfo(uuid))
        )

        assertThat(res?.saksnummer).isEqualTo(uuid)
    }

}
