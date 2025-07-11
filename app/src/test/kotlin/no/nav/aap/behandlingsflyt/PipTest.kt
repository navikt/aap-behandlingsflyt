package no.nav.aap.behandlingsflyt

import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.runBlocking
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.BarnRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.OppgitteBarn
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn.VurderingAvForeldreAnsvar
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn.VurdertBarn
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.pip.IdenterDTO
import no.nav.aap.behandlingsflyt.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.postgresRepositoryRegistry
import no.nav.aap.behandlingsflyt.repository.sak.PersonRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.SakRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Årsak
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.ÅrsakTilBehandling
import no.nav.aap.behandlingsflyt.test.Fakes
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.error.DefaultResponseHandler
import no.nav.aap.komponenter.httpklient.httpclient.get
import no.nav.aap.komponenter.httpklient.httpclient.request.GetRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.io.InputStream
import java.net.URI
import java.time.LocalDate

@Fakes
class PipTest {
    companion object {
        private lateinit var port: Number

        private val dataSource = InitTestDatabase.freshDatabase()

        private val client: RestClient<InputStream> = RestClient(
            config = ClientConfig(scope = "behandlingsflyt"),
            tokenProvider = ClientCredentialsTokenProvider,
            responseHandler = DefaultResponseHandler()
        )

        // Starter server
        private val server = embeddedServer(Netty, port = 0) {
            System.setProperty("NAIS_CLUSTER_NAME", "LOCAL")
            server(dataSource, repositoryRegistry = postgresRepositoryRegistry)
        }

        @JvmStatic
        @BeforeAll
        fun beforeall() {
            server.start()
            port = runBlocking {
                server.engine.resolvedConnectors().first { it.type == ConnectorType.Companion.HTTP }.port
            }
        }

        @JvmStatic
        @AfterAll
        fun afterAll() {
            server.stop()
        }
    }

    @Test
    fun `pip test sak`() {
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

            val barnRepository = postgresRepositoryRegistry.provider(connection).provide<BarnRepository>()

            barnRepository.lagreRegisterBarn(behandling.id, listOf(Ident("regbarn")))
            barnRepository.lagreOppgitteBarn(behandling.id, OppgitteBarn(identer = listOf(Ident("oppgittbarn"))))
            barnRepository.lagreVurderinger(
                behandling.id,
                "ident",
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

        Assertions.assertThat(pipIdenter).isNotNull
        Assertions.assertThat(pipIdenter?.søker)
            .isNotEmpty
            .contains("ident", "gammelident", "endaeldreident")
        Assertions.assertThat(pipIdenter?.barn)
            .isNotEmpty
            .contains("regbarn", "oppgittbarn", "vurdertbarn")
    }

    @Test
    fun `pip test behandling`() {
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

            val barnRepository = postgresRepositoryRegistry.provider(connection).provide<BarnRepository>()

            barnRepository.lagreRegisterBarn(behandling.id, listOf(Ident("regbarn")))
            barnRepository.lagreOppgitteBarn(behandling.id, OppgitteBarn(identer = listOf(Ident("oppgittbarn"))))
            barnRepository.lagreVurderinger(
                behandling.id,
                "ident",
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

            barnRepository.lagreRegisterBarn(behandling2.id, listOf(Ident("regbar2")))
            barnRepository.lagreOppgitteBarn(behandling2.id, OppgitteBarn(identer = listOf(Ident("oppgittbar2"))))
            barnRepository.lagreVurderinger(
                behandling2.id,
                "ident",
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

        Assertions.assertThat(pipIdenter).isNotNull
        Assertions.assertThat(pipIdenter?.søker)
            .hasSize(3)
            .contains("ident", "gammelident", "endaeldreident")
        Assertions.assertThat(pipIdenter?.barn)
            .hasSize(6)
            .contains("regbarn", "oppgittbarn", "vurdertbarn", "regbar2", "oppgittbar2", "vurdertbar2")
    }
}