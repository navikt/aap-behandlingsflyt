package no.nav.aap.behandlingsflyt

import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.runBlocking
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.Barn
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.BarnRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.OppgitteBarn
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Fødselsdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn.BarnIdentifikator
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn.VurderingAvForeldreAnsvar
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn.VurdertBarn
import no.nav.aap.behandlingsflyt.integrasjon.defaultGatewayProvider
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.pip.IdenterDTO
import no.nav.aap.behandlingsflyt.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.postgresRepositoryRegistry
import no.nav.aap.behandlingsflyt.repository.sak.PersonRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.SakRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovOgÅrsak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.test.Fakes
import no.nav.aap.komponenter.dbconnect.transaction
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
        private val postgres = postgreSQLContainer()
        private lateinit var port: Number

        private val dbConfig = DbConfig(
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
            System.setProperty("NAIS_CLUSTER_NAME", "LOCAL")
            server(
                dbConfig = dbConfig,
                repositoryRegistry = postgresRepositoryRegistry,
                gatewayProvider = defaultGatewayProvider { },
            )
        }

        @JvmStatic
        @BeforeAll
        fun beforeall() {
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

    val dataSource = initDatasource(dbConfig)

    @Test
    fun `pip test sak`() {

        val saksnummer = dataSource.transaction { connection ->
            val periode = Periode(LocalDate.now(), LocalDate.now())
            val personRepository = PersonRepositoryImpl(connection)
            val person = personRepository.finnEllerOpprett(
                listOf(
                    Ident("ident", true),
                    Ident("gammelident", false),
                    Ident("endaeldreident", false)
                )
            )
            val sak = SakRepositoryImpl(connection).finnEllerOpprett(person, periode)
            val behandling = BehandlingRepositoryImpl(connection).opprettBehandling(
                sakId = sak.id,
                typeBehandling = TypeBehandling.Førstegangsbehandling,
                forrigeBehandlingId = null,
                vurderingsbehovOgÅrsak = VurderingsbehovOgÅrsak(
                    vurderingsbehov = listOf(VurderingsbehovMedPeriode(Vurderingsbehov.MOTTATT_SØKNAD, periode)),
                    årsak = ÅrsakTilOpprettelse.SØKNAD,
                )
            )

            val barnRepository = postgresRepositoryRegistry.provider(connection).provide<BarnRepository>()

            barnRepository.lagreRegisterBarn(
                behandling.id,
                listOf(
                    Barn(
                        ident = BarnIdentifikator.BarnIdent("regbarn"),
                        Fødselsdato(LocalDate.now())
                    )
                ).associateWith { personRepository.finnEllerOpprett(listOf((it.ident as BarnIdentifikator.BarnIdent).ident)).id }
            )
            barnRepository.lagreOppgitteBarn(
                behandling.id,
                OppgitteBarn(oppgitteBarn = listOf(OppgitteBarn.OppgittBarn(Ident("oppgittbarn"), null)))
            )
            barnRepository.lagreVurderinger(
                behandling.id,
                "ident",
                listOf(
                    VurdertBarn(
                        BarnIdentifikator.BarnIdent("vurdertbarn"),
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
            val personRepository = PersonRepositoryImpl(connection)
            val person = personRepository.finnEllerOpprett(
                listOf(
                    Ident("ident", true),
                    Ident("gammelident", false),
                    Ident("endaeldreident", false)
                )
            )
            val sak = SakRepositoryImpl(connection).finnEllerOpprett(person, periode)
            val behandling = BehandlingRepositoryImpl(connection).opprettBehandling(
                sakId = sak.id,
                typeBehandling = TypeBehandling.Førstegangsbehandling,
                forrigeBehandlingId = null,
                vurderingsbehovOgÅrsak = VurderingsbehovOgÅrsak(
                    vurderingsbehov = listOf(VurderingsbehovMedPeriode(Vurderingsbehov.MOTTATT_SØKNAD, periode)),
                    årsak = ÅrsakTilOpprettelse.SØKNAD,
                )
            )

            val barnRepository = postgresRepositoryRegistry.provider(connection).provide<BarnRepository>()

            barnRepository.lagreRegisterBarn(
                behandling.id,
                listOf(
                    Barn(
                        ident = BarnIdentifikator.BarnIdent("regbarn"),
                        Fødselsdato(LocalDate.now())
                    ),
                    Barn(
                        ident = BarnIdentifikator.BarnIdent("regbarn2"),
                        Fødselsdato(LocalDate.now().minusYears(1))
                    )
                ).associateWith { personRepository.finnEllerOpprett(listOf((it.ident as BarnIdentifikator.BarnIdent).ident)).id }
            )
            // Lagrer ett oppgitt barn med ident og ett uten
            barnRepository.lagreOppgitteBarn(
                behandling.id,
                OppgitteBarn(
                    oppgitteBarn = listOf(
                        OppgitteBarn.OppgittBarn(Ident("oppgittbarn"), null), OppgitteBarn.OppgittBarn(
                            null, "Foffo Fjong",
                            Fødselsdato(LocalDate.now().minusYears(10))
                        )
                    )
                )
            )
            barnRepository.lagreVurderinger(
                behandling.id,
                "ident",
                listOf(
                    VurdertBarn(
                        BarnIdentifikator.BarnIdent("vurdertbarn"),
                        listOf(VurderingAvForeldreAnsvar(periode.fom, true, "fordi"))
                    )
                )
            )


            val periode2 = Periode(LocalDate.now().minusYears(5), LocalDate.now().minusYears(5))
            val behandling2 = BehandlingRepositoryImpl(connection).opprettBehandling(
                sakId = sak.id,
                typeBehandling = TypeBehandling.Førstegangsbehandling,
                forrigeBehandlingId = null,
                vurderingsbehovOgÅrsak = VurderingsbehovOgÅrsak(
                    vurderingsbehov = listOf(VurderingsbehovMedPeriode(Vurderingsbehov.MOTTATT_SØKNAD, periode2)),
                    årsak = ÅrsakTilOpprettelse.SØKNAD,
                )
            )

            barnRepository.lagreRegisterBarn(
                behandling2.id,
                listOf(
                    Barn(ident = BarnIdentifikator.BarnIdent("regbar2"), Fødselsdato(LocalDate.now()))
                ).associateWith { personRepository.finnEllerOpprett(listOf((it.ident as BarnIdentifikator.BarnIdent).ident)).id }
            )
            barnRepository.lagreOppgitteBarn(
                behandling.id,
                OppgitteBarn(oppgitteBarn = listOf(OppgitteBarn.OppgittBarn(Ident("oppgittbar2"), null)))
            )

            barnRepository.lagreVurderinger(
                behandling2.id,
                "ident",
                listOf(
                    VurdertBarn(
                        BarnIdentifikator.BarnIdent("vurdertbar2"),
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
            .containsExactlyInAnyOrder(
                "regbarn",
                "regbarn2",
                "oppgittbarn",
                "vurdertbarn",
                "regbar2",
                "oppgittbar2",
                "vurdertbar2"
            )
    }
}
