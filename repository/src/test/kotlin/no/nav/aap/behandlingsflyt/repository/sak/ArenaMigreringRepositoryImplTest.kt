package no.nav.aap.behandlingsflyt.repository.sak

import no.nav.aap.behandlingsflyt.help.opprettSak
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.ArenaMigrering
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
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
    fun `hentForSakHvisEksisterer returnerer null når ingen migrering er lagret`() {
        val sak = dataSource.transaction { connection ->
            opprettSak(connection, LocalDate.now())
        }

        val hentet = dataSource.transaction { connection ->
            ArenaMigreringRepositoryImpl(connection).hentForSakHvisEksisterer(sak.id)
        }

        assertThat(hentet).isNull()
    }
}
