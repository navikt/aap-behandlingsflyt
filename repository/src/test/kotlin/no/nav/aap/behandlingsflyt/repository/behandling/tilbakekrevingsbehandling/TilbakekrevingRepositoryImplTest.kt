package no.nav.aap.behandlingsflyt.repository.behandling.tilbakekrevingsbehandling

import no.nav.aap.behandlingsflyt.behandling.tilbakekrevingsbehandling.TilbakekrevingBehandlingsstatus
import no.nav.aap.behandlingsflyt.behandling.tilbakekrevingsbehandling.Tilbakekrevingshendelse
import no.nav.aap.behandlingsflyt.help.FakePdlGateway
import no.nav.aap.behandlingsflyt.repository.sak.PersonRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.SakRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonOgSakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.test.ident
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Beløp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.net.URI
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class TilbakekrevingRepositoryImplTest {

    companion object {
        val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))

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
    fun `to hendelser på samme behandling`() {
        dataSource.transaction { connection ->
            val sak = sak(connection)
            val nå = LocalDateTime.now()
            val hendelse = Tilbakekrevingshendelse(
                eksternFagsakId = "123",
                hendelseOpprettet = nå,
                eksternBehandlingId = UUID.randomUUID().toString(),
                sakOpprettet = nå,
                varselSendt = nå,
                behandlingsstatus = TilbakekrevingBehandlingsstatus.OPPRETTET,
                totaltFeilutbetaltBeløp = Beløp(1000),
                saksbehandlingURL = URI.create("https://nav.no"),
                fullstendigPeriode = periode,
                versjon = 1
            )

            val repo = TilbakekrevingRepositoryImpl(connection)

            repo.lagre(sak.id, hendelse)
            val behandlinger = repo.hent(sak.id)

            assertThat(behandlinger).hasSize(1)
            assertThat(behandlinger.first().behandlingsstatus).isEqualTo(TilbakekrevingBehandlingsstatus.OPPRETTET)


            repo.lagre(sak.id, hendelse.copy(behandlingsstatus = TilbakekrevingBehandlingsstatus.TIL_BEHANDLING))
            val behandlingerEtterHendelse2 = repo.hent(sak.id)

            assertThat(behandlingerEtterHendelse2).hasSize(1)
            assertThat(behandlingerEtterHendelse2.first().behandlingsstatus).isEqualTo(TilbakekrevingBehandlingsstatus.TIL_BEHANDLING)
        }

    }

    @Test
    fun `to hendelser på forskjellig behandling`() {
        dataSource.transaction { connection ->
            val sak = sak(connection)
            val nå = LocalDateTime.now()
            val hendelse = Tilbakekrevingshendelse(
                eksternFagsakId = "123",
                hendelseOpprettet = nå,
                eksternBehandlingId = UUID.randomUUID().toString(),
                sakOpprettet = nå,
                varselSendt = nå,
                behandlingsstatus = TilbakekrevingBehandlingsstatus.OPPRETTET,
                totaltFeilutbetaltBeløp = Beløp(1000),
                saksbehandlingURL = URI.create("https://nav.no"),
                fullstendigPeriode = periode,
                versjon = 1
            )

            val repo = TilbakekrevingRepositoryImpl(connection)

            repo.lagre(sak.id, hendelse)
            val behandlinger = repo.hent(sak.id)

            assertThat(behandlinger).hasSize(1)
            assertThat(behandlinger.first().behandlingsstatus).isEqualTo(TilbakekrevingBehandlingsstatus.OPPRETTET)


            repo.lagre(sak.id, hendelse.copy(eksternBehandlingId = UUID.randomUUID().toString()))
            val behandlingerEtterHendelse2 = repo.hent(sak.id)

            assertThat(behandlingerEtterHendelse2).hasSize(2)
        }

    }

    private fun sak(connection: DBConnection): Sak {
        return PersonOgSakService(
            FakePdlGateway,
            PersonRepositoryImpl(connection),
            SakRepositoryImpl(connection)
        ).finnEllerOpprett(ident(), periode)
    }
}