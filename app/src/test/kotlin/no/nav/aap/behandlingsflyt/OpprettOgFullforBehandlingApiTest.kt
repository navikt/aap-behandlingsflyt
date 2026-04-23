package no.nav.aap.behandlingsflyt

import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import no.nav.aap.behandlingsflyt.repository.postgresRepositoryRegistry
import no.nav.aap.behandlingsflyt.test.BehandlingStatusRequest
import no.nav.aap.behandlingsflyt.test.BehandlingStatusRespons
import no.nav.aap.behandlingsflyt.test.FakePersoner
import no.nav.aap.behandlingsflyt.test.Fakes
import no.nav.aap.behandlingsflyt.test.LokalUnleash
import no.nav.aap.behandlingsflyt.test.OpprettDummySakDto
import no.nav.aap.behandlingsflyt.test.OpprettOgFullforBehandlingRespons
import no.nav.aap.behandlingsflyt.test.modell.TestPerson
import no.nav.aap.behandlingsflyt.test.testGatewayProvider
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.error.DefaultResponseHandler
import no.nav.aap.komponenter.httpklient.httpclient.post
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.NoTokenTokenProvider
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.slf4j.LoggerFactory
import java.io.InputStream
import java.net.URI
import java.time.LocalDate

@Fakes
@Execution(ExecutionMode.SAME_THREAD)
class OpprettOgFullforBehandlingApiTest {

    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private val postgres = postgreSQLContainer()

        private val dbConfig = DbConfig(
            url = postgres.jdbcUrl,
            username = postgres.username,
            password = postgres.password,
        )

        private val ccClient: RestClient<InputStream> = RestClient(
            config = ClientConfig(scope = "behandlingsflyt"),
            tokenProvider = ClientCredentialsTokenProvider,
            responseHandler = DefaultResponseHandler(),
        )

        private val noTokenClient: RestClient<InputStream> = RestClient(
            config = ClientConfig(scope = "behandlingsflyt"),
            tokenProvider = NoTokenTokenProvider(),
            responseHandler = DefaultResponseHandler(),
        )

        private val server = embeddedServer(Netty, port = 0) {
            server(
                dbConfig = dbConfig,
                repositoryRegistry = postgresRepositoryRegistry,
                gatewayProvider = testGatewayProvider(LokalUnleash::class),
            )
        }

        @JvmStatic
        @BeforeAll
        fun beforeAll() {
            System.setProperty("NAIS_CLUSTER_NAME", "LOCAL")
            server.start()
        }

        @JvmStatic
        @AfterAll
        fun afterAll() {
            server.stop()
            postgres.close()
        }

        private val port: Number get() = runBlocking {
            server.engine.resolvedConnectors().first { it.type == ConnectorType.HTTP }.port
        }
    }

    @Test
    fun `oppretter og fullfoerer behandling automatisk`() {
        val ident = "10107099950"
        FakePersoner.leggTil(
            TestPerson(
                identer = setOf(no.nav.aap.behandlingsflyt.sakogbehandling.Ident(ident)),
                fødselsdato = no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Fødselsdato(
                    LocalDate.now().minusYears(25)
                ),
            )
        )

        val respons: OpprettOgFullforBehandlingRespons? = ccClient.post(
            URI.create("http://localhost:$port/api/test/opprettOgFullforBehandling"),
            PostRequest(
                body = OpprettDummySakDto(
                    ident = ident,
                    erStudent = false,
                    harYrkesskade = false,
                    harMedlemskap = true,
                    andreUtbetalinger = null,
                )
            )
        )

        requireNotNull(respons) { "Ingen respons fra opprettOgFullforBehandling" }
        val saksnummer = respons.saksnummer

        val behandlingStatus = pollBehandlingStatus(saksnummer)

        assertThat(behandlingStatus?.ferdig).isTrue()
        assertThat(behandlingStatus?.behandlingStatus).isEqualTo("AVSLUTTET")
    }

    private fun pollBehandlingStatus(saksnummer: String): BehandlingStatusRespons? {
        return runBlocking {
            var status: BehandlingStatusRespons? = null
            var tries = 0
            while (tries < 120) {
                try {
                    status = ccClient.post(
                        URI.create("http://localhost:$port/api/test/behandlingStatus"),
                        PostRequest(body = BehandlingStatusRequest(saksnummer = saksnummer))
                    )
                    if (status?.ferdig == true) return@runBlocking status
                } catch (e: Exception) {
                    log.info("Poll exception: $e")
                }
                delay(1000)
                tries++
            }
            status
        }
    }
}
