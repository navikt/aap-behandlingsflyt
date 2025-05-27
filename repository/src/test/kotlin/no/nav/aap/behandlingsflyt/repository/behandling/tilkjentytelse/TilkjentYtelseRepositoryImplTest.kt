package no.nav.aap.behandlingsflyt.repository.behandling.tilkjentytelse

import no.nav.aap.behandlingsflyt.behandling.søknad.TrukketSøknadService
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.Tilkjent
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.TilkjentGradering
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.TilkjentYtelsePeriode
import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.repository.avklaringsbehov.AvklaringsbehovRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.avklaringsbehov.FakePdlGateway
import no.nav.aap.behandlingsflyt.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.søknad.TrukketSøknadRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.PersonRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.SakRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonOgSakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.test.ident
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryAvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryTrukketSøknadRepository
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.komponenter.verdityper.GUnit
import no.nav.aap.komponenter.verdityper.Prosent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

class TilkjentYtelseRepositoryImplTest {
    @Test
    fun `kan lagre og hente tilkjentYtelse`() {
        InitTestDatabase.freshDatabase().transaction { connection ->
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
                            gradering = TilkjentGradering(Prosent.`66_PROSENT`, Prosent.`0_PROSENT`, Prosent.`0_PROSENT`, Prosent.`0_PROSENT`, Prosent.`0_PROSENT`),
                            barnetillegg = Beløp(999),
                            grunnlagsfaktor = GUnit("1.0"),
                            grunnlag = Beløp(999),
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
                            gradering = TilkjentGradering(Prosent.`50_PROSENT`, Prosent.`0_PROSENT`, Prosent.`0_PROSENT`, Prosent.`0_PROSENT`, Prosent.`0_PROSENT`),
                            barnetillegg = Beløp(1000),
                            grunnlagsfaktor = GUnit("1.0"),
                            grunnlag = Beløp(1000),
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
            assertEquals(tilkjentYtelse, tilkjentYtelseHentet)
            // Dobbeltsjekke at vi avrunder redusert dagsats til nærmeste heltall
            assertThat(tilkjentYtelse.first().tilkjent.redusertDagsats()).isEqualTo(Beløp(BigDecimal("1319")))
        }

    }

    @Test
    fun `finner ingen tilkjentYtelse hvis den ikke eksisterer`() {
        InitTestDatabase.freshDatabase().transaction { connection ->
            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)

            val tilkjentYtelseRepository = TilkjentYtelseRepositoryImpl(connection)
            val tilkjentYtelseHentet = tilkjentYtelseRepository.hentHvisEksisterer(behandling.id)
            assertNull(tilkjentYtelseHentet)
        }
    }

    private companion object {
        private val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))
    }


    private fun sak(connection: DBConnection): Sak {
        return PersonOgSakService(
            FakePdlGateway,
            PersonRepositoryImpl(connection),
            SakRepositoryImpl(connection),
            BehandlingRepositoryImpl(connection),
            TrukketSøknadService(
                AvklaringsbehovRepositoryImpl(connection),
                TrukketSøknadRepositoryImpl(connection)
            ),
        ).finnEllerOpprett(
            ident(),
            periode
        )
    }
}