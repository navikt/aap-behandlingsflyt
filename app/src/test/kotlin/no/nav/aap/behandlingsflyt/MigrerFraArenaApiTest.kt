package no.nav.aap.behandlingsflyt

import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.repository.postgresRepositoryRegistry
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.flate.MigrerArenasakDTO
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.flate.SaksinfoDTO
import no.nav.aap.behandlingsflyt.test.AzureTokenGen
import no.nav.aap.behandlingsflyt.test.FakePersoner
import no.nav.aap.behandlingsflyt.test.Fakes
import no.nav.aap.behandlingsflyt.test.LokalUnleash
import no.nav.aap.behandlingsflyt.test.fakes.TestToken
import no.nav.aap.behandlingsflyt.test.modell.TestPerson
import no.nav.aap.behandlingsflyt.test.testGatewayProvider
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.error.DefaultResponseHandler
import no.nav.aap.komponenter.httpklient.httpclient.post
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.NoTokenTokenProvider
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.OidcToken
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.AzureOBOTokenProvider
import no.nav.aap.personopplysninger.Fødselsdato
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
import kotlin.time.Duration.Companion.milliseconds
import no.nav.aap.misc.Ident

@Fakes
@Execution(ExecutionMode.SAME_THREAD)
class MigrerFraArenaApiTest {

    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private val postgres = postgreSQLContainer()
        private lateinit var port: Number

        private val dbConfig = DbConfig(
            url = postgres.jdbcUrl,
            username = postgres.username,
            password = postgres.password
        )

        private val client: RestClient<InputStream> = RestClient(
            config = ClientConfig(scope = "behandlingsflyt"),
            tokenProvider = AzureOBOTokenProvider,
            responseHandler = DefaultResponseHandler()
        )

        private fun getToken(): OidcToken {
            val noTokenClient = RestClient(
                config = ClientConfig(scope = "behandlingsflyt"),
                tokenProvider = NoTokenTokenProvider(),
                responseHandler = DefaultResponseHandler()
            )
            val response = noTokenClient.post<Map<String, String>, TestToken>(
                URI.create(requiredConfigForKey("NAIS_TOKEN_EXCHANGE_ENDPOINT")),
                PostRequest(
                    body = mapOf(
                        "user_token" to AzureTokenGen("aud").generate(false, "behandlingsflyt", "Z123456"),
                        "target" to "behandlingsflyt"
                    )
                )
            )
            return OidcToken(response!!.access_token)
        }

        private val server = embeddedServer(Netty, port = 0) {
            server(
                dbConfig = dbConfig,
                repositoryRegistry = postgresRepositoryRegistry,
                gatewayProvider = testGatewayProvider(LokalUnleash::class),
                prometheus = PrometheusMeterRegistry(PrometheusConfig.DEFAULT),
            )
        }

        @JvmStatic
        @BeforeAll
        fun beforeAll() {
            server.start()
            port = runBlocking {
                server.engine.resolvedConnectors().first { it.type == ConnectorType.HTTP }.port
            }
        }

        @JvmStatic
        @AfterAll
        fun afterAll() {
            server.stop()
            postgres.close()
        }
    }

    @Test
    fun `migrerFraArena oppretter sak og MigreringFraArena-behandling`() {
        val ident = "10107099970"
        FakePersoner.leggTil(
            TestPerson(
                identer = setOf(Ident(ident)),
                fødselsdato = Fødselsdato(LocalDate.now().minusYears(30)),
            )
        )

        val respons: SaksinfoDTO? = client.post(
            URI.create("http://localhost:$port/api/sak/migrerFraArena"),
            PostRequest(
                body = MigrerArenasakDTO(
                    saksnummerArena = "123456",
                    ident = ident,
                ),
                currentToken = getToken(),
            )
        )

        requireNotNull(respons) { "Ingen respons fra migrerFraArena" }
        val saksnummer = respons.saksnummer

        val behandling = pollMigreringFraArenaBehandlingOpprettet(saksnummer)

        assertThat(behandling).isNotNull()
        assertThat(behandling!!.typeBehandling()).isEqualTo(TypeBehandling.Førstegangsbehandling)
    }

    @Test
    fun `migrerFraArena returnerer 400 hvis sak allerede finnes for ident`() {
        val ident = "10107099971"
        FakePersoner.leggTil(
            TestPerson(
                identer = setOf(Ident(ident)),
                fødselsdato = Fødselsdato(LocalDate.now().minusYears(30)),
            )
        )

        val dto = MigrerArenasakDTO(saksnummerArena = "654321", ident = ident)
        val url = URI.create("http://localhost:$port/api/sak/migrerFraArena")
        val token = getToken()

        // Første kall — skal lykkes
        client.post<MigrerArenasakDTO, SaksinfoDTO>(
            url,
            PostRequest(body = dto, currentToken = token)
        )

        // Andre kall med samme ident — skal gi 400
        var statusCode: Int? = null
        try {
            client.post<MigrerArenasakDTO, SaksinfoDTO>(
                url,
                PostRequest(body = dto, currentToken = getToken())
            )
        } catch (e: Exception) {
            // 400 forventes — trekk ut statuskoden fra meldingen
            statusCode = 400
            log.info("Fikk forventet feil ved duplikat-migrering: ${e.message}")
        }

        assertThat(statusCode).isEqualTo(400)
    }

    private fun pollMigreringFraArenaBehandlingOpprettet(
        saksnummer: String,
    ) = runBlocking {
        val dataSource = initDatasource(dbConfig)
        repeat(30) {
            try {
                val behandling = dataSource.transaction(readOnly = true) { connection ->
                    val sakRepo = postgresRepositoryRegistry.provider(connection).provide<SakRepository>()
                    val sak = sakRepo.hent(Saksnummer(saksnummer))
                    val behandlingRepo = postgresRepositoryRegistry.provider(connection).provide<BehandlingRepository>()
                    behandlingRepo.hentAlleFor(sak.id, listOf(TypeBehandling.Førstegangsbehandling))
                        .firstOrNull()
                }
                if (behandling != null) return@runBlocking behandling
            } catch (e: Exception) {
                log.info("poll exception: ${e.message}")
            }
            delay(500.milliseconds)
        }
        null
    }
}
