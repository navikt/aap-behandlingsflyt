package no.nav.aap.behandlingsflyt.behandling.tilkjentytelse

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import no.nav.aap.behandlingsflyt.BaseApiTest
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.meldeperiode.MeldeperiodeUtleder
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.ArbeidIPeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.Meldekort
import no.nav.aap.behandlingsflyt.help.tomtTilkjentYtelseGrunnlag
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.test.Fakes
import no.nav.aap.behandlingsflyt.test.MockDataSource
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryMeldekortRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryMeldeperiodeRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryTilkjentYtelseRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.inMemoryRepositoryRegistry
import no.nav.aap.behandlingsflyt.utils.diff.Endret
import no.nav.aap.behandlingsflyt.utils.diff.Fjernet
import no.nav.aap.behandlingsflyt.utils.diff.LagtTil
import no.nav.aap.behandlingsflyt.utils.diff.Uendret
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
class TilkjentYtelseApiTest : BaseApiTest() {

    @Test
    fun `teste v2`() {
        val ds = MockDataSource()
        val sak = nySak(LocalDate.parse("2025-08-06"))
        val behandling = opprettBehandling(sak, TypeBehandling.Revurdering)

        testApplication {
            installApplication {
                tilkjentYtelseApi(ds, inMemoryRepositoryRegistry)
            }
            val rettighetsperiode = sak.rettighetsperiode

            val perioder = MeldeperiodeUtleder.utledMeldeperiode(
                null,
                rettighetsperiode
            ).take(3)

            InMemoryMeldeperiodeRepository.lagreFørsteMeldeperiode(
                behandling.id,
                perioder.first()
            )

            val tilkjentYtelseVerdi = lagTilkjentYtelse(rettighetsperiode.fom)

            val tilkjentYtelsePerioder = perioder.mapIndexed { index, periode ->
                lagTilkjentYtelsePeriode(periode, tilkjentYtelseVerdi, index)
            }

            InMemoryTilkjentYtelseRepository.lagre(
                behandling.id,
                tilkjent = tilkjentYtelsePerioder,
                faktagrunnlag = tomtTilkjentYtelseGrunnlag,
                versjon = ""
            )

            InMemoryMeldekortRepository.lagre(
                behandling.id, opprettMeldekort(
                    Periode(
                        rettighetsperiode.fom,
                        rettighetsperiode.fom.plusWeeks(2),
                    )
                )
            )

            val respons =
                sendGetRequest(
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
                                    felter = felter(dagsats = 400.0, effektivDagsats = 236.0)
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
                                    felter = felter(500.0, effektivDagsats = 286.0)
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
                                    felter = felter(dagsats = 600.0, effektivDagsats = 336.0)
                                )
                            ),
                        ),
                    )
                )
            )
        }

    }

    @Test
    fun `tilkjent ytelse skal generere diff-objekter`() {
        val ds = MockDataSource()
        val sak = nySak(LocalDate.parse("2025-08-06"))
        val behandling = opprettBehandling(sak, TypeBehandling.Revurdering)
        val behandling2 = opprettBehandling(sak, TypeBehandling.Revurdering, forrigeBehandlingId = behandling.id)

        testApplication {
            installApplication {
                tilkjentYtelseApi(ds, inMemoryRepositoryRegistry)
            }
            val rettighetsperiodeB1 = sak.rettighetsperiode.flytt(14)
            val rettighetsperiodeB2 = sak.rettighetsperiode

            val perioderFørsteBehandling = MeldeperiodeUtleder.utledMeldeperiode(null, rettighetsperiodeB1).take(3)
            val perioderAndreBehandling = MeldeperiodeUtleder.utledMeldeperiode(null, rettighetsperiodeB2).take(3)

            InMemoryMeldeperiodeRepository.lagreFørsteMeldeperiode(behandling.id, perioderFørsteBehandling.first())
            InMemoryMeldeperiodeRepository.lagreFørsteMeldeperiode(behandling2.id, perioderAndreBehandling.first())

            val tilkjentYtelseVerdi = lagTilkjentYtelse(rettighetsperiodeB1.fom)

            val tilkjentYtelsePerioderFørsteBehandling = perioderFørsteBehandling.mapIndexed { index, periode ->
                lagTilkjentYtelsePeriode(
                    periode,
                    tilkjentYtelseVerdi,
                    (index + 1) * (index + 1)
                )  // Hack for at første periode skal bli uendret til neste behandling, men andre periode endres
            }

            val tilkjentYtelsePerioderAndreBehandling = perioderAndreBehandling.mapIndexed { index, periode ->
                lagTilkjentYtelsePeriode(periode, tilkjentYtelseVerdi, index)
            }

            InMemoryTilkjentYtelseRepository.lagre(
                behandling.id,
                tilkjent = tilkjentYtelsePerioderFørsteBehandling,
                faktagrunnlag = tomtTilkjentYtelseGrunnlag,
                versjon = ""
            )

            InMemoryTilkjentYtelseRepository.lagre(
                behandling2.id,
                tilkjent = tilkjentYtelsePerioderAndreBehandling,
                faktagrunnlag = tomtTilkjentYtelseGrunnlag,
                versjon = ""
            )

            val meldekortPeriode = Periode(
                rettighetsperiodeB1.fom,
                rettighetsperiodeB1.fom.plusWeeks(2),
            )
            InMemoryMeldekortRepository.lagre(
                behandling.id, opprettMeldekort(meldekortPeriode)
            )
            InMemoryMeldekortRepository.lagre(
                behandling2.id, opprettMeldekort(meldekortPeriode)
            )

            val responsBehandling1 =
                sendGetRequest(
                    "/api/behandling/tilkjent-med-diff/${behandling.referanse.referanse}"
                )
            assertThat(responsBehandling1.status).isEqualTo(HttpStatusCode.OK)

            val tilkjentYtelseResponseBehandling1 = responsBehandling1.body<TilkjentYtelse2MedDiffDto>()
            tilkjentYtelseResponseBehandling1.perioder.forEach { periode ->
                assertThat(periode is LagtTil).isTrue()
            }

            val responsBehandling2 =
                sendGetRequest(
                    "/api/behandling/tilkjent-med-diff/${behandling2.referanse.referanse}"
                )
            assertThat(responsBehandling2.status).isEqualTo(HttpStatusCode.OK)

            val tilkjentYtelseResponsBehandling2 = responsBehandling2.body<TilkjentYtelse2MedDiffDto>()
            assertThat(tilkjentYtelseResponsBehandling2).usingRecursiveComparison().isEqualTo(
                TilkjentYtelse2MedDiffDto(
                    perioder = listOf(
                        LagtTil(
                            TilkjentYtelsePeriode2Dto(
                                meldeperiode = Periode(
                                    meldekortPeriode.fom.minusDays(2).minusWeeks(2),
                                    meldekortPeriode.fom.minusDays(3)
                                ),
                                levertMeldekortDato = null,
                                sisteLeverteMeldekort = null,
                                meldekortStatus = null,
                                vurdertePerioder = listOf(
                                    VurdertPeriode(
                                        periode = Periode(
                                            meldekortPeriode.fom.minusDays(2).minusWeeks(2),
                                            meldekortPeriode.fom.minusDays(3)
                                        ),
                                        felter = felter(dagsats = 400.0, effektivDagsats = 236.0)
                                    )
                                ),
                            )
                        ),
                        Uendret(
                            TilkjentYtelsePeriode2Dto(
                                meldeperiode = Periode(
                                    meldekortPeriode.fom.minusDays(2),
                                    meldekortPeriode.fom.plusWeeks(2).minusDays(3)
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
                                            meldekortPeriode.fom.minusDays(2),
                                            meldekortPeriode.fom.plusWeeks(2).minusDays(3)
                                        ),
                                        felter = felter(dagsats = 500.0, effektivDagsats = 286.0)
                                    )
                                ),
                            )
                        ),
                        Endret(
                            fra =
                                TilkjentYtelsePeriode2Dto(
                                    meldeperiode = Periode(
                                        meldekortPeriode.fom.plusWeeks(2).minusDays(2),
                                        meldekortPeriode.fom.plusWeeks(4).minusDays(3)
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
                                                meldekortPeriode.fom.plusWeeks(2).minusDays(2),
                                                meldekortPeriode.fom.plusWeeks(4).minusDays(3)
                                            ),
                                            felter = felter(dagsats = 800.0, effektivDagsats = 436.0)
                                        )
                                    ),
                                ),
                            til = TilkjentYtelsePeriode2Dto(
                                meldeperiode = Periode(
                                    meldekortPeriode.fom.plusWeeks(2).minusDays(2),
                                    meldekortPeriode.fom.plusWeeks(4).minusDays(3)
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
                                            meldekortPeriode.fom.plusWeeks(2).minusDays(2),
                                            meldekortPeriode.fom.plusWeeks(4).minusDays(3)
                                        ),
                                        felter = felter(dagsats = 600.0, effektivDagsats = 336.0)
                                    )
                                ),
                            )

                        ),
                        Fjernet(
                            TilkjentYtelsePeriode2Dto(
                                meldeperiode = Periode(
                                    meldekortPeriode.fom.plusWeeks(4).minusDays(2),
                                    meldekortPeriode.fom.plusWeeks(6).minusDays(3)
                                ),
                                levertMeldekortDato = null,
                                sisteLeverteMeldekort = null,
                                meldekortStatus = null,
                                vurdertePerioder = listOf(
                                    VurdertPeriode(
                                        periode = Periode(
                                            meldekortPeriode.fom.plusWeeks(4).minusDays(2),
                                            meldekortPeriode.fom.plusWeeks(6).minusDays(3)
                                        ),
                                        felter = felter(dagsats = 1300.0, effektivDagsats = 686.0)
                                    )
                                ),
                            )
                        ),
                    )
                )
            )
        }

    }

    private fun felter(dagsats: Double, effektivDagsats: Double): Felter = Felter(
        dagsats = dagsats,
        barneTilleggsats = 36.00,
        barnetillegg = 72.00,
        arbeidGradering = 50,
        samordningGradering = 80,
        institusjonGradering = 50,
        totalReduksjon = 50,
        effektivDagsats = effektivDagsats,
        arbeidsgiverGradering = 50,
        barnepensjonDagsats = 0.0

    )

    private fun lagTilkjentYtelsePeriode(
        periode: Periode,
        tilkjentYtelseVerdi: Tilkjent,
        index: Int
    ): TilkjentYtelsePeriode = TilkjentYtelsePeriode(
        Periode(
            fom = periode.fom,
            tom = periode.tom,
        ),
        tilkjent = tilkjentYtelseVerdi.copy(
            dagsats = Beløp(400 + index * 100.00.toLong()),
            redusertDagsats = null
        )
    )

    private fun opprettMeldekort(meldekortPeriode: Periode): Set<Meldekort> = setOf(
        Meldekort(
            journalpostId = JournalpostId("123456789"),
            timerArbeidPerPeriode = setOf(
                ArbeidIPeriode(
                    meldekortPeriode,
                    TimerArbeid(BigDecimal("10.0"))
                )
            ),
            mottattTidspunkt = LocalDate.parse("2025-08-07").atTime(9, 0)
        ),
        Meldekort(
            journalpostId = JournalpostId("1234567810"),
            timerArbeidPerPeriode = setOf(
                ArbeidIPeriode(
                    meldekortPeriode,
                    TimerArbeid(BigDecimal("11.0"))
                )
            ),
            mottattTidspunkt = LocalDate.parse("2025-08-07").atTime(10, 0)
        ),
    )

    private fun lagTilkjentYtelse(utbetalingsdato: LocalDate): Tilkjent = Tilkjent(
        dagsats = Beløp(500),
        gradering = Prosent(50),
        graderingGrunnlag = GraderingGrunnlag(
            samordningGradering = Prosent(50),
            institusjonGradering = Prosent(50),
            arbeidGradering = Prosent(50),
            samordningUføregradering = Prosent(30),
            samordningArbeidsgiverGradering = Prosent(50),
            meldepliktGradering = Prosent(0),
        ),
        grunnlagsfaktor = GUnit("1.5"),
        grunnbeløp = Beløp(106399),
        barnepensjonDagsats = Beløp(0),
        antallBarn = 2,
        barnetilleggsats = Beløp(36),
        barnetillegg = Beløp(72),
        utbetalingsdato = utbetalingsdato,
        minsteSats = Minstesats.MINSTESATS_UNDER_25,
        redusertDagsats = Beløp(236)
    )

    private suspend fun ApplicationTestBuilder.sendGetRequest(
        path: String
    ): HttpResponse {
        val client = createClient()
        return client.get(path) {
            header("Authorization", "Bearer ${getToken().token()}")
            header("X-callid", UUID.randomUUID().toString())
            contentType(ContentType.Application.Json)
        }
    }
}