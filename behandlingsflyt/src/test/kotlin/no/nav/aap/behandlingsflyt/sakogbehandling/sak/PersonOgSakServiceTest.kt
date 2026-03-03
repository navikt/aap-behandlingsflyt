package no.nav.aap.behandlingsflyt.sakogbehandling.sak

import io.mockk.Called
import io.mockk.checkUnnecessaryStub
import io.mockk.clearAllMocks
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.aap.behandlingsflyt.behandling.søknad.TrukketSøknadRepository
import no.nav.aap.behandlingsflyt.behandling.søknad.TrukketSøknadVurdering
import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.hendelse.datadeling.ApiInternGateway
import no.nav.aap.behandlingsflyt.hendelse.datadeling.ArenaStatusResponse
import no.nav.aap.behandlingsflyt.repository.postgresRepositoryRegistry
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.db.PersonRepository
import no.nav.aap.behandlingsflyt.test.ident
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.komponenter.verdityper.Tid
import no.nav.aap.verdityper.dokument.JournalpostId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import java.sql.SQLException
import java.time.Instant
import java.time.LocalDate

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PersonOgSakServiceTest {
    private val pdlGateway: IdentGateway = mockk()
    private val apiInternGateway: ApiInternGateway = mockk(relaxed = true)

    private lateinit var dataSource: TestDataSource

    @BeforeAll
    fun setup() {
        dataSource = TestDataSource()
    }

    @AfterAll
    fun tearDown() {
        dataSource.close()
    }

    @BeforeEach
    fun beforeEach() {
        clearAllMocks()
    }

    @AfterEach
    fun afterEach() {
        confirmVerified(apiInternGateway, pdlGateway)
        checkUnnecessaryStub(pdlGateway, apiInternGateway)
    }

    @Nested
    @DisplayName("finnEllerOpprett - vanlige saker")
    inner class FinnEllerOpprettTest {
        @Test
        fun `finnEllerOpprett oppretter ny sak for ny person`() {
            val ident = ident()
            every { pdlGateway.hentAlleIdenterForPerson(ident) } returns listOf(ident)

            val sak = dataSource.transaction { connection ->
                val service = initPersonOgSakService(connection)
                service.finnEllerOpprett(ident, LocalDate.now())
            }

            assertThat(sak).isNotNull
            assertThat(sak.person.er(ident)).isTrue()

            verify(exactly = 1) {
                pdlGateway.hentAlleIdenterForPerson(ident)
                apiInternGateway.hentArenaStatus(setOf(ident.identifikator))
            }
        }

        @Test
        fun `finnEllerOpprett returnerer eksisterende sak for samme person`() {
            val ident = ident()
            every { pdlGateway.hentAlleIdenterForPerson(ident) } returns listOf(ident)

            val sak1 = dataSource.transaction { connection ->
                val service = initPersonOgSakService(connection)
                service.finnEllerOpprett(ident, LocalDate.now())
            }
            val sak2 = dataSource.transaction { connection ->
                val service = initPersonOgSakService(connection)
                service.finnEllerOpprett(ident, LocalDate.now())
            }

            assertThat(sak1.id).isEqualTo(sak2.id)
            assertThat(sak1.saksnummer).isEqualTo(sak2.saksnummer)

            verify(exactly = 2) {
                pdlGateway.hentAlleIdenterForPerson(ident)
                apiInternGateway.hentArenaStatus(setOf(ident.identifikator))
            }
        }

        @Test
        fun `finnEllerOpprett feiler når PDL returnerer tom identliste`() {
            val ident = ident()
            every { pdlGateway.hentAlleIdenterForPerson(ident) } returns emptyList()

            assertThrows<IllegalArgumentException> {
                dataSource.transaction { connection ->
                    val service = initPersonOgSakService(connection)
                    service.finnEllerOpprett(ident, LocalDate.now())
                }
            }

            verify(exactly = 1) { pdlGateway.hentAlleIdenterForPerson(ident) }
            verify { apiInternGateway wasNot Called }
        }

        @Test
        fun `finnEllerOpprett med flere identer finner personen med alle identer`() {
            val aktivIdent = Ident("12345678901", aktivIdent = true)
            val gammelIdent = Ident("98765432109", aktivIdent = false)
            val identliste = listOf(aktivIdent, gammelIdent)

            every { pdlGateway.hentAlleIdenterForPerson(aktivIdent) } returns identliste

            val sak = dataSource.transaction { connection ->
                val service = initPersonOgSakService(connection)
                service.finnEllerOpprett(aktivIdent, LocalDate.now())
            }

            assertThat(sak).isNotNull
            assertThat(sak.person.er(aktivIdent)).isTrue()
            assertThat(sak.person.er(gammelIdent)).isTrue()

            verify(exactly = 1) {
                pdlGateway.hentAlleIdenterForPerson(aktivIdent)
                apiInternGateway.hentArenaStatus(identliste.map { it.identifikator }.toSet())
            }
        }

        @Test
        fun `finnEllerOpprett rapporterer når person finnes i Arena men ikke i Kelvin`() {
            val ident = ident()
            every { pdlGateway.hentAlleIdenterForPerson(ident) } returns listOf(ident)
            every { apiInternGateway.hentArenaStatus(setOf(ident.identifikator)) } returns ArenaStatusResponse(true)

            val sak = dataSource.transaction { connection ->
                val service = initPersonOgSakService(connection)
                service.finnEllerOpprett(ident, LocalDate.now())
            }

            assertThat(sak).isNotNull
            verify(exactly = 1) {
                pdlGateway.hentAlleIdenterForPerson(ident)
                apiInternGateway.hentArenaStatus(setOf(ident.identifikator))
            }
        }
    }

    @Nested
    @DisplayName("finnEllerOpprett med trukket søknad")
    inner class FinnEllerOpprettMedTrukketSøknadTest {

        @Test
        fun `finnEllerOpprett returnerer eksisterende sak når trukket søknad har skalTrekkes false`() {
            val ident = ident()
            every { pdlGateway.hentAlleIdenterForPerson(ident) } returns listOf(ident)

            val (opprinneligSak, sammeSak) = dataSource.transaction { connection ->
                val repositoryProvider = postgresRepositoryRegistry.provider(connection)
                val service = PersonOgSakService(
                    pdlGateway,
                    apiInternGateway,
                    repositoryProvider.provide<PersonRepository>(),
                    repositoryProvider.provide<SakRepository>()
                )

                val opprinneligSak = service.finnEllerOpprett(ident, LocalDate.now())
                val behandling = finnEllerOpprettBehandling(connection, opprinneligSak)

                val trukketSøknadRepository = repositoryProvider.provide<TrukketSøknadRepository>()
                trukketSøknadRepository.lagreTrukketSøknadVurdering(
                    behandling.id,
                    TrukketSøknadVurdering(
                        journalpostId = JournalpostId("456"),
                        begrunnelse = "Søknad trekkes ikke likevel",
                        skalTrekkes = false,
                        vurdertAv = Bruker("Z999999"),
                        vurdert = Instant.now(),
                    )
                )

                val sammeSak = service.finnEllerOpprett(ident, LocalDate.now())
                opprinneligSak to sammeSak
            }

            assertThat(sammeSak.id).isEqualTo(opprinneligSak.id)
            assertThat(sammeSak.saksnummer).isEqualTo(opprinneligSak.saksnummer)

            verify(exactly = 2) {
                pdlGateway.hentAlleIdenterForPerson(ident)
                apiInternGateway.hentArenaStatus(setOf(ident.identifikator))
            }
        }

        @Test
        fun `finnEllerOpprett oppretter ny sak når eksisterende sak har trukket søknad med skalTrekkes true`() {
            val ident = ident()
            val søknadsdato = LocalDate.now()
            every { pdlGateway.hentAlleIdenterForPerson(ident) } returns listOf(ident)

            val (opprinneligSak, nySak) = dataSource.transaction { connection ->
                val repositoryProvider = postgresRepositoryRegistry.provider(connection)
                val sakRepository = repositoryProvider.provide<SakRepository>()
                val service = PersonOgSakService(
                    pdlGateway,
                    apiInternGateway,
                    repositoryProvider.provide<PersonRepository>(),
                    sakRepository
                )

                val opprinneligSak = service.finnEllerOpprett(ident, søknadsdato)
                val behandling = finnEllerOpprettBehandling(connection, opprinneligSak)

                val trukketSøknadRepository = repositoryProvider.provide<TrukketSøknadRepository>()
                trukketSøknadRepository.lagreTrukketSøknadVurdering(
                    behandling.id,
                    TrukketSøknadVurdering(
                        journalpostId = JournalpostId("123"),
                        begrunnelse = "Søknad trekkes",
                        skalTrekkes = true,
                        vurdertAv = Bruker("Z999999"),
                        vurdert = Instant.now(),
                    )
                )

                // Justere rettighetsperioden for å unngå SQLException på overlappende periode
                sakRepository.oppdaterRettighetsperiode(
                    opprinneligSak.id,
                    Periode(Tid.MIN, Tid.MIN.plusDays(1))
                )

                val nySak = service.finnEllerOpprett(ident, søknadsdato)
                opprinneligSak to nySak
            }

            assertThat(nySak.id).isNotEqualTo(opprinneligSak.id)
            assertThat(nySak.saksnummer).isNotEqualTo(opprinneligSak.saksnummer)
            assertThat(nySak.rettighetsperiode.fom).isEqualTo(søknadsdato)
            assertThat(nySak.rettighetsperiode.tom).isEqualTo(Tid.MAKS)

            verify(exactly = 2) {
                pdlGateway.hentAlleIdenterForPerson(ident)
                apiInternGateway.hentArenaStatus(setOf(ident.identifikator))
            }
        }

        @Test
        fun `finnEllerOpprett med trukket søknad kaster feil hvis samme søknadsdato (fom rettighetsperiode)`() {
            val ident = ident()
            val søknadsdato = LocalDate.now()
            every { pdlGateway.hentAlleIdenterForPerson(ident) } returns listOf(ident)

            dataSource.transaction { connection ->
                val repositoryProvider = postgresRepositoryRegistry.provider(connection)
                val sakRepository = repositoryProvider.provide<SakRepository>()
                val service = PersonOgSakService(
                    pdlGateway,
                    apiInternGateway,
                    repositoryProvider.provide<PersonRepository>(),
                    sakRepository
                )

                val opprinneligSak = service.finnEllerOpprett(ident, søknadsdato)
                val behandling = finnEllerOpprettBehandling(connection, opprinneligSak)

                val trukketSøknadRepository = repositoryProvider.provide<TrukketSøknadRepository>()
                trukketSøknadRepository.lagreTrukketSøknadVurdering(
                    behandling.id,
                    TrukketSøknadVurdering(
                        journalpostId = JournalpostId("123"),
                        begrunnelse = "Søknad trekkes",
                        skalTrekkes = true,
                        vurdertAv = Bruker("Z999999"),
                        vurdert = Instant.now(),
                    )
                )

                assertThrows<SQLException> {
                    service.finnEllerOpprett(ident, søknadsdato.plusDays(1))
                }
            }

            verify(exactly = 2) {
                pdlGateway.hentAlleIdenterForPerson(ident)
                apiInternGateway.hentArenaStatus(setOf(ident.identifikator))
            }
        }
    }

    @Nested
    @DisplayName("Finn saker for ident")
    inner class FinnSakerForTest {

        @Test
        fun `finnSakerFor returnerer tom liste når person ikke har saker`() {
            val ident = ident()
            every { pdlGateway.hentAlleIdenterForPerson(ident) } returns listOf(ident)

            val saker = dataSource.transaction { connection ->
                val service = initPersonOgSakService(connection)
                service.finnSakerFor(ident)
            }

            assertThat(saker).isEmpty()

            verify(exactly = 1) { pdlGateway.hentAlleIdenterForPerson(ident) }
            verify { apiInternGateway wasNot Called }
        }

        @Test
        fun `finnSakerFor returnerer saker for person som har saker`() {
            val ident = ident()
            every { pdlGateway.hentAlleIdenterForPerson(ident) } returns listOf(ident)

            val (opprettetSak, funnetSaker) = dataSource.transaction { connection ->
                val service = initPersonOgSakService(connection)

                val sak = service.finnEllerOpprett(ident, LocalDate.now())
                val saker = service.finnSakerFor(ident)
                sak to saker
            }

            assertThat(funnetSaker).hasSize(1)
            assertThat(funnetSaker.first().id).isEqualTo(opprettetSak.id)

            verify(exactly = 2) { pdlGateway.hentAlleIdenterForPerson(ident) }
            verify(exactly = 1) { apiInternGateway.hentArenaStatus(setOf(ident.identifikator))  }
        }

        @Test
        fun `finnSakerFor feiler når PDL returnerer tom identliste`() {
            val ident = ident()
            every { pdlGateway.hentAlleIdenterForPerson(ident) } returns emptyList()

            assertThrows<IllegalArgumentException> {
                dataSource.transaction { connection ->
                    val repositoryProvider = postgresRepositoryRegistry.provider(connection)
                    PersonOgSakService(
                        pdlGateway,
                        apiInternGateway,
                        repositoryProvider.provide<PersonRepository>(),
                        repositoryProvider.provide<SakRepository>()
                    ).finnSakerFor(ident)
                }
            }

            verify(exactly = 1) {
                pdlGateway.hentAlleIdenterForPerson(ident)
            }
            verify { apiInternGateway wasNot Called }
        }

        @Test
        fun `finnSakerFor finner saker via alternativ ident`() {
            val aktivIdent = Ident("11223344556", aktivIdent = true)
            val gammelIdent = Ident("66554433221", aktivIdent = false)
            val identliste = listOf(aktivIdent, gammelIdent)

            every { pdlGateway.hentAlleIdenterForPerson(aktivIdent) } returns identliste
            every { pdlGateway.hentAlleIdenterForPerson(gammelIdent) } returns identliste

            val (opprettetSak, funnetSaker) = dataSource.transaction { connection ->
                val service = initPersonOgSakService(connection)

                val sak = service.finnEllerOpprett(aktivIdent, LocalDate.now())
                val saker = service.finnSakerFor(gammelIdent)
                sak to saker
            }

            assertThat(funnetSaker).hasSize(1)
            assertThat(funnetSaker.first().id).isEqualTo(opprettetSak.id)

            verify(exactly = 1) {
                pdlGateway.hentAlleIdenterForPerson(aktivIdent)
                apiInternGateway.hentArenaStatus(setOf(aktivIdent.identifikator, gammelIdent.identifikator))
                pdlGateway.hentAlleIdenterForPerson(gammelIdent)
            }
        }
    }

    private fun initPersonOgSakService(connection: DBConnection): PersonOgSakService {
        val repositoryProvider = postgresRepositoryRegistry.provider(connection)
        val service = PersonOgSakService(
            pdlGateway,
            apiInternGateway,
            repositoryProvider.provide<PersonRepository>(),
            repositoryProvider.provide<SakRepository>()
        )
        return service
    }
}
