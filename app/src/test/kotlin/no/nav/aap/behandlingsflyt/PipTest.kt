package no.nav.aap.behandlingsflyt

import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.runBlocking
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.BarnRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.OppgitteBarn
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn.VurderingAvForeldreAnsvar
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn.VurdertBarn
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Årsak
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.db.PersonRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.db.SakRepositoryImpl
import no.nav.aap.behandlingsflyt.test.Fakes
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.error.DefaultResponseHandler
import no.nav.aap.komponenter.httpklient.httpclient.get
import no.nav.aap.komponenter.httpklient.httpclient.request.GetRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.pip.IdenterDTO
import no.nav.aap.verdityper.flyt.ÅrsakTilBehandling
import no.nav.aap.verdityper.sakogbehandling.Ident
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy
import java.io.InputStream
import java.net.URI
import java.time.Duration
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.time.measureTime

@Fakes
class PipTest {
    companion object {
        private val postgres = postgreSQLContainer()
        private lateinit var port: Number

        private val dbConfig = DbConfig(
            host = "sdg",
            port = "sdf",
            database = "sdf",
            url = postgres.jdbcUrl,
            username = postgres.username,
            password = postgres.password
        )

        private val client: RestClient<InputStream> = RestClient(
            config = ClientConfig(scope = "behandlingsflyt"),
            tokenProvider = ClientCredentialsTokenProvider,
            responseHandler = DefaultResponseHandler()
        )

        // Starter server
        private val server = embeddedServer(Netty, port = 0) {
            server(dbConfig = dbConfig)
        }

        @JvmStatic
        @BeforeAll
        fun beforeall() {
            server.start()
            port = runBlocking { server.resolvedConnectors().filter { it.type == ConnectorType.HTTP }.first().port }
        }

        @JvmStatic
        @AfterAll
        fun afterAll() {
            server.stop()
            postgres.close()
        }
    }

    @Test
    fun `pip test`() {
        val dataSource = initDatasource(dbConfig)

        val identer1 = listOf(
            Ident("ident", true),
            Ident("gammelident", false),
            Ident("endaeldreident", false)
        )
        val saksnummer = dataSource.transaction { connection ->
            val periode = Periode(LocalDate.now(), LocalDate.now())
            val person = PersonRepository(connection).finnEllerOpprett(identer1)
            val sak = SakRepositoryImpl(connection).finnEllerOpprett(person, periode)
            val behandling = BehandlingRepositoryImpl(connection).opprettBehandling(
                sak.id,
                listOf(Årsak(ÅrsakTilBehandling.MOTTATT_SØKNAD, periode)),
                TypeBehandling.Førstegangsbehandling
            )

            val barnRepository = BarnRepository(connection)

            barnRepository.lagreRegisterBarn(behandling.id, setOf(Ident("regbarn")))
            barnRepository.lagreOppgitteBarn(behandling.id, OppgitteBarn(identer = setOf(Ident("oppgittbarn"))))
            barnRepository.lagreVurderinger(
                behandling.id,
                listOf(
                    VurdertBarn(
                        Ident("vurdertbarn"),
                        listOf(VurderingAvForeldreAnsvar(periode, true, "fordi"))
                    )
                )
            )

            sak.saksnummer
        }

        var pipIdenter: IdenterDTO? = null
        val times = 1

        var pip2Identer: IdenterDTO? = null
        val pip2Tid = measureTime {
            repeat(times) {
                pip2Identer = client.get(
                    URI.create("http://localhost:$port/")
                        .resolve("pip/api2/sak/$saksnummer/identer"),
                    GetRequest()
                )
            }
        }

        val pipTid = measureTime {
            repeat(times) {
                pipIdenter = client.get(
                    URI.create("http://localhost:$port/")
                        .resolve("pip/api/sak/$saksnummer/identer"),
                    GetRequest()
                )
            }
        }
        println("Piptid: ${pipTid.inWholeMilliseconds}")
        println("Pip2tid: ${pip2Tid.inWholeMilliseconds}")

        assertThat(pipIdenter).isNotNull
        assertThat(pipIdenter?.søker)
            .isNotEmpty
            .contains("ident", "gammelident", "endaeldreident")
        assertThat(pipIdenter?.barn)
            .isNotEmpty
            .contains("regbarn", "oppgittbarn", "vurdertbarn")

        assertThat(pip2Identer).isNotNull
        assertThat(pip2Identer?.søker)
            .isNotEmpty
            .contains("ident", "gammelident", "endaeldreident")
        assertThat(pip2Identer?.barn)
            .isNotEmpty
            .contains("regbarn", "oppgittbarn", "vurdertbarn")
    }
}

private fun postgreSQLContainer(): PostgreSQLContainer<Nothing> {
    val postgres = PostgreSQLContainer<Nothing>("postgres:16")
    postgres.waitingFor(HostPortWaitStrategy().withStartupTimeout(Duration.of(60L, ChronoUnit.SECONDS)))
    postgres.start()
    return postgres
}
