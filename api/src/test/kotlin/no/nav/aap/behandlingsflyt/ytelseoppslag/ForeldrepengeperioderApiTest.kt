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
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway.Anvist
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway.Utbetalingsgrad
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway.Ytelse
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway.Ytelser
import no.nav.aap.behandlingsflyt.integrasjon.createGatewayProvider
import no.nav.aap.behandlingsflyt.kontrakt.ytelseoppslag.ForeldrepengeperioderDTO
import no.nav.aap.behandlingsflyt.kontrakt.ytelseoppslag.YtelseoppslagRequest
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.test.Fakes
import no.nav.aap.behandlingsflyt.test.MockDataSource
import no.nav.aap.behandlingsflyt.test.inmemorygateway.FakeTilgangGateway
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryPersonRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.inMemoryRepositoryRegistry
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

@Fakes
class ForeldrepengeperioderApiTest : BaseApiTest() {

    private val gatewayProvider = createGatewayProvider {
        register<FakeTilgangGateway>()
        register<FakeForeldrepengerGateway>()
    }

    @BeforeEach
    fun reset() = FakeForeldrepengerGateway.reset()

    @Test
    fun `returnerer foreldrepengeperioder for person`() {
        val ident = "22345678901"
        InMemoryPersonRepository.finnEllerOpprett(listOf(Ident(ident)))

        val tom = LocalDate.of(2026, 7, 1)
        val fom = tom.minusWeeks(52)
        val foreldrepengeperiode = Periode(tom.minusWeeks(30), tom.minusWeeks(20))

        FakeForeldrepengerGateway.ytelserPerIdent = mapOf(
            ident to listOf(
                ytelse(Ytelser.FORELDREPENGER, foreldrepengeperiode, utbetalingsgrad = 80, beløp = 1234)
            )
        )

        testApplication {
            installApplication {
                foreldrepengeperioderApi(MockDataSource(), inMemoryRepositoryRegistry, gatewayProvider)
            }

            val response = createClient().post(FORELDREPENGEPERIODER_PATH) {
                header("Authorization", "Bearer ${getToken().token()}")
                contentType(ContentType.Application.Json)
                setBody(YtelseoppslagRequest(personident = ident, fom = fom, tom = tom))
            }

            assertThat(response.status).isEqualTo(HttpStatusCode.OK)

            val body = response.body<ForeldrepengeperioderDTO>()
            assertThat(body.oppslagsperiode.fom).isEqualTo(fom)
            assertThat(body.oppslagsperiode.tom).isEqualTo(tom)
            assertThat(FakeForeldrepengerGateway.periodeBrukt).isEqualTo(fom to tom)
            assertThat(body.perioder).hasSize(1)
            assertThat(body.perioder.first().fom).isEqualTo(foreldrepengeperiode.fom)
            assertThat(body.perioder.first().tom).isEqualTo(foreldrepengeperiode.tom)
            assertThat(body.perioder.first().utbetalingsgrad.toInt()).isEqualTo(80)
            assertThat(body.perioder.first().beløp?.toInt()).isEqualTo(1234)
            assertThat(body.perioder.first().saksnummer).isEqualTo("352017890")
        }
    }

    @Test
    fun `filtrerer bort andre ytelser enn foreldrepenger`() {
        val ident = "22345678903"
        InMemoryPersonRepository.finnEllerOpprett(listOf(Ident(ident)))

        val periode = Periode(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 4, 1))
        FakeForeldrepengerGateway.ytelserPerIdent = mapOf(
            ident to listOf(ytelse(Ytelser.PLEIEPENGER_SYKT_BARN, periode, utbetalingsgrad = 100, beløp = null))
        )

        testApplication {
            installApplication {
                foreldrepengeperioderApi(MockDataSource(), inMemoryRepositoryRegistry, gatewayProvider)
            }

            val response = createClient().post(FORELDREPENGEPERIODER_PATH) {
                header("Authorization", "Bearer ${getToken().token()}")
                contentType(ContentType.Application.Json)
                setBody(oppslag(ident))
            }

            assertThat(response.status).isEqualTo(HttpStatusCode.OK)
            assertThat(response.body<ForeldrepengeperioderDTO>().perioder).isEmpty()
        }
    }

    @Test
    fun `slår opp både aktiv og historisk ident`() {
        val aktivIdent = "22345678902"
        val historiskIdent = "20987654322"
        InMemoryPersonRepository.finnEllerOpprett(
            listOf(Ident(historiskIdent, aktivIdent = false), Ident(aktivIdent))
        )

        testApplication {
            installApplication {
                foreldrepengeperioderApi(MockDataSource(), inMemoryRepositoryRegistry, gatewayProvider)
            }

            val response = createClient().post(FORELDREPENGEPERIODER_PATH) {
                header("Authorization", "Bearer ${getToken().token()}")
                contentType(ContentType.Application.Json)
                setBody(oppslag(aktivIdent))
            }

            assertThat(response.status).isEqualTo(HttpStatusCode.OK)
            assertThat(FakeForeldrepengerGateway.identerBrukt)
                .containsExactlyInAnyOrder(aktivIdent, historiskIdent)
        }
    }

    @Test
    fun `ukjent person gir tom liste`() {
        testApplication {
            installApplication {
                foreldrepengeperioderApi(MockDataSource(), inMemoryRepositoryRegistry, gatewayProvider)
            }

            val ukjentIdent = "22345678909"
            val response = createClient().post(FORELDREPENGEPERIODER_PATH) {
                header("Authorization", "Bearer ${getToken().token()}")
                contentType(ContentType.Application.Json)
                setBody(oppslag(ukjentIdent))
            }

            assertThat(response.status).isEqualTo(HttpStatusCode.OK)
            assertThat(response.body<ForeldrepengeperioderDTO>().perioder).isEmpty()
            assertThat(FakeForeldrepengerGateway.identerBrukt).containsExactly(ukjentIdent)
        }
    }

    private fun oppslag(personident: String) = YtelseoppslagRequest(
        personident = personident,
        fom = LocalDate.of(2026, 1, 1),
        tom = LocalDate.of(2026, 7, 1),
    )

    private fun ytelse(type: Ytelser, periode: Periode, utbetalingsgrad: Number, beløp: Number?) = Ytelse(
        ytelse = type,
        saksnummer = "352017890",
        kildesystem = "FPSAK",
        ytelseStatus = "AVSLUTTET",
        vedtattTidspunkt = periode.fom.minusWeeks(1),
        anvist = listOf(
            Anvist(
                periode = periode,
                utbetalingsgrad = Utbetalingsgrad(utbetalingsgrad),
                beløp = beløp,
            )
        )
    )
}


