package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.register.yrkesskade

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade.SkadekombinasjonRegister
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade.Yrkesskade
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade.Yrkesskader
import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.help.sak
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.test.juni
import no.nav.aap.behandlingsflyt.test.mai
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class YrkesskadeBackfillMigreringTest {

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

        private val yrkesskadeUtenNyeFelter = Yrkesskade(
            ref = "ref-1",
            saksnummer = 123,
            kildesystem = "KOMPYS",
            skadedato = 4 juni 2019,
        )

        private val oppdatertYrkesskade = Yrkesskade(
            ref = "ref-1",
            saksnummer = 123,
            kildesystem = "KOMPYS",
            skadedato = 4 juni 2019,
            vedtaksdato = 1 mai 2020,
            skadeart = "Arbeidsulykke",
            diagnose = "Lumbago",
            skadekombinasjoner = listOf(SkadekombinasjonRegister(kroppsdel = "korsrygg", skadetype = "belastningsskade")),
            skadekombinasjonerTekst = "Belastningsskade i korsrygg",
        )
    }

    @Test
    fun `hentKandidaterForBackfill returnerer rader hvor skadeart er null`() {
        dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)
            val repo = YrkesskadeRepositoryImpl(connection)

            repo.lagre(
                behandling.id,
                registerYrkesskader = Yrkesskader(listOf(yrkesskadeUtenNyeFelter)),
                oppgittYrkesskadeISøknad = false,
            )

            val kandidater = repo.hentKandidaterForBackfill()
                .filter { it.behandlingId == behandling.id }
            assertThat(kandidater).hasSize(1)
            assertThat(kandidater.single().ref).isEqualTo("ref-1")
        }
    }

    @Test
    fun `hentKandidaterForBackfill returnerer ikke rader hvor skadeart allerede er satt`() {
        dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)
            val repo = YrkesskadeRepositoryImpl(connection)

            repo.lagre(
                behandling.id,
                registerYrkesskader = Yrkesskader(listOf(oppdatertYrkesskade)),
                oppgittYrkesskadeISøknad = false,
            )

            val kandidater = repo.hentKandidaterForBackfill()
                .filter { it.behandlingId == behandling.id }
            assertThat(kandidater).isEmpty()
        }
    }

    @Test
    fun `hentKandidaterForBackfill returnerer ikke inaktive rader`() {
        dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)
            val repo = YrkesskadeRepositoryImpl(connection)

            repo.lagre(
                behandling.id,
                registerYrkesskader = Yrkesskader(listOf(yrkesskadeUtenNyeFelter)),
                oppgittYrkesskadeISøknad = false,
            )
            repo.lagre(
                behandling.id,
                registerYrkesskader = Yrkesskader(listOf(yrkesskadeUtenNyeFelter.copy(skadedato = 4 mai 2019))),
                oppgittYrkesskadeISøknad = false,
            )

            val kandidater = repo.hentKandidaterForBackfill()
                .filter { it.behandlingId == behandling.id }
            assertThat(kandidater).hasSize(1)
        }
    }

    @Test
    fun `backfillYrkesskadeDato oppdaterer nye felter på eksisterende rad`() {
        dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)
            val repo = YrkesskadeRepositoryImpl(connection)

            repo.lagre(
                behandling.id,
                registerYrkesskader = Yrkesskader(listOf(yrkesskadeUtenNyeFelter)),
                oppgittYrkesskadeISøknad = false,
            )

            val kandidat = repo.hentKandidaterForBackfill()
                .single { it.behandlingId == behandling.id }

            repo.backfillYrkesskadeDato(kandidat.yrkesskadeDatoId, oppdatertYrkesskade)

            val hentet = repo.hentHvisEksisterer(behandling.id)?.yrkesskader?.yrkesskader?.single()
            assertThat(hentet?.vedtaksdato).isEqualTo(1 mai 2020)
            assertThat(hentet?.skadeart).isEqualTo("Arbeidsulykke")
            assertThat(hentet?.diagnose).isEqualTo("Lumbago")
            assertThat(hentet?.skadekombinasjonerTekst).isEqualTo("Belastningsskade i korsrygg")
        }
    }

    @Test
    fun `backfillYrkesskadeDato endrer ikke skadedato eller andre eksisterende felter`() {
        dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)
            val repo = YrkesskadeRepositoryImpl(connection)

            repo.lagre(
                behandling.id,
                registerYrkesskader = Yrkesskader(listOf(yrkesskadeUtenNyeFelter)),
                oppgittYrkesskadeISøknad = false,
            )

            val kandidat = repo.hentKandidaterForBackfill()
                .single { it.behandlingId == behandling.id }
            repo.backfillYrkesskadeDato(kandidat.yrkesskadeDatoId, oppdatertYrkesskade)

            val hentet = repo.hentHvisEksisterer(behandling.id)?.yrkesskader?.yrkesskader?.single()
            assertThat(hentet?.ref).isEqualTo("ref-1")
            assertThat(hentet?.saksnummer).isEqualTo(123)
            assertThat(hentet?.kildesystem).isEqualTo("KOMPYS")
            assertThat(hentet?.skadedato).isEqualTo(4 juni 2019)
        }
    }

    @Test
    fun `backfillYrkesskadeDato etter kandidat er blitt inaktiv påvirker ikke aktiv rad`() {
        dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)
            val repo = YrkesskadeRepositoryImpl(connection)

            repo.lagre(
                behandling.id,
                registerYrkesskader = Yrkesskader(listOf(yrkesskadeUtenNyeFelter)),
                oppgittYrkesskadeISøknad = false,
            )
            val kandidat = repo.hentKandidaterForBackfill()
                .single { it.behandlingId == behandling.id }

            repo.lagre(
                behandling.id,
                registerYrkesskader = Yrkesskader(listOf(yrkesskadeUtenNyeFelter.copy(skadedato = 4 mai 2019))),
                oppgittYrkesskadeISøknad = false,
            )

            repo.backfillYrkesskadeDato(kandidat.yrkesskadeDatoId, oppdatertYrkesskade)

            val aktiv = repo.hentHvisEksisterer(behandling.id)?.yrkesskader?.yrkesskader?.single()
            assertThat(aktiv?.skadedato).isEqualTo(4 mai 2019)
            assertThat(aktiv?.skadeart).isNull()
        }
    }

    @Test
    fun `hentKandidaterForBackfill returnerer ikke rader uten registerdata (kun søknad)`() {
        dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)
            val repo = YrkesskadeRepositoryImpl(connection)

            repo.lagre(
                behandling.id,
                registerYrkesskader = null,
                oppgittYrkesskadeISøknad = true,
            )

            val kandidater = repo.hentKandidaterForBackfill()
                .filter { it.behandlingId == behandling.id }
            assertThat(kandidater).isEmpty()
        }
    }

    @Test
    fun `hentKandidaterForBackfill returnerer kun aktiv behandling ved revurdering`() {
        dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling1 = finnEllerOpprettBehandling(connection, sak)
            val repo = YrkesskadeRepositoryImpl(connection)

            repo.lagre(
                behandling1.id,
                registerYrkesskader = Yrkesskader(listOf(yrkesskadeUtenNyeFelter)),
                oppgittYrkesskadeISøknad = false,
            )
            BehandlingRepositoryImpl(connection).oppdaterBehandlingStatus(behandling1.id, Status.AVSLUTTET)

            val behandling2 = finnEllerOpprettBehandling(connection, sak)

            val kandidater = repo.hentKandidaterForBackfill()
                .filter { it.behandlingId == behandling1.id || it.behandlingId == behandling2.id }
            assertThat(kandidater.map { it.behandlingId })
                .containsExactlyInAnyOrder(behandling1.id, behandling2.id)
        }
    }
}