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
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.db.PersonRepositoryImpl
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
import no.nav.aap.behandlingsflyt.pip.IdenterDTO
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.ÅrsakTilBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
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
            port = runBlocking { server.engine.resolvedConnectors().first { it.type == ConnectorType.HTTP }.port }
        }

        @JvmStatic
        @AfterAll
        fun afterAll() {
            server.stop()
            postgres.close()
        }
    }

    @Test
    fun `pip test sak`() {
        val dataSource = initDatasource(dbConfig)

        val saksnummer = dataSource.transaction { connection ->
            val periode = Periode(LocalDate.now(), LocalDate.now())
            val person = PersonRepositoryImpl(connection).finnEllerOpprett(
                listOf(
                    Ident("ident", true),
                    Ident("gammelident", false),
                    Ident("endaeldreident", false)
                )
            )
            val sak = SakRepositoryImpl(connection).finnEllerOpprett(person, periode)
            val behandling = BehandlingRepositoryImpl(connection).opprettBehandling(
                sak.id,
                listOf(Årsak(ÅrsakTilBehandling.MOTTATT_SØKNAD, periode)),
                TypeBehandling.Førstegangsbehandling, null
            )

            val barnRepository = BarnRepository(connection)

            barnRepository.lagreRegisterBarn(behandling.id, setOf(Ident("regbarn")))
            barnRepository.lagreOppgitteBarn(behandling.id, OppgitteBarn(identer = setOf(Ident("oppgittbarn"))))
            barnRepository.lagreVurderinger(
                behandling.id,
                listOf(
                    VurdertBarn(
                        Ident("vurdertbarn"),
                        listOf(VurderingAvForeldreAnsvar(periode.fom, true, "fordi"))
                    )
                )
            )

            sak.saksnummer
        }

        val pipIdenter: IdenterDTO? = client.get(
            URI.create("http://localhost:$port/")
                .resolve("pip/api/sak/$saksnummer/identer"),
            GetRequest()
        )

        assertThat(pipIdenter).isNotNull
        assertThat(pipIdenter?.søker)
            .isNotEmpty
            .contains("ident", "gammelident", "endaeldreident")
        assertThat(pipIdenter?.barn)
            .isNotEmpty
            .contains("regbarn", "oppgittbarn", "vurdertbarn")
    }

    @Test
    fun `pip test behandling`() {
        val dataSource = initDatasource(dbConfig)

        val behandlingsreferanse = dataSource.transaction { connection ->
            val periode = Periode(LocalDate.now(), LocalDate.now())
            val person = PersonRepositoryImpl(connection).finnEllerOpprett(
                listOf(
                    Ident("ident", true),
                    Ident("gammelident", false),
                    Ident("endaeldreident", false)
                )
            )
            val sak = SakRepositoryImpl(connection).finnEllerOpprett(person, periode)
            val behandling = BehandlingRepositoryImpl(connection).opprettBehandling(
                sak.id,
                listOf(Årsak(ÅrsakTilBehandling.MOTTATT_SØKNAD, periode)),
                TypeBehandling.Førstegangsbehandling, null
            )

            val barnRepository = BarnRepository(connection)

            barnRepository.lagreRegisterBarn(behandling.id, setOf(Ident("regbarn")))
            barnRepository.lagreOppgitteBarn(behandling.id, OppgitteBarn(identer = setOf(Ident("oppgittbarn"))))
            barnRepository.lagreVurderinger(
                behandling.id,
                listOf(
                    VurdertBarn(
                        Ident("vurdertbarn"),
                        listOf(VurderingAvForeldreAnsvar(periode.fom, true, "fordi"))
                    )
                )
            )


            val periode2 = Periode(LocalDate.now().minusYears(5), LocalDate.now().minusYears(5))
            val behandling2 = BehandlingRepositoryImpl(connection).opprettBehandling(
                sak.id,
                listOf(Årsak(ÅrsakTilBehandling.MOTTATT_SØKNAD, periode2)),
                TypeBehandling.Førstegangsbehandling, null
            )

            barnRepository.lagreRegisterBarn(behandling2.id, setOf(Ident("regbar2")))
            barnRepository.lagreOppgitteBarn(behandling2.id, OppgitteBarn(identer = setOf(Ident("oppgittbar2"))))
            barnRepository.lagreVurderinger(
                behandling2.id,
                listOf(
                    VurdertBarn(
                        Ident("vurdertbar2"),
                        listOf(VurderingAvForeldreAnsvar(periode.fom, true, "fordi"))
                    )
                )
            )

            behandling.referanse.referanse
        }

        val pipIdenter: IdenterDTO? = client.get(
            URI.create("http://localhost:$port/")
                .resolve("pip/api/behandling/$behandlingsreferanse/identer"),
            GetRequest()
        )

        assertThat(pipIdenter).isNotNull
        assertThat(pipIdenter?.søker)
            .hasSize(3)
            .contains("ident", "gammelident", "endaeldreident")
        assertThat(pipIdenter?.barn)
            .hasSize(6)
            .contains("regbarn", "oppgittbarn", "vurdertbarn", "regbar2", "oppgittbar2", "vurdertbar2")
    }
}

private fun postgreSQLContainer(): PostgreSQLContainer<Nothing> {
    val postgres = PostgreSQLContainer<Nothing>("postgres:16")
    postgres.waitingFor(HostPortWaitStrategy().withStartupTimeout(Duration.of(60L, ChronoUnit.SECONDS)))
    postgres.start()
    return postgres
}
