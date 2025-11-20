package no.nav.aap.behandlingsflyt.repository.behandling.tilkjentytelse

import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.Tilkjent
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.TilkjentGradering
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.TilkjentYtelsePeriode
import no.nav.aap.behandlingsflyt.help.FakePdlGateway
import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.repository.sak.PersonRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.SakRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonOgSakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.test.ident
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.komponenter.verdityper.GUnit
import no.nav.aap.komponenter.verdityper.Prosent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.math.BigDecimal
import java.time.LocalDate

class TilkjentYtelseRepositoryImplTest {
    private lateinit var dataSource: TestDataSource

    @BeforeEach
    fun setUp() {
        dataSource = TestDataSource()
    }

    @AfterEach
    fun tearDown() {
        dataSource.close()
    }

    @Test
    fun `kan lagre og hente tilkjentYtelse`() {
        dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)

            val tilkjentYtelseRepository = TilkjentYtelseRepositoryImpl(connection)
            val tilkjentYtelse =
                listOf(
                    TilkjentYtelsePeriode(
                        periode = Periode(
                            LocalDate.now(),
                            LocalDate.now().plusDays(1)
                        ),
                        tilkjent = Tilkjent(
                            dagsats = Beløp(999),
                            gradering = TilkjentGradering(
                                Prosent.`66_PROSENT`,
                                Prosent.`0_PROSENT`,
                                Prosent.`0_PROSENT`,
                                Prosent.`0_PROSENT`,
                                Prosent.`0_PROSENT`,
                                Prosent.`0_PROSENT`
                            ),
                            barnetillegg = Beløp(999),
                            grunnlagsfaktor = GUnit("1.0"),
                            antallBarn = 1,
                            barnetilleggsats = Beløp(999),
                            grunnbeløp = Beløp(1000),
                            utbetalingsdato = LocalDate.now().plusDays(14)
                        )
                    ),
                    TilkjentYtelsePeriode(
                        periode = Periode(
                            LocalDate.now().plusDays(2),
                            LocalDate.now().plusDays(3)
                        ),
                        tilkjent = Tilkjent(
                            dagsats = Beløp(1000),
                            gradering = TilkjentGradering(
                                Prosent.`50_PROSENT`,
                                Prosent.`0_PROSENT`,
                                Prosent.`0_PROSENT`,
                                Prosent.`0_PROSENT`,
                                Prosent.`0_PROSENT`,
                                Prosent.`0_PROSENT`
                            ),
                            barnetillegg = Beløp(1000),
                            grunnlagsfaktor = GUnit("1.0"),
                            antallBarn = 1,
                            barnetilleggsats = Beløp(1000),
                            grunnbeløp = Beløp(1000),
                            utbetalingsdato = LocalDate.now().plusDays(14)
                        )
                    ),
                )

