package behandlingsflyt

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
import no.nav.aap.behandlingsflyt.datadeling.Maksimum
import no.nav.aap.behandlingsflyt.datadeling.SakStatus
import no.nav.aap.behandlingsflyt.datadeling.SakerRequest
import no.nav.aap.behandlingsflyt.datadeling.datadelingAPI
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.test.MockDataSource
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryBehandlingRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryPersonRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemorySakRepository
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.lookup.repository.RepositoryRegistry
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.time.LocalDate

class DatadelingApiTest {
    companion object{
        @BeforeAll
        @JvmStatic
        fun beforeAll() {
            RepositoryRegistry
                .register<InMemorySakRepository>()
                .register<InMemoryBehandlingRepository>()
                .register<InMemoryPersonRepository>()
        }
    }

    @Test
    fun `hente ut samordningsgrunnlag fra API`() {
        val ds = MockDataSource()
        val person = nyPerson(listOf(Ident("12345678910")))
        val sak = nySak(person)
        val behandling = opprettBehandling(sak, TypeBehandling.Revurdering)

        testApplication {
            installApplication {
                datadelingAPI(ds)
            }
            val body = SakerRequest(listOf("12345678910"))

            val response = createClient().post("/api/datadeling/sakerByFnr") {
                contentType(ContentType.Application.Json)
                setBody(body)
            }
            assertThat(response.status).isEqualTo(HttpStatusCode.OK)

            assertThat(response.body<List<SakStatus>>()).isEqualTo(
                listOf(
                    SakStatus(
                        sakId = "10000",
                        statusKode = SakStatus.VedtakStatus.OPPRE,
                        periode = Maksimum.Periode(sak.rettighetsperiode.fom, sak.rettighetsperiode.tom)
                    )
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

    private fun nyPerson(identer: List<Ident>) = InMemoryPersonRepository.finnEllerOpprett(
        identer
    )

    private fun nySak(person: Person) = InMemorySakRepository.finnEllerOpprett(
        person,
        periode = Periode(LocalDate.now(), LocalDate.now().plusYears(1))
    )

    private fun ApplicationTestBuilder.createClient() = createClient {
        install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
            jackson {
                registerModule(JavaTimeModule())
                disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            }
        }
    }
}