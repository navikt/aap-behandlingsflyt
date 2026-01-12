package no.nav.aap.behandlingsflyt.prosessering

import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.GraderingGrunnlag
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.Tilkjent
import no.nav.aap.behandlingsflyt.repository.postgresRepositoryRegistry
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.komponenter.verdityper.GUnit
import no.nav.aap.komponenter.verdityper.Prosent
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

class VarsleVedtakJobbUtførerTest {

    private lateinit var dataSource: TestDataSource

    @BeforeEach
    fun setUp() {
        dataSource = TestDataSource()
    }

    @Test
    fun `endringITilkjentYtelseTidslinje - begge null gir false`() {
        dataSource.transaction { connection ->
            val jobbUtfører = VarsleVedtakJobbUtfører(
                repositoryProvider = postgresRepositoryRegistry.provider(connection),
                gatewayProvider = GatewayProvider,
            )

            val resultat = jobbUtfører.endringITilkjentYtelseTidslinje(null, null)
            assertFalse(resultat)
        }
    }

    @Test
    fun `endringITilkjentYtelseTidslinje - ingen forrige men nåværende gir true`() {
        dataSource.transaction { connection ->
            val jobbUtfører = VarsleVedtakJobbUtfører(
                repositoryProvider = postgresRepositoryRegistry.provider(connection),
                gatewayProvider = GatewayProvider,
            )

            val nåværende = tidslinjeMedGradering(grad = Prosent.`50_PROSENT`)

            val resultat = jobbUtfører.endringITilkjentYtelseTidslinje(null, nåværende)
            assertTrue(resultat)
        }
    }

    @Test
    fun `endringITilkjentYtelseTidslinje - endring fra 0 til ikke-0 gir true`() {
        dataSource.transaction { connection ->
            val jobbUtfører = VarsleVedtakJobbUtfører(
                repositoryProvider = postgresRepositoryRegistry.provider(connection),
                gatewayProvider = GatewayProvider,
            )

            val forrige = tidslinjeMedGradering(grad = Prosent.`0_PROSENT`)
            val nåværende = tidslinjeMedGradering(grad = Prosent.`50_PROSENT`)

            val resultat = jobbUtfører.endringITilkjentYtelseTidslinje(forrige, nåværende)
            assertTrue(resultat)
        }
    }

    @Test
    fun `endringITilkjentYtelseTidslinje - samme gradering og dagsats gir false`() {
        dataSource.transaction { connection ->
            val jobbUtfører = VarsleVedtakJobbUtfører(
                repositoryProvider = postgresRepositoryRegistry.provider(connection),
                gatewayProvider = GatewayProvider,
            )

            val forrige = tidslinjeMedGradering(grad = Prosent.`50_PROSENT`)
            val nåværende = tidslinjeMedGradering(grad = Prosent.`50_PROSENT`)

            val resultat = jobbUtfører.endringITilkjentYtelseTidslinje(forrige, nåværende)
            assertFalse(resultat)
        }
    }

    private fun tidslinjeMedGradering(grad: Prosent): Tidslinje<Tilkjent> {
        val tilkjent = Tilkjent(
            dagsats = Beløp(100),
            gradering = grad,
            graderingGrunnlag = GraderingGrunnlag(
                samordningGradering = Prosent.`0_PROSENT`,
                institusjonGradering = Prosent.`0_PROSENT`,
                arbeidGradering = Prosent.`0_PROSENT`,
                samordningUføregradering = Prosent.`0_PROSENT`,
                samordningArbeidsgiverGradering = Prosent.`0_PROSENT`,
                meldepliktGradering = Prosent.`0_PROSENT`,
            ),
            grunnlagsfaktor = GUnit("1.0"),
            grunnbeløp = Beløp(100),
            antallBarn = 0,
            barnetilleggsats = Beløp(0),
            barnetillegg = Beløp(0),
            utbetalingsdato = LocalDate.now(),
        )

        val periode = Periode(LocalDate.now(), LocalDate.now())
        return Tidslinje(listOf(Segment(periode, tilkjent))).komprimer()
    }
}