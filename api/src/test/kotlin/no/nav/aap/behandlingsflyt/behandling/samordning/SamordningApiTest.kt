package no.nav.aap.behandlingsflyt.behandling.samordning

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.papsign.ktor.openapigen.OpenAPIGen
import com.papsign.ktor.openapigen.route.apiRouting
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.testing.*
import no.nav.aap.behandlingsflyt.behandling.grunnlag.samordning.SamordningYtelseDTO
import no.nav.aap.behandlingsflyt.behandling.grunnlag.samordning.SamordningYtelseVurderingGrunnlagDTO
import no.nav.aap.behandlingsflyt.behandling.grunnlag.samordning.samordningGrunnlag
import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvResponse
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningYtelse
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningYtelsePeriode
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.test.Fakes
import no.nav.aap.behandlingsflyt.test.MockDataSource
import no.nav.aap.behandlingsflyt.test.inmemorygateway.FakeTilgangGateway
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryBehandlingRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemorySakRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemorySamordningVurderingRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemorySamordningYtelseRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryTjenestePensjonRepository
import no.nav.aap.komponenter.gateway.GatewayRegistry
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation


@Fakes
class SamordningApiKtTest {
    private val repositoryRegistry = RepositoryRegistry()
        .register<InMemorySamordningVurderingRepository>()
        .register<InMemorySamordningYtelseRepository>()
        .register<InMemoryBehandlingRepository>()
        .register<InMemoryTjenestePensjonRepository>()

    companion object {
        private val server = MockOAuth2Server()

        @BeforeAll
        @JvmStatic
        fun beforeAll() {
            server.start()


            GatewayRegistry
                .register<FakeTilgangGateway>()
        }

    }

    @Test
    fun `hente ut samordningsgrunnlag fra API`() {
        val ds = MockDataSource()
        val behandling = opprettBehandling(nySak(), TypeBehandling.Revurdering)

        InMemorySamordningYtelseRepository.lagre(
            behandling.id,
            listOf(
                SamordningYtelse(
                    ytelseType = Ytelse.PLEIEPENGER,
                    ytelsePerioder = listOf(
                        SamordningYtelsePeriode(
                            periode = Periode(LocalDate.now(), LocalDate.now().plusYears(1).minusDays(1)),
                            gradering = Prosent(50),
                            kronesum = null,
                        )
                    ),
                    kilde = "kilde",
                    saksRef = "saksref",
                ),
            ),
        )

        testApplication {
            installApplication {
                samordningGrunnlag(ds, repositoryRegistry)
            }

            val jwt = issueToken("nav:aap:afpoffentlig.read")
            val response = createClient().get("/api/behandling/${behandling.referanse.referanse}/grunnlag/samordning") {
                header("Authorization", "Bearer ${jwt.serialize()}")
            }
            assertThat(response.status).isEqualTo(HttpStatusCode.OK)

            assertThat(response.body<SamordningYtelseVurderingGrunnlagDTO>()).isEqualTo(
                SamordningYtelseVurderingGrunnlagDTO(
                    harTilgangTilÅSaksbehandle = true,
                    ytelser = listOf(
                        SamordningYtelseDTO(
                            ytelseType = Ytelse.PLEIEPENGER,
                            kilde = "kilde",
                            saksRef = "saksref",
                            periode =
                                Periode(
                                    fom = LocalDate.now(),
                                    tom = LocalDate.now().plusYears(1).minusDays(1),
                                ),
                            gradering = 50,
                            kronesum = null,
                            endringStatus = EndringStatus.NY
                        )
                    ),
                    vurderinger = listOf(),
                    begrunnelse = null,
                    fristNyRevurdering = null,
                    maksDatoEndelig = null,
                    tpYtelser = null,
                    vurdertAv = null
                )
            )
        }
    }

    private fun ApplicationTestBuilder.installApplication(apiEndepunkt: NormalOpenAPIRoute.() -> Unit) {
        application {
            install(OpenAPIGen)
            install(ContentNegotiation) {
                jackson {
                    registerModule(JavaTimeModule())
                    disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                }
            }

            apiRouting(apiEndepunkt)
        }
    }

    private fun opprettBehandling(sak: Sak, typeBehandling: TypeBehandling) =
        InMemoryBehandlingRepository.opprettBehandling(
            sak.id,
            årsaker = listOf(),
            typeBehandling = typeBehandling,
            forrigeBehandlingId = null,
        )

    private fun nySak() = InMemorySakRepository.finnEllerOpprett(
        person = Person(
            id = 0,
            identifikator = UUID.randomUUID(),
            identer = listOf(Ident("0".repeat(11)))
        ),
        periode = Periode(LocalDate.now(), LocalDate.now().plusYears(1))
    )

    private fun issueToken(scope: String) = server.issueToken(
        issuerId = "default",
        claims = mapOf(
            "scope" to scope,
            "consumer" to mapOf("authority" to "123", "ID" to "0192:889640782")
        ),
    )

    private fun ApplicationTestBuilder.createClient() = createClient {
        install(ClientContentNegotiation) {
            jackson {
                registerModule(JavaTimeModule())
                disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            }
        }
    }
}
