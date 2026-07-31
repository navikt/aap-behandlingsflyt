package no.nav.aap.behandlingsflyt.ytelseoppslag

import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import no.nav.aap.behandlingsflyt.BaseApiTest
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway.UtbetaltePerioder
import no.nav.aap.behandlingsflyt.integrasjon.createGatewayProvider
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.test.Fakes
import no.nav.aap.behandlingsflyt.test.MockDataSource
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryPersonRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.inMemoryRepositoryRegistry
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

@Fakes
class SykepengeperioderApiTest : BaseApiTest() {

    private val gatewayProvider = createGatewayProvider {
        register<FakeSykepengerGateway>()
    }

    @BeforeEach
    fun reset() = FakeSykepengerGateway.reset()

    @Test
    fun `returnerer sykepengeperioder for person`() {
        val ident = "12345678901"
        InMemoryPersonRepository.finnEllerOpprett(listOf(Ident(ident)))

        val tom = LocalDate.of(2026, 7, 1)
        val fom = tom.minusMonths(4)
        val periodeFom = tom.minusMonths(2)
        val periodeTom = tom.minusMonths(1)
        FakeSykepengerGateway.perioder = listOf(
            UtbetaltePerioder(fom = periodeFom, tom = periodeTom, grad = 100, organisasjonsnummer = "999999999")
        )

        testApplication {
            installApplication {
                sykepengeperioderApi(MockDataSource(), inMemoryRepositoryRegistry, gatewayProvider)
            }

            val response = createClient().post(SYKEPENGEPERIODER_PATH) {
                header("Authorization", "Bearer ${getToken().token()}")
                contentType(ContentType.Application.Json)
                setBody(YtelseoppslagRequest(personident = ident, fom = fom, tom = tom))
            }

            assertThat(response.status).isEqualTo(HttpStatusCode.OK)

            val perioder = response.body<List<SykepengeperiodeDTO>>()
            assertThat(perioder).hasSize(1)
            assertThat(perioder.first().fom).isEqualTo(periodeFom)
            assertThat(perioder.first().tom).isEqualTo(periodeTom)
            assertThat(perioder.first().grad.toInt()).isEqualTo(100)
            assertThat(perioder.first().organisasjonsnummer).isEqualTo("999999999")
        }
    }

    @Test
    fun `slår opp både aktiv og historisk ident`() {
        val aktivIdent = "12345678902"
        val historiskIdent = "10987654322"
        InMemoryPersonRepository.finnEllerOpprett(
            listOf(Ident(historiskIdent, aktivIdent = false), Ident(aktivIdent))
        )

        testApplication {
            installApplication {
                sykepengeperioderApi(MockDataSource(), inMemoryRepositoryRegistry, gatewayProvider)
            }

            val response = createClient().post(SYKEPENGEPERIODER_PATH) {
                header("Authorization", "Bearer ${getToken().token()}")
                contentType(ContentType.Application.Json)
                setBody(oppslag(aktivIdent))
            }

            assertThat(response.status).isEqualTo(HttpStatusCode.OK)
            assertThat(FakeSykepengerGateway.identerBrukt).containsExactlyInAnyOrder(aktivIdent, historiskIdent)
        }
    }

    @Test
    fun `ukjent person gir tom liste og bruker innsendt ident`() {
        testApplication {
            installApplication {
                sykepengeperioderApi(MockDataSource(), inMemoryRepositoryRegistry, gatewayProvider)
            }

            val ukjentIdent = "12345678909"
            val response = createClient().post(SYKEPENGEPERIODER_PATH) {
                header("Authorization", "Bearer ${getToken().token()}")
                contentType(ContentType.Application.Json)
                setBody(oppslag(ukjentIdent))
            }

            assertThat(response.status).isEqualTo(HttpStatusCode.OK)
            assertThat(response.body<List<SykepengeperiodeDTO>>()).isEmpty()
            assertThat(FakeSykepengerGateway.identerBrukt).containsExactly(ukjentIdent)
        }
    }

    @Test
    fun `videresender oppslagsvinduet til gatewayen`() {
        val ident = "12345678904"
        InMemoryPersonRepository.finnEllerOpprett(listOf(Ident(ident)))

        val fom = LocalDate.of(2026, 3, 1)
        val tom = LocalDate.of(2026, 7, 1)

        testApplication {
            installApplication {
                sykepengeperioderApi(MockDataSource(), inMemoryRepositoryRegistry, gatewayProvider)
            }

            val response = createClient().post(SYKEPENGEPERIODER_PATH) {
                header("Authorization", "Bearer ${getToken().token()}")
                contentType(ContentType.Application.Json)
                setBody(YtelseoppslagRequest(personident = ident, fom = fom, tom = tom))
            }

            assertThat(response.status).isEqualTo(HttpStatusCode.OK)
            assertThat(FakeSykepengerGateway.periodeBrukt).isEqualTo(fom to tom)
        }
    }

    private fun oppslag(personident: String) = YtelseoppslagRequest(
        personident = personident,
        fom = LocalDate.of(2026, 1, 1),
        tom = LocalDate.of(2026, 7, 1),
    )
}
