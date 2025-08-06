package no.nav.aap.behandlingsflyt.behandling.tilkjentytelse

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import no.nav.aap.behandlingsflyt.BaseApiTest
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.ArbeidIPeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.Meldekort
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.test.Fakes
import no.nav.aap.behandlingsflyt.test.MockDataSource
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryBehandlingRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryMeldekortRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryMeldeperiodeRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryTilkjentYtelseRepository
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.komponenter.verdityper.GUnit
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.komponenter.verdityper.TimerArbeid
import no.nav.aap.verdityper.dokument.JournalpostId
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
        .register<InMemoryMeldekortRepository>()
        .register<InMemoryMeldeperiodeRepository>()

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
                        gradering = TilkjentGradering(Prosent(50), Prosent(50), Prosent(50), Prosent(50), Prosent(30), Prosent(0)),
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

            val response =
                sendGetRequest(behandling.id, "/api/behandling/tilkjent/${behandling.referanse.referanse}")
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

    @Test
    fun `teste v2`() {
        val ds = MockDataSource()
        val behandling = opprettBehandling(nySak(), TypeBehandling.Revurdering)

        val tilkjentYtelseVerdi = Tilkjent(
            dagsats = Beløp(500),
            gradering = TilkjentGradering(
                endeligGradering = Prosent(50),
                samordningGradering = Prosent(50),
                institusjonGradering = Prosent(50),
                arbeidGradering = Prosent(50),
                samordningUføregradering = Prosent(30),
                samordningArbeidsgiverGradering = null,
            ),
            grunnlag = Beløp(10000),
            grunnlagsfaktor = GUnit("1.5"),
            grunnbeløp = Beløp(106399),
            antallBarn = 2,
            barnetilleggsats = Beløp(150),
            barnetillegg = Beløp(300),
            utbetalingsdato = LocalDate.parse("2025-03-08"),
        )
        InMemoryTilkjentYtelseRepository.lagre(
            behandling.id, tilkjent = listOf(
                TilkjentYtelsePeriode(
                    Periode(
                        fom = LocalDate.parse("2025-03-07"),
                        tom = LocalDate.parse("2025-03-21"),
                    ),
                    tilkjent = tilkjentYtelseVerdi
                ),
                TilkjentYtelsePeriode(
                    Periode(
                        fom = LocalDate.parse("2025-03-26"),
                        tom = LocalDate.parse("2025-04-10"),
                    ),
                    tilkjent = tilkjentYtelseVerdi.copy(dagsats = Beløp(400))
                ),
                TilkjentYtelsePeriode(
                    Periode(
                        fom = LocalDate.parse("2025-03-22"),
                        tom = LocalDate.parse("2025-03-25"),
                    ),
                    tilkjent = tilkjentYtelseVerdi.copy(dagsats = Beløp(300))
                )
            )
        )

        InMemoryMeldeperiodeRepository.lagre(
            behandling.id,
            listOf(
                Periode(LocalDate.parse("2025-03-07"), LocalDate.parse("2025-03-21")),
                Periode(LocalDate.parse("2025-03-22"), LocalDate.parse("2025-04-10"))
            )
        )

        InMemoryMeldekortRepository.lagre(
            behandling.id, setOf(
                Meldekort(
                    journalpostId = JournalpostId("123456789"),
                    timerArbeidPerPeriode = setOf(
                        ArbeidIPeriode(
                            Periode(
                                LocalDate.parse("2025-03-07"),
                                LocalDate.parse("2025-03-21")
                            ), TimerArbeid(BigDecimal("10.0"))
                        )
                    ),
                    mottattTidspunkt = LocalDate.parse("2025-03-07").atTime(9, 0)
                ),
                Meldekort(
                    journalpostId = JournalpostId("1234567810"),
                    timerArbeidPerPeriode = setOf(
                        ArbeidIPeriode(
                            Periode(
                                LocalDate.parse("2025-03-07"),
                                LocalDate.parse("2025-03-21")
                            ), TimerArbeid(BigDecimal("11.0"))
                        )
                    ),
                    mottattTidspunkt = LocalDate.parse("2025-03-07").atTime(10, 0)
                )
            )
        )

        testApplication {
            installApplication {
                tilkjentYtelseAPI(ds, repositoryRegistry)
            }
            val response =
                sendGetRequest(
                    behandling.id,
                    "/api/behandling/tilkjentV2/${behandling.referanse.referanse}"
                )
            assertThat(response.status).isEqualTo(HttpStatusCode.OK)

            assertThat(response.body<TilkjentYtelse2Dto>()).usingRecursiveComparison().isEqualTo(
                TilkjentYtelse2Dto(
                    perioder = listOf(
                        TilkjentYtelsePeriode2Dto(
                            meldeperiode = Periode(LocalDate.parse("2025-03-07"), LocalDate.parse("2025-03-21")),
                            levertMeldekortDato = LocalDate.parse("2025-03-07"),
                            sisteLeverteMeldekort = MeldekortDto(
                                timerArbeidPerPeriode = ArbeidIPeriodeDto(11.0),
                                mottattTidspunkt = LocalDate.parse("2025-03-07").atTime(10, 0),
                            ),
                            meldekortStatus = null,
                            vurdertePerioder = listOf(
                                VurdertPeriode(
                                    periode = Periode(LocalDate.parse("2025-03-07"), LocalDate.parse("2025-03-21")),
                                    felter = Felter(
                                        dagsats = 500.0,
                                        barneTilleggsats = 300.00,
                                        arbeidGradering = 50,
                                        samordningGradering = 80,
                                        institusjonGradering = 50,
                                        totalReduksjon = 50,
                                        effektivDagsats = 400.0
                                    )
                                )
                            ),
                        ),
                        TilkjentYtelsePeriode2Dto(
                            meldeperiode = Periode(LocalDate.parse("2025-03-22"), LocalDate.parse("2025-04-10")),
                            levertMeldekortDato = null,
                            sisteLeverteMeldekort = null,
                            meldekortStatus = null,
                            vurdertePerioder = listOf(
                                VurdertPeriode(
                                    periode = Periode(LocalDate.parse("2025-03-22"), LocalDate.parse("2025-03-25")),
                                    felter = Felter(
                                        dagsats = 300.0,
                                        barneTilleggsats = 300.00,
                                        arbeidGradering = 50,
                                        samordningGradering = 80,
                                        institusjonGradering = 50,
                                        totalReduksjon = 50,
                                        effektivDagsats = 300.0
                                    )
                                ),
                                VurdertPeriode(
                                    periode = Periode(LocalDate.parse("2025-03-26"), LocalDate.parse("2025-04-10")),
                                    felter = Felter(
                                        dagsats = 400.0,
                                        barneTilleggsats = 300.00,
                                        arbeidGradering = 50,
                                        samordningGradering = 80,
                                        institusjonGradering = 50,
                                        totalReduksjon = 50,
                                        effektivDagsats = 350.0
                                    )
                                )
                            ),
                        )
                    ),
                )
            )
        }

    }

    private suspend fun ApplicationTestBuilder.sendGetRequest(
        payload: Any,
        path: String
    ): HttpResponse {
        val client = createClient()
        val jwt = issueToken("nav:aap:afpoffentlig.read")
        return client.get(path) {
            header("Authorization", "Bearer ${jwt.serialize()}")
            header("X-callid", UUID.randomUUID().toString())
            contentType(ContentType.Application.Json)
            setBody(payload)
        }
    }
}