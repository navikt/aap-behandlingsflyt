package no.nav.aap.behandlingsflyt.repository.sak

import no.nav.aap.behandlingsflyt.help.opprettSak
import no.nav.aap.behandlingsflyt.hendelse.datadeling.ArenaSakMedVedtakResponse
import no.nav.aap.behandlingsflyt.hendelse.datadeling.ArenaSakPerson
import no.nav.aap.behandlingsflyt.hendelse.datadeling.ArenaVedtakDetaljer
import no.nav.aap.behandlingsflyt.hendelse.datadeling.ArenaVedtakfakta
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.ArenaMigrering
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.time.LocalDateTime

class ArenaMigreringRepositoryImplTest {
    companion object {
        private lateinit var dataSource: TestDataSource

        @BeforeAll
        @JvmStatic
        fun setup() {
            dataSource = TestDataSource()
        }

        @AfterAll
        @JvmStatic
        fun tearDown() = dataSource.close()
    }

    @Test
    fun `lagre og hent for sak — sjekk alle felter`() {
        val sak = dataSource.transaction { connection ->
            opprettSak(connection, LocalDate.now())
        }

        val migrertTidspunkt = LocalDateTime.of(2024, 6, 15, 12, 30, 0)
        val migrering = ArenaMigrering(
            sakId = sak.id,
            saksnummerArena = "2018-123456",
            ident = sak.person.aktivIdent().identifikator,
            migrertTidspunkt = migrertTidspunkt,
        )

        dataSource.transaction { connection ->
            ArenaMigreringRepositoryImpl(connection).lagre(migrering)
        }

        val hentet = dataSource.transaction { connection ->
            ArenaMigreringRepositoryImpl(connection).hentForSakHvisEksisterer(sak.id)
        }

        assertThat(hentet).isNotNull
        assertThat(hentet!!.sakId).isEqualTo(sak.id)
        assertThat(hentet.saksnummerArena).isEqualTo("2018-123456")
        assertThat(hentet.ident).isEqualTo(sak.person.aktivIdent().identifikator)
        assertThat(hentet.migrertTidspunkt).isEqualTo(migrertTidspunkt)
    }

    @Test
    fun `lagre og hent for sak (inkluder arenaSakData) — sjekk alle felter`() {
        val sak = dataSource.transaction { connection ->
            opprettSak(connection, LocalDate.now())
        }

        val arenaSakData = arenaSakMedVedtak()
        val migrertTidspunkt = LocalDateTime.of(2024, 6, 15, 12, 30, 0)
        val migrering = ArenaMigrering(
            sakId = sak.id,
            saksnummerArena = "2018-123456",
            ident = sak.person.aktivIdent().identifikator,
            migrertTidspunkt = migrertTidspunkt,
            arenaSakData = arenaSakData
        )

        dataSource.transaction { connection ->
            ArenaMigreringRepositoryImpl(connection).lagre(migrering)
        }

        val hentet = dataSource.transaction { connection ->
            ArenaMigreringRepositoryImpl(connection).hentForSakHvisEksisterer(sak.id)
        }

        assertThat(hentet).isNotNull
        assertThat(hentet!!.sakId).isEqualTo(sak.id)
        assertThat(hentet.saksnummerArena).isEqualTo("2018-123456")
        assertThat(hentet.ident).isEqualTo(sak.person.aktivIdent().identifikator)
        assertThat(hentet.migrertTidspunkt).isEqualTo(migrertTidspunkt)
        assertThat(hentet.arenaSakData).isEqualTo(arenaSakData)
    }

    @Test
    fun `hentForSakHvisEksisterer returnerer null når ingen migrering er lagret`() {
        val sak = dataSource.transaction { connection ->
            opprettSak(connection, LocalDate.now())
        }

        val hentet = dataSource.transaction { connection ->
            ArenaMigreringRepositoryImpl(connection).hentForSakHvisEksisterer(sak.id)
        }

        assertThat(hentet).isNull()
    }

    private fun arenaSakMedVedtak() = ArenaSakMedVedtakResponse(
        sakId = "2018-123456",
        opprettetAar = 2018,
        lopenr = 123456,
        person = ArenaSakPerson(
            personId = 1,
            fodselsnummer = "12345678910",
            fornavn = "Test",
            etternavn = "Testesen",
        ),
        statuskode = "AKTIV",
        statusnavn = "Aktiv",
        registrertDato = LocalDateTime.of(2018, 1, 1, 10, 0, 0),
        avsluttetDato = null,
        vedtak = listOf(
            ArenaVedtakDetaljer(
                vedtakId = 42,
                lopenrvedtak = 1,
                statusKode = "IVERK",
                statusNavn = "Iverksatt",
                vedtaktypeKode = "O",
                vedtaktypeNavn = "Ordinær",
                aktivitetsfaseKode = "AF",
                aktivitetsfaseNavn = "Arbeidsfase",
                fraOgMed = LocalDate.of(2018, 2, 1),
                tilDato = LocalDate.of(2019, 1, 31),
                rettighetkode = "AAP",
                rettighetnavn = "Arbeidsavklaringspenger",
                utfallkode = "JA",
                begrunnelse = null,
                saksbehandler = null,
                beslutter = null,
                relatertVedtak = null,
                fakta = listOf(
                    ArenaVedtakfakta(
                        kode = "FAKTA",
                        navn = "Faktanavn",
                        verdi = "verdi",
                        registrertDato = LocalDate.of(2018, 2, 1),
                    )
                ),
            )
        ),
    )
}
