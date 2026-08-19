package no.nav.aap.behandlingsflyt.sakogbehandling.sak

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.aap.behandlingsflyt.help.ident
import no.nav.aap.behandlingsflyt.hendelse.datadeling.ApiInternGateway
import no.nav.aap.behandlingsflyt.hendelse.datadeling.ArenaSakMedVedtakResponse
import no.nav.aap.behandlingsflyt.hendelse.datadeling.ArenaSakOppsummering
import no.nav.aap.behandlingsflyt.hendelse.datadeling.ArenaSakPerson
import no.nav.aap.behandlingsflyt.hendelse.datadeling.ArenaSakerResponse
import no.nav.aap.behandlingsflyt.hendelse.datadeling.ArenaStatusResponse
import no.nav.aap.behandlingsflyt.hendelse.mottak.MottattHendelseService
import no.nav.aap.behandlingsflyt.repository.postgresRepositoryRegistry
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.db.PersonRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryArenaMigreringRepository
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.komponenter.verdityper.Tid
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.time.LocalDateTime

class ArenaMigreringServiceTest {
    private val apiInternGateway: ApiInternGateway = mockk()
    private val pdlGateway: IdentGateway = mockk()

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

    @BeforeEach
    fun reset() {
        InMemoryArenaMigreringRepository.reset()
    }

    @Test
    fun `henter arenasak med vedtak og lagrer den på migreringen`() {
        val sakId = SakId(1)
        val saksnummerArena = "2018-123456"
        InMemoryArenaMigreringRepository.lagre(
            ArenaMigrering(
                sakId = sakId,
                saksnummerArena = saksnummerArena,
                ident = "12345678910",
                migrertTidspunkt = LocalDateTime.now(),
            )
        )
        val respons = arenaSakMedVedtak(saksnummerArena)
        every { apiInternGateway.hentArenaSakMedVedtak(saksnummerArena) } returns respons

        val resultat = ArenaMigreringService(
            apiInternGateway = apiInternGateway,
            arenaMigreringRepository = InMemoryArenaMigreringRepository,
            personOgSakService = mockk(),
            mottattHendelseService = mockk(),
        ).hentOgLagreArenaSakMedVedtak(sakId, saksnummerArena)

        assertThat(resultat).isEqualTo(respons)
        assertThat(InMemoryArenaMigreringRepository.hentForSakHvisEksisterer(sakId)?.arenaSakData)
            .isEqualTo(respons)
        verify(exactly = 1) { apiInternGateway.hentArenaSakMedVedtak(saksnummerArena) }
    }

    @Test
    fun `feiler når saken ikke er migrert fra arena`() {
        val sakId = SakId(2)
        val saksnummerArena = "2018-123456"
        every { apiInternGateway.hentArenaSakMedVedtak(saksnummerArena) } returns
                arenaSakMedVedtak(saksnummerArena)

        assertThrows<IllegalArgumentException> {
            ArenaMigreringService(
                apiInternGateway = apiInternGateway,
                arenaMigreringRepository = InMemoryArenaMigreringRepository,
                personOgSakService = mockk(),
                mottattHendelseService = mockk(),
            ).hentOgLagreArenaSakMedVedtak(sakId, saksnummerArena)
        }
    }

    @Test
    fun `migrerFraArena oppretter sak, lagrer arenasak-data og registrerer hendelse`() {
        val ident = ident()
        val saksnummerArena = "2016-123456"
        stubGatewayerFor(ident, saksnummerArena, statuskode = "AKTIV")

        val resultat = dataSource.transaction { connection ->
            initService(connection).migrerFraArena(ident, saksnummerArena)
        }

        assertThat(resultat).isInstanceOf(MigrerFraArenaResultat.Migrert::class.java)
        val migrert = resultat as MigrerFraArenaResultat.Migrert
        assertThat(migrert.sak.person.er(ident)).isTrue()
        assertThat(migrert.sak.rettighetsperiode.fom).isEqualTo(LocalDate.now())
        assertThat(migrert.sak.rettighetsperiode.tom).isEqualTo(Tid.MAKS)

        val migrering = dataSource.transaction { connection ->
            postgresRepositoryRegistry.provider(connection)
                .provide<ArenaMigreringRepository>()
                .hentForSakHvisEksisterer(migrert.sak.id)
        }
        assertThat(migrering?.saksnummerArena).isEqualTo(saksnummerArena)
        assertThat(migrering?.arenaSakData).isEqualTo(arenaSakMedVedtak(saksnummerArena))
    }

    @Test
    fun `migrerFraArena returnerer SakFinnesAllerede når personen har sak i Kelvin fra før`() {
        val ident = ident()
        val saksnummerArena = "2016-123456"
        stubGatewayerFor(ident, saksnummerArena, statuskode = "AKTIV")

        dataSource.transaction { connection ->
            initService(connection).migrerFraArena(ident, saksnummerArena)
        }

        val resultat = dataSource.transaction { connection ->
            initService(connection).migrerFraArena(ident, saksnummerArena)
        }

        assertThat(resultat).isEqualTo(MigrerFraArenaResultat.SakFinnesAllerede)
    }

    @Test
    fun `migrerFraArena returnerer ArenasakIkkeMigrerbar når arenasaken ikke er aktiv`() {
        val ident = ident()
        val saksnummerArena = "2016-123456"
        stubGatewayerFor(ident, saksnummerArena, statuskode = "AVSLU")

        val resultat = dataSource.transaction { connection ->
            initService(connection).migrerFraArena(ident, saksnummerArena)
        }

        assertThat(resultat).isInstanceOf(MigrerFraArenaResultat.ArenasakIkkeMigrerbar::class.java)
    }

    @Test
    fun `migrerFraArena returnerer ArenasakIkkeMigrerbar når arenasaken ikke finnes`() {
        val ident = ident()
        stubGatewayerFor(ident, "2016-123456", statuskode = "AKTIV")

        val resultat = dataSource.transaction { connection ->
            initService(connection).migrerFraArena(ident, "2020-999999")
        }

        assertThat(resultat).isInstanceOf(MigrerFraArenaResultat.ArenasakIkkeMigrerbar::class.java)
    }

    private fun stubGatewayerFor(ident: Ident, saksnummerArena: String, statuskode: String) {
        val (år, løpenummer) = saksnummerArena.split("-")
        every { pdlGateway.hentAlleIdenterForPerson(ident) } returns listOf(ident)
        every { apiInternGateway.hentArenaStatus(any()) } returns Result.success(ArenaStatusResponse(false))
        every { apiInternGateway.hentSakerForPerson(ident.identifikator) } returns ArenaSakerResponse(
            saker = listOf(
                ArenaSakOppsummering(
                    sakId = saksnummerArena,
                    lopenummer = løpenummer.toInt(),
                    aar = år.toInt(),
                    antallVedtak = 1,
                    statuskode = statuskode,
                    statusnavn = statuskode,
                    sakstype = null,
                    regDato = LocalDate.of(år.toInt(), 1, 1),
                    avsluttetDato = null,
                )
            )
        )
        every { apiInternGateway.hentArenaSakMedVedtak(saksnummerArena) } returns
                arenaSakMedVedtak(saksnummerArena)
    }

    private fun initService(connection: DBConnection): ArenaMigreringService {
        val repositoryProvider = postgresRepositoryRegistry.provider(connection)
        return ArenaMigreringService(
            apiInternGateway = apiInternGateway,
            arenaMigreringRepository = repositoryProvider.provide<ArenaMigreringRepository>(),
            personOgSakService = PersonOgSakService(
                pdlGateway,
                apiInternGateway,
                repositoryProvider.provide<PersonRepository>(),
                repositoryProvider.provide<SakRepository>(),
                repositoryProvider.provide<ArenaMigreringRepository>()
            ),
            mottattHendelseService = MottattHendelseService(repositoryProvider),
        )
    }

    private fun arenaSakMedVedtak(saksnummerArena: String) = ArenaSakMedVedtakResponse(
        sakId = saksnummerArena,
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
        vedtak = listOf(),
    )
}
