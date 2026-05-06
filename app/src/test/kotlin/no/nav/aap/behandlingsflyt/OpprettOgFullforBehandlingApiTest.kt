package no.nav.aap.behandlingsflyt

import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.InntektPerÅr
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Fødselsdato
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.AfpDto
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.repository.postgresRepositoryRegistry
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.test.AndreUtbetalingerApiDto
import no.nav.aap.behandlingsflyt.test.AndreUtbetalingerYtelserApiDto
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.behandlingsflyt.test.BehandlingStatusEnum
import no.nav.aap.behandlingsflyt.test.BehandlingStatusRequest
import no.nav.aap.behandlingsflyt.test.BehandlingStatusRespons
import no.nav.aap.behandlingsflyt.test.FakePersoner
import no.nav.aap.behandlingsflyt.test.Fakes
import no.nav.aap.behandlingsflyt.test.JaNeiDto
import no.nav.aap.behandlingsflyt.test.LokalUnleash
import no.nav.aap.behandlingsflyt.test.OpprettOgFullforBehandlingRequest
import no.nav.aap.behandlingsflyt.test.OpprettOgFullforBehandlingRespons
import no.nav.aap.behandlingsflyt.test.modell.TestPerson
import no.nav.aap.behandlingsflyt.test.modell.defaultInntekt
import no.nav.aap.behandlingsflyt.test.testGatewayProvider
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.error.DefaultResponseHandler
import no.nav.aap.komponenter.httpklient.httpclient.post
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
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
import kotlin.time.Duration.Companion.milliseconds

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
    fun `oppretter og fullfører behandling automatisk`() {
        opprettOgVerifiserBehandling(
            ident = "10107099950",
            erStudent = false,
        )
    }

    @Test
    fun `oppretter og fullfører behandling automatisk for student`() {
        opprettOgVerifiserBehandling(
            ident = "10107099951",
            erStudent = true,
        )
    }

    @Test
    fun `oppretter og fullfører behandling automatisk uten inntekt`() {
        opprettOgVerifiserBehandling(
            ident = "10107099955",
            erStudent = true,
            brukersInntekter = emptyList()
        )
    }

    @Test
    fun `oppretter og fullfører behandling automatisk med samordning afp`() {
        opprettOgVerifiserBehandling(
            ident = "10107099956",
            erStudent = true,
            andreUtbetalingerApiDto = AndreUtbetalingerApiDto(
                loenn = JaNeiDto.JA,
                afp = AfpDto("Nav"),
                stoenad = listOf(AndreUtbetalingerYtelserApiDto.AFP)
            )
        )
    }

    @Test
    fun `oppretter og fullfører behandling automatisk uten medlemskap`() {
        opprettOgVerifiserBehandling(
            ident = "10107099952",
            erStudent = false,
            harMedlemskap = false,
        )
    }

    private fun opprettOgVerifiserBehandling(
        ident: String,
        erStudent: Boolean,
        harMedlemskap: Boolean = true,
        harYrkesskade: Boolean = false,
        andreUtbetalingerApiDto: AndreUtbetalingerApiDto? = null,
        brukersInntekter: List<InntektPerÅr>? = null,
    ) {
        FakePersoner.leggTil(
            TestPerson(
                identer = setOf(Ident(ident)),
                fødselsdato = Fødselsdato(
                    LocalDate.now().minusYears(25)
                ),
            ).medInntekter(brukersInntekter ?: defaultInntekt()),
        )

        val respons: OpprettOgFullforBehandlingRespons? = ccClient.post(
            URI.create("http://localhost:$port/api/test/opprettOgFullfoerBehandling"),
            PostRequest(
                body = OpprettOgFullforBehandlingRequest(
                    ident = ident,
                    erStudent = erStudent,
                    harYrkesskade = harYrkesskade,
                    harMedlemskap = harMedlemskap,
                    andreUtbetalinger = andreUtbetalingerApiDto,
                )
            )
        )

        requireNotNull(respons) { "Ingen respons fra opprettOgFullforBehandling" }
        val saksnummer = respons.saksnummer

        val behandlingStatus = pollBehandlingStatus(ident)

        assertThat(behandlingStatus?.ferdig).isTrue()
        assertThat(behandlingStatus?.behandlingStatus).isEqualTo(BehandlingStatusEnum.AVSLUTTET)

        val dataSource = initDatasource(dbConfig)
        dataSource.transaction { connection ->
            val sakRepo = postgresRepositoryRegistry.provider(connection).provide<SakRepository>()
            val sak = sakRepo.hent(Saksnummer(saksnummer))

            val behandlingRepo = postgresRepositoryRegistry.provider(connection).provide<BehandlingRepository>()
            val behandlinger = behandlingRepo.hentAlleFor(sak.id)

            assertThat(behandlinger).hasSize(1)
            assertThat(behandlinger.first().status()).isEqualTo(Status.AVSLUTTET)
        }
    }

    private fun pollBehandlingStatus(ident: String): BehandlingStatusRespons? = runBlocking {
        repeat(120) {
            try {
                val status = ccClient.post<BehandlingStatusRequest, BehandlingStatusRespons>(
                    URI.create("http://localhost:$port/api/test/behandlingStatus"),
                    PostRequest(body = BehandlingStatusRequest(ident = ident))
                )
                if (status?.ferdig == true) return@runBlocking status
            } catch (e: Exception) {
                log.info("Poll exception: $e")
            }
            delay(1000.milliseconds)
        }
        null
    }
}