            tilkjentYtelseRepository.lagre(behandling.id, tilkjentYtelse)
            val tilkjentYtelseHentet = tilkjentYtelseRepository.hentHvisEksisterer(behandling.id)
            assertNotNull(tilkjentYtelseHentet)
            assertThat(tilkjentYtelseHentet).isEqualTo(tilkjentYtelse)
            // Dobbeltsjekke at vi avrunder redusert dagsats til nærmeste heltall
            assertThat(tilkjentYtelse.first().tilkjent.redusertDagsats()).isEqualTo(Beløp(BigDecimal("1319")))
        }

    }

    @Test
    fun `finner ingen tilkjentYtelse hvis den ikke eksisterer`() {
        dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)

            val tilkjentYtelseRepository = TilkjentYtelseRepositoryImpl(connection)
            val tilkjentYtelseHentet = tilkjentYtelseRepository.hentHvisEksisterer(behandling.id)
            assertNull(tilkjentYtelseHentet)
        }
    }

    @Test
    fun `test sletting`() {
        dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)
            val tilkjentYtelseRepository = TilkjentYtelseRepositoryImpl(connection)
            tilkjentYtelseRepository.lagre(
                behandling.id, listOf(
                    TilkjentYtelsePeriode(
                        periode = Periode(
                            LocalDate.now(),
                            LocalDate.now().plusDays(1)
                        ),
                        tilkjent = Tilkjent(
                            dagsats = Beløp(999),
                            gradering = TilkjentGradering(
                                Prosent.`66_PROSENT`,
                                Prosent.`0_PROSENT`,
                                Prosent.`0_PROSENT`,
                                Prosent.`0_PROSENT`,
                                Prosent.`0_PROSENT`,
                                Prosent.`0_PROSENT`
                            ),
                            barnetillegg = Beløp(999),
                            grunnlagsfaktor = GUnit("1.0"),
                            antallBarn = 1,
                            barnetilleggsats = Beløp(999),
                            grunnbeløp = Beløp(1000),
                            utbetalingsdato = LocalDate.now().plusDays(14)
                        )
                    ),
                    TilkjentYtelsePeriode(
                        periode = Periode(
                            LocalDate.now().plusDays(2),
                            LocalDate.now().plusDays(3)
                        ),
                        tilkjent = Tilkjent(
                            dagsats = Beløp(1000),
                            gradering = TilkjentGradering(
                                Prosent.`50_PROSENT`,
                                Prosent.`0_PROSENT`,
                                Prosent.`0_PROSENT`,
                                Prosent.`0_PROSENT`,
                                Prosent.`0_PROSENT`,
                                Prosent.`0_PROSENT`
                            ),
                            barnetillegg = Beløp(1000),
                            grunnlagsfaktor = GUnit("1.0"),
                            antallBarn = 1,
                            barnetilleggsats = Beløp(1000),
                            grunnbeløp = Beløp(1000),
                            utbetalingsdato = LocalDate.now().plusDays(14)
                        )
                    ),
                )

            )
            tilkjentYtelseRepository.lagre(
                behandling.id, listOf(
                    TilkjentYtelsePeriode(
                        periode = Periode(
                            LocalDate.now(),
                            LocalDate.now().plusDays(1)
                        ),
                        tilkjent = Tilkjent(
                            dagsats = Beløp(999),
                            gradering = TilkjentGradering(
                                Prosent.`66_PROSENT`,
                                Prosent.`0_PROSENT`,
                                Prosent.`0_PROSENT`,
                                Prosent.`0_PROSENT`,
                                Prosent.`0_PROSENT`,
                                Prosent.`0_PROSENT`
                            ),
                            barnetillegg = Beløp(999),
                            grunnlagsfaktor = GUnit("1.0"),
                            antallBarn = 1,
                            barnetilleggsats = Beløp(999),
                            grunnbeløp = Beløp(1000),
                            utbetalingsdato = LocalDate.now().plusDays(14)
                        )
                    ),
                    TilkjentYtelsePeriode(
                        periode = Periode(
                            LocalDate.now().plusDays(4),
                            LocalDate.now().plusDays(5)
                        ),
                        tilkjent = Tilkjent(
                            dagsats = Beløp(1000),
                            gradering = TilkjentGradering(
                                Prosent.`50_PROSENT`,
                                Prosent.`0_PROSENT`,
                                Prosent.`0_PROSENT`,
                                Prosent.`0_PROSENT`,
                                Prosent.`0_PROSENT`,
                                Prosent.`0_PROSENT`
                            ),
                            barnetillegg = Beløp(1000),
                            grunnlagsfaktor = GUnit("1.0"),
                            antallBarn = 1,
                            barnetilleggsats = Beløp(1000),
                            grunnbeløp = Beløp(1000),
                            utbetalingsdato = LocalDate.now().plusDays(14)
                        )
                    ),
                )
            )
            assertDoesNotThrow { tilkjentYtelseRepository.slett(behandling.id) }
        }
    }

    private companion object {
        private val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))
    }


    private fun sak(connection: DBConnection): Sak {
        return PersonOgSakService(
            FakePdlGateway,
            PersonRepositoryImpl(connection),
            SakRepositoryImpl(connection)
        ).finnEllerOpprett(
            ident(),
            periode
        )
    }
}