package no.nav.aap.behandlingsflyt

import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.GraderingGrunnlag
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.Minstesats
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.Tilkjent
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.TilkjentYtelsePeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokument
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokumentRepositoryImpl
import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.help.sak
import no.nav.aap.behandlingsflyt.help.tomtTilkjentYtelseGrunnlag
import no.nav.aap.behandlingsflyt.integrasjon.createGatewayProvider
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.repository.behandling.tilkjentytelse.TilkjentYtelseRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.test.AlleAvskruddUnleash
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.komponenter.verdityper.GUnit
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.verdityper.dokument.Kanal
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

class BackfillRedusertDagsatsRunnerTest {
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
    fun `beregner og lagrer redusert dagsats for behandling som mangler verdi`() {
        val sakOgBehandling = opprettSakMedManglendeRedusertDagsats()

        assertThat(hentLagretRedusertDagsats(sakOgBehandling.behandlingId)).isNull()

        val backfill = nyBackfillRunner()
        backfill.backfillSak(sakOgBehandling.sakId)

        assertThat(hentLagretRedusertDagsats(sakOgBehandling.behandlingId))
            .isEqualByComparingTo(BigDecimal("1000"))
    }

    private fun opprettSakMedManglendeRedusertDagsats(): SakOgBehandling {
        val søknadsdato = LocalDate.of(2024, 1, 10)
        return dataSource.transaction { connection ->
            val sak = sak(connection, søknadsdato)
            val behandling = finnEllerOpprettBehandling(connection, sak)

            lagreSøknad(connection, sak.id, behandling.id, søknadsdato)
            lagreTilkjentYtelseUtenRedusertDagsats(connection, behandling.id, søknadsdato)

            SakOgBehandling(sak.id.toLong(), behandling.id.toLong())
        }
    }

    private fun lagreSøknad(
        connection: DBConnection,
        sakId: SakId,
        behandlingId: BehandlingId,
        dato: LocalDate,
    ) {
        MottattDokumentRepositoryImpl(connection).lagre(
            MottattDokument(
                referanse = InnsendingReferanse(
                    InnsendingReferanse.Type.JOURNALPOST,
                    UUID.randomUUID().toString(),
                ),
                sakId = sakId,
                behandlingId = behandlingId,
                mottattTidspunkt = dato.atStartOfDay(),
                type = InnsendingType.SØKNAD,
                kanal = Kanal.DIGITAL,
                strukturertDokument = null,
            )
        )
    }

    private fun lagreTilkjentYtelseUtenRedusertDagsats(
        connection: DBConnection,
        behandlingId: BehandlingId,
        dato: LocalDate,
    ) {
        TilkjentYtelseRepositoryImpl(connection).lagre(
            behandlingId,
            listOf(
                TilkjentYtelsePeriode(
                    periode = Periode(dato, dato.plusDays(30)),
                    tilkjent = Tilkjent(
                        dagsats = Beløp(1000),
                        gradering = Prosent.`100_PROSENT`,
                        graderingGrunnlag = graderingUtenReduksjon(),
                        barnetillegg = Beløp(0),
                        grunnlagsfaktor = GUnit("1.0"),
                        grunnbeløp = Beløp(1000),
                        antallBarn = 0,
                        barnetilleggsats = Beløp(0),
                        barnepensjonDagsats = Beløp(0),
                        utbetalingsdato = dato.plusDays(14),
                        minsteSats = Minstesats.IKKE_MINSTESATS,
                        redusertDagsats = null,
                    )
                )
            ),
            tomtTilkjentYtelseGrunnlag,
            "test",
        )

        connection.execute(
            """
            UPDATE TILKJENT_PERIODE
            SET REDUSERT_DAGSATS = NULL
            WHERE TILKJENT_YTELSE_ID IN (
                SELECT ID FROM TILKJENT_YTELSE WHERE BEHANDLING_ID = ?
            )
            """.trimIndent()
        ) {
            setParams { setLong(1, behandlingId.toLong()) }
        }
    }

    private fun graderingUtenReduksjon() = GraderingGrunnlag(
        samordningGradering = Prosent.`0_PROSENT`,
        institusjonGradering = Prosent.`0_PROSENT`,
        arbeidGradering = Prosent.`0_PROSENT`,
        samordningUføregradering = Prosent.`0_PROSENT`,
        samordningArbeidsgiverGradering = Prosent.`0_PROSENT`,
        meldepliktGradering = Prosent.`0_PROSENT`,
    )

    private fun nyBackfillRunner() = BackfillRedusertDagsats(
        dataSource,
        createGatewayProvider { register<AlleAvskruddUnleash>() },
    )

    private fun hentLagretRedusertDagsats(behandlingId: Long): BigDecimal? =
        dataSource.transaction { connection ->
            connection.queryFirstOrNull(
                """
                SELECT TP.REDUSERT_DAGSATS
                FROM TILKJENT_PERIODE TP
                JOIN TILKJENT_YTELSE TY ON TY.ID = TP.TILKJENT_YTELSE_ID
                WHERE TY.BEHANDLING_ID = ? AND TY.AKTIV = TRUE
                """.trimIndent()
            ) {
                setParams { setLong(1, behandlingId) }
                setRowMapper { it.getBigDecimalOrNull("REDUSERT_DAGSATS") }
            }
        }

    private data class SakOgBehandling(
        val sakId: Long,
        val behandlingId: Long,
    )
}
