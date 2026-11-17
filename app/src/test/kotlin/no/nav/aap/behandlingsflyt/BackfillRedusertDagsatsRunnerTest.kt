package no.nav.aap.behandlingsflyt

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.GraderingGrunnlag
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.Minstesats
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.Tilkjent
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokument
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokumentRepositoryImpl
import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.help.sak
import no.nav.aap.behandlingsflyt.integrasjon.createGatewayProvider
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.repository.behandling.tilkjentytelse.TilkjentYtelseRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.test.AlleAvskruddUnleash
import no.nav.aap.behandlingsflyt.test.februar
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.komponenter.tidslinje.tidslinjeOf
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.komponenter.verdityper.GUnit
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.verdityper.dokument.Kanal
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

@Tag("db")
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

    @Test
    fun `sjekk at samtlige endrede tilkjent_periode rader logges i app-logg`() {
        val sakOgBehandling = opprettSakMedManglendeRedusertDagsats()
        val behandlingId = BehandlingId(sakOgBehandling.behandlingId)
        val forventedeIder = dataSource.transaction { connection ->
            lagreTilkjentYtelseUtenRedusertDagsats(
                connection,
                behandlingId,
                10 februar 2024,
            )
            TilkjentYtelseRepositoryImpl(connection).hentTilkjentUtenRedusertDagsats(behandlingId)
                .map { it.tilkjentPeriodeId }
        }
        assertThat(forventedeIder).hasSize(2)

        val loggmeldinger = fangAppLogg {
            val antallBackfillet = nyBackfillRunner().backfillSak(sakOgBehandling.sakId)
            assertThat(antallBackfillet).isEqualTo(forventedeIder.size)
        }
        dataSource.transaction { connection ->
            assertThat(
                TilkjentYtelseRepositoryImpl(connection).hentTilkjentUtenRedusertDagsats(behandlingId)
            ).isEmpty()
        }

        val loggedeIder = loggmeldinger
            .filter { it.startsWith("Backfill utført for tilkjent_periode radene: ") }
            .flatMap {
                it.removePrefix("Backfill utført for tilkjent_periode radene: ")
                    .removeSurrounding("[", "]")
                    .split(", ")
                    .map(String::toLong)
            }
        assertThat(loggedeIder).containsExactlyInAnyOrderElementsOf(forventedeIder)
    }

    private fun fangAppLogg(handling: () -> Unit): List<String> {
        val logger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger
        val appender = ListAppender<ILoggingEvent>().apply { start() }
        logger.addAppender(appender)
        try {
            handling()
            return appender.list.map { it.formattedMessage }
        } finally {
            logger.detachAppender(appender)
            appender.stop()
        }
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
            tidslinjeOf(
                Periode(dato, dato.plusDays(30)) to Tilkjent(
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
            ),
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
