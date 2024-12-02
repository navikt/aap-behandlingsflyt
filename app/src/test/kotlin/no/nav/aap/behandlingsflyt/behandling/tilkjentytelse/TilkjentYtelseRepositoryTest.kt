package no.nav.aap.behandlingsflyt.behandling.tilkjentytelse

import kotlinx.coroutines.runBlocking
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.FakePdlGateway
import no.nav.aap.behandlingsflyt.test.ident
import no.nav.aap.behandlingsflyt.faktagrunnlag.SakOgBehandlingService
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Årsak
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonOgSakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.komponenter.verdityper.GUnit
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.ÅrsakTilBehandling
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.LocalDate

class TilkjentYtelseRepositoryTest {
    @Test
    fun `kan lagre og hente tilkjentYtelse`() {
        InitTestDatabase.dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = behandling(connection, sak)

            val tilkjentYtelseRepository = TilkjentYtelseRepository(connection)
            val tilkjentYtelse = Tidslinje(
                listOf(
                    Segment(
                        periode = Periode(
                            LocalDate.now(),
                            LocalDate.now().plusDays(1)
                        ),
                        verdi = Tilkjent(
                            dagsats = Beløp(1000),
                            gradering = Prosent(50),
                            barnetillegg = Beløp(1000),
                            grunnlagsfaktor = GUnit("1.0"),
                            grunnlag = Beløp(1000),
                            antallBarn = 1,
                            barnetilleggsats = Beløp(1000),
                            grunnbeløp = Beløp(1000)
                        )
                    ),
                    Segment(
                        periode = Periode(
                            LocalDate.now().plusDays(2),
                            LocalDate.now().plusDays(3)
                        ),
                        verdi = Tilkjent(
                            dagsats = Beløp(1000),
                            gradering = Prosent(50),
                            barnetillegg = Beløp(1000),
                            grunnlagsfaktor = GUnit("1.0"),
                            grunnlag = Beløp(1000),
                            antallBarn = 1,
                            barnetilleggsats = Beløp(1000),
                            grunnbeløp = Beløp(1000)
                        )
                    ),
                )
            )
            tilkjentYtelseRepository.lagre(behandling.id, tilkjentYtelse)
            val tilkjentYtelseHentet = tilkjentYtelseRepository.hentHvisEksisterer(behandling.id)
            assertNotNull(tilkjentYtelseHentet)
            assertEquals(tilkjentYtelse, tilkjentYtelseHentet)

        }

    }

    @Test
    fun `finner ingen tilkjentYtelse hvis den ikke eksisterer`() {
        InitTestDatabase.dataSource.transaction { connection ->
            val sak = runBlocking { sak(connection) }
            val behandling = behandling(connection, sak)

            val tilkjentYtelseRepository = TilkjentYtelseRepository(connection)
            val tilkjentYtelseHentet = tilkjentYtelseRepository.hentHvisEksisterer(behandling.id)
            assertNull(tilkjentYtelseHentet)
        }
    }

    private companion object {
        private val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))
    }


    private fun sak(connection: DBConnection): Sak {
        return PersonOgSakService(connection, FakePdlGateway).finnEllerOpprett(
            ident(),
            periode
        )
    }

    private fun behandling(connection: DBConnection, sak: Sak): Behandling {
        return SakOgBehandlingService(connection).finnEllerOpprettBehandling(
            sak.saksnummer,
            listOf(Årsak(ÅrsakTilBehandling.MOTTATT_SØKNAD))
        ).behandling
    }
}