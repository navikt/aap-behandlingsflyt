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
import no.nav.aap.tilgang.Behandlingsreferanse
import no.nav.aap.tilgang.Operasjon
import no.nav.aap.tilgang.Ressurs
import no.nav.aap.tilgang.RessursType
import no.nav.aap.tilgang.Saksreferanse
import no.nav.aap.tilgang.authorizedBehandlingPost
import no.nav.aap.tilgang.authorizedGet
import no.nav.aap.tilgang.authorizedSakPost
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import java.net.URI
import java.util.*

class TilgangPluginTest {
    companion object {
        private val fakes = Fakes(azurePort = 8081)

        private val client = RestClient(
            config = ClientConfig(scope = "behandlingsflyt"),
            tokenProvider = ClientCredentialsTokenProvider,
            responseHandler = DefaultResponseHandler()
        )

        class Saksinfo(val saksnummer: UUID) : Saksreferanse {
            override fun hentSaksreferanse(): String {
                return saksnummer.toString()
            }
        }

        class Behandlingsinfo(val behandlingsnummer: UUID) : Behandlingsreferanse {
            override fun hentBehandlingsreferanse(): String {
                return behandlingsnummer.toString()
            }

            override fun hentAvklaringsbehovKode(): String? {
                return null
            }
        }

        fun NormalOpenAPIRoute.getTestRoute() {
            route("testApi/sak/{saksnummer}") {
                authorizedGet<TestReferanse, Saksinfo>(
                    Operasjon.SE,
                    Ressurs("saksnummer", RessursType.Sak)
                ) { req ->
                    respond(Saksinfo(saksnummer = req.saksnummer))
                }
            }
        }

        val uuid = UUID.randomUUID()
        fun NormalOpenAPIRoute.postTestRouteSak() {
            route(
                "testApi/sak",
            ) {
                authorizedSakPost<Unit, Saksinfo, Saksinfo>(
                    Operasjon.SAKSBEHANDLE,
                ) { _, dto ->
                    respond(Saksinfo(saksnummer = uuid))
                }
            }
        }

        fun NormalOpenAPIRoute.postTestRouteBehandling() {
            route(
                "testApi/behandling",
            ) {
                authorizedBehandlingPost<Unit, Behandlingsinfo, Behandlingsinfo>(
                    Operasjon.SAKSBEHANDLE,
                ) { _, dto ->
                    respond(Behandlingsinfo(behandlingsnummer = uuid))
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
                getTestRoute()
                postTestRouteSak()
                postTestRouteBehandling()
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
        val res = client.post<_, Saksinfo>(
            URI.create("http://localhost:8080/")
                .resolve("testApi/sak"),
            PostRequest(Saksinfo(uuid))
        )

        assertThat(res?.saksnummer).isEqualTo(uuid)
    }

    @Test
    fun `Skal kunne hente behandlingsnummer fra request body`() {
        val res = client.post<_, Behandlingsinfo>(
            URI.create("http://localhost:8080/")
                .resolve("testApi/behandling"),
            PostRequest(Behandlingsinfo(uuid))
        )

        assertThat(res?.behandlingsnummer).isEqualTo(uuid)
    }

}
