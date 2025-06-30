package no.nav.aap.behandlingsflyt.behandling.tilkjentytelse

import com.nimbusds.jwt.SignedJWT
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import no.nav.aap.behandlingsflyt.BaseApiTest
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.test.Fakes
import no.nav.aap.behandlingsflyt.test.MockDataSource
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryBehandlingRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryTilkjentYtelseRepository
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.komponenter.verdityper.GUnit
import no.nav.aap.komponenter.verdityper.Prosent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

@Fakes
@ExtendWith(BaseApiTest::class)
class TilkjentYtelseAPITest : BaseApiTest() {
    private val repositoryRegistry = RepositoryRegistry()
        .register<InMemoryTilkjentYtelseRepository>()
        .register<InMemoryBehandlingRepository>()

    @Test
    fun `hente ut tilkjent ytelse fra API`() {
        val ds = MockDataSource()
        val behandling = opprettBehandling(nySak(), TypeBehandling.Revurdering)

        InMemoryTilkjentYtelseRepository.lagre(
            behandling.id, tilkjent = listOf(
                TilkjentYtelsePeriode(
                    Periode(
                        fom = LocalDate.parse("2025-03-07"),
                        tom = LocalDate.parse("2026-03-07"),
                    ),
                    tilkjent = Tilkjent(
                        dagsats = Beløp(500),
                        gradering = TilkjentGradering(Prosent(50), Prosent(50), Prosent(50), Prosent(50), Prosent(30)),
                        grunnlag = Beløp(10000),
                        grunnlagsfaktor = GUnit("1.5"),
                        grunnbeløp = Beløp(106399),
                        antallBarn = 2,
                        barnetilleggsats = Beløp(150),
                        barnetillegg = Beløp(300),
                        utbetalingsdato = LocalDate.parse("2025-03-08"),
                    )
                )
            )
        )

        testApplication {
            installApplication {
                tilkjentYtelseAPI(ds, repositoryRegistry)
            }
            val jwt = issueToken("nav:aap:afpoffentlig.read")

            val client = createClient()

            val response =
                sendGetRequest(client, jwt, behandling.id, "/api/behandling/tilkjent/${behandling.referanse.referanse}")
            assertThat(response.status).isEqualTo(HttpStatusCode.OK)

            assertThat(response.body<TilkjentYtelseDto>()).isEqualTo(
                TilkjentYtelseDto(
                    perioder = listOf(
                        TilkjentYtelsePeriodeDTO(
                            fraOgMed = LocalDate.parse("2025-03-07"),
                            tilOgMed = LocalDate.parse("2026-03-07"),
                            dagsats = BigDecimal("500.00"),
                            gradering = 50,
                            grunnlag = BigDecimal("10000.00"),
                            grunnlagsfaktor = BigDecimal("1.5000000000"),
                            grunnbeløp = BigDecimal("106399.00"),
                            antallBarn = 2,
                            barnetilleggsats = BigDecimal("150.00"),
                            barnetillegg = BigDecimal("300.00"),
                            utbetalingsdato = LocalDate.parse("2025-03-08"),
                            redusertDagsats = 400.0,
                            arbeidGradering = 50,
                            institusjonGradering = 50,
                            samordningGradering = 50,
                            samordningUføreGradering = 30
                        )
                    )
                )
            )
        }
    }

    private suspend fun sendGetRequest(
        client: HttpClient,
        jwt: SignedJWT,
        payload: Any,
        path: String
    ) = client.get(path) {
        header("Authorization", "Bearer ${jwt.serialize()}")
        header("X-callid", UUID.randomUUID().toString())
        contentType(ContentType.Application.Json)
        setBody(payload)
    }
}