package no.nav.aap.utenlandsopphold

import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.help.sak
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

@Tag("db")
internal class BekreftBrevSendtRepositoryImplTest {
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

    private fun bekreftelse(tidspunkt: LocalDateTime = LocalDateTime.of(2025, 2, 1, 12, 0)) =
        BekreftBrevSendtBekreftelse(brevSendt = true, bekreftetAv = "Z000000", bekreftetTidspunkt = tidspunkt)

    @Test
    fun `lagrer og henter bekreftelse`() {
        dataSource.transaction { connection ->
            val sak = sak(connection, LocalDate.of(2025, 1, 1))
            val behandling = finnEllerOpprettBehandling(connection, sak, Vurderingsbehov.SØKNAD_UTENLANDSOPPHOLD)
            val repository = BekreftBrevSendtRepositoryImpl(connection)

            assertThat(repository.hentHvisEksisterer(behandling.id)).isNull()

            repository.lagre(behandling.id, bekreftelse())

            assertThat(repository.hentHvisEksisterer(behandling.id)!!.bekreftelse).isEqualTo(bekreftelse())
        }
    }

    @Test
    fun `ny bekreftelse erstatter den forrige`() {
        dataSource.transaction { connection ->
            val sak = sak(connection, LocalDate.of(2025, 1, 1))
            val behandling = finnEllerOpprettBehandling(connection, sak, Vurderingsbehov.SØKNAD_UTENLANDSOPPHOLD)
            val repository = BekreftBrevSendtRepositoryImpl(connection)

            repository.lagre(behandling.id, bekreftelse())
            val nyere = bekreftelse(LocalDateTime.of(2025, 3, 1, 12, 0))
            repository.lagre(behandling.id, nyere)

            assertThat(repository.hentHvisEksisterer(behandling.id)!!.bekreftelse).isEqualTo(nyere)
        }
    }

    @Test
    fun `kopier kopierer ikke grunnlaget`() {
        dataSource.transaction { connection ->
            val sak = sak(connection, LocalDate.of(2025, 1, 1))
            val fra = finnEllerOpprettBehandling(connection, sak, Vurderingsbehov.SØKNAD_UTENLANDSOPPHOLD)
            val til = finnEllerOpprettBehandling(connection, sak, Vurderingsbehov.SØKNAD_UTENLANDSOPPHOLD)
            val repository = BekreftBrevSendtRepositoryImpl(connection)
            repository.lagre(fra.id, bekreftelse())

            repository.kopier(fra.id, til.id)

            assertThat(repository.hentHvisEksisterer(til.id)).isNull()
        }
    }

    @Test
    fun `slett fjerner grunnlaget og er idempotent`() {
        dataSource.transaction { connection ->
            val sak = sak(connection, LocalDate.of(2025, 1, 1))
            val behandling = finnEllerOpprettBehandling(connection, sak, Vurderingsbehov.SØKNAD_UTENLANDSOPPHOLD)
            val repository = BekreftBrevSendtRepositoryImpl(connection)
            repository.lagre(behandling.id, bekreftelse())

            repository.slett(behandling.id)
            repository.slett(behandling.id)

            assertThat(repository.hentHvisEksisterer(behandling.id)).isNull()
        }
    }
}
