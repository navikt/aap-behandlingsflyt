package no.nav.aap.behandlingsflyt.behandling.tilkjentytelse

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import no.nav.aap.behandlingsflyt.BaseApiTest
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.ArbeidIPeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.Meldekort
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.FastsettMeldeperiodeSteg
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
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

@Fakes
class TilkjentYtelseAPITest : BaseApiTest() {
    private val repositoryRegistry = RepositoryRegistry()
        .register<InMemoryTilkjentYtelseRepository>()
        .register<InMemoryBehandlingRepository>()
        .register<InMemoryMeldekortRepository>()
        .register<InMemoryMeldeperiodeRepository>()

    @Test
    fun `teste v2`() {
        val ds = MockDataSource()
        val sak = nySak(LocalDate.parse("2025-08-06"))
        val behandling = opprettBehandling(sak, TypeBehandling.Revurdering)

        testApplication {
            installApplication {
                tilkjentYtelseAPI(ds, repositoryRegistry)
            }
            val rettighetsperiode = sak.rettighetsperiode

            val perioder = FastsettMeldeperiodeSteg.utledMeldeperiode(
                listOf(),
                rettighetsperiode
            ).take(3)

            InMemoryMeldeperiodeRepository.lagre(
                behandling.id,
                perioder
            )

            println("PERIODER $perioder")

            val tilkjentYtelseVerdi = Tilkjent(
                dagsats = Beløp(500),
                gradering = TilkjentGradering(
                    endeligGradering = Prosent(50),
                    samordningGradering = Prosent(50),
                    institusjonGradering = Prosent(50),
                    arbeidGradering = Prosent(50),
                    samordningUføregradering = Prosent(30),
                    samordningArbeidsgiverGradering = Prosent(50),
                ),
                grunnlagsfaktor = GUnit("1.5"),
                grunnbeløp = Beløp(106399),
                antallBarn = 2,
                barnetilleggsats = Beløp(36),
                barnetillegg = Beløp(72),
                utbetalingsdato = rettighetsperiode.fom
            )

            val tilkjentYtelsePerioder = perioder.mapIndexed { index, periode ->
                TilkjentYtelsePeriode(
                    Periode(
                        // TODO, begrens til rettighetsperioden
                        fom = periode.fom,
                        tom = periode.tom,
                    ),
                    tilkjent = tilkjentYtelseVerdi.copy(dagsats = Beløp(400 + index * 100.00.toLong()))
                )
            }

            InMemoryTilkjentYtelseRepository.lagre(
                behandling.id, tilkjent = tilkjentYtelsePerioder
            )

            InMemoryMeldekortRepository.lagre(
                behandling.id, setOf(
                    Meldekort(
                        journalpostId = JournalpostId("123456789"),
                        timerArbeidPerPeriode = setOf(
                            ArbeidIPeriode(
                                Periode(
                                    rettighetsperiode.fom,
                                    rettighetsperiode.fom.plusWeeks(2),
                                ),
                                TimerArbeid(BigDecimal("10.0"))
                            )
                        ),
                        mottattTidspunkt = LocalDate.parse("2025-08-07").atTime(9, 0)
                    ),
                    Meldekort(
                        journalpostId = JournalpostId("1234567810"),
                        timerArbeidPerPeriode = setOf(
                            ArbeidIPeriode(
                                Periode(
                                    rettighetsperiode.fom,
                                    rettighetsperiode.fom.plusWeeks(2),
                                ),
                                TimerArbeid(BigDecimal("11.0"))
                            )
                        ),
                        mottattTidspunkt = LocalDate.parse("2025-08-07").atTime(10, 0)
                    ),
                )
            )

            val respons =
                sendGetRequest(
                    behandling.id,
                    "/api/behandling/tilkjentV2/${behandling.referanse.referanse}"
                )
            assertThat(respons.status).isEqualTo(HttpStatusCode.OK)

            val actual = respons.body<TilkjentYtelse2Dto>()
            assertThat(actual).usingRecursiveComparison().isEqualTo(
                TilkjentYtelse2Dto(
                    perioder = listOf(
                        TilkjentYtelsePeriode2Dto(
                            meldeperiode = Periode(
                                rettighetsperiode.fom.minusDays(2),
                                rettighetsperiode.fom.plusWeeks(2).minusDays(3)
                            ),
                            levertMeldekortDato = LocalDate.parse("2025-08-07"),
                            sisteLeverteMeldekort = MeldekortDto(
                                timerArbeidPerPeriode = ArbeidIPeriodeDto(11.0),
                                mottattTidspunkt = LocalDate.parse("2025-08-07").atTime(10, 0),
                            ),
                            meldekortStatus = null,
                            vurdertePerioder = listOf(
                                VurdertPeriode(
                                    periode = Periode(
                                        rettighetsperiode.fom.minusDays(2),
                                        rettighetsperiode.fom.plusWeeks(2).minusDays(3)
                                    ),
                                    felter = Felter(
                                        dagsats = 400.0,
                                        barneTilleggsats = 36.00,
                                        arbeidGradering = 50,
                                        samordningGradering = 80,
                                        institusjonGradering = 50,
                                        totalReduksjon = 50,
                                        effektivDagsats = 236.0,
                                        arbeidsgiverGradering = 50
                                    )
                                )
                            ),
                        ),
                        TilkjentYtelsePeriode2Dto(
                            meldeperiode = Periode(
                                rettighetsperiode.fom.plusWeeks(2).minusDays(2),
                                rettighetsperiode.fom.plusWeeks(4).minusDays(3)
                            ),
                            levertMeldekortDato = rettighetsperiode.fom.plusDays(1),
                            sisteLeverteMeldekort = MeldekortDto(
                                timerArbeidPerPeriode = ArbeidIPeriodeDto(11.0),
                                mottattTidspunkt = rettighetsperiode.fom.plusDays(1).atTime(10, 0),
                            ),
                            meldekortStatus = null,
                            vurdertePerioder = listOf(
                                VurdertPeriode(
                                    periode = Periode(
                                        rettighetsperiode.fom.plusWeeks(2).minusDays(2),
                                        rettighetsperiode.fom.plusWeeks(4).minusDays(3)
                                    ),
                                    felter = Felter(
                                        dagsats = 500.0,
                                        barneTilleggsats = 36.00,
                                        arbeidGradering = 50,
                                        samordningGradering = 80,
                                        institusjonGradering = 50,
                                        totalReduksjon = 50,
                                        effektivDagsats = 286.0,
                                        arbeidsgiverGradering = 50
                                    )
                                )
                            ),
                        ),
                        TilkjentYtelsePeriode2Dto(
                            meldeperiode = Periode(
                                rettighetsperiode.fom.plusWeeks(4).minusDays(2),
                                rettighetsperiode.fom.plusWeeks(6).minusDays(3)
                            ),
                            levertMeldekortDato = null,
                            sisteLeverteMeldekort = null,
                            meldekortStatus = null,
                            vurdertePerioder = listOf(
                                VurdertPeriode(
                                    periode = Periode(
                                        rettighetsperiode.fom.plusWeeks(4).minusDays(2),
                                        rettighetsperiode.fom.plusWeeks(6).minusDays(3)
                                    ),
                                    felter = Felter(
                                        dagsats = 600.0,
                                        barneTilleggsats = 36.00,
                                        arbeidGradering = 50,
                                        samordningGradering = 80,
                                        institusjonGradering = 50,
                                        totalReduksjon = 50,
                                        effektivDagsats = 336.0,
                                        arbeidsgiverGradering = 50
                                    )
                                )
                            ),
                        ),
                    )
                )
            )
        }

    }

    private suspend fun ApplicationTestBuilder.sendGetRequest(
        payload: Any,
        path: String
    ): HttpResponse {
        val client = createClient()
        return client.get(path) {
            header("Authorization", "Bearer ${getToken().token()}")
            header("X-callid", UUID.randomUUID().toString())
            contentType(ContentType.Application.Json)
            setBody(payload)
        }
    }
}