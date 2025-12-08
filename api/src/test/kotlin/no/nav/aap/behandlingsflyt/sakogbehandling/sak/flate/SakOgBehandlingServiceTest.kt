package no.nav.aap.behandlingsflyt.sakogbehandling.sak.flate

import no.nav.aap.behandlingsflyt.behandling.tilbakekrevingsbehandling.TilbakekrevingBehandlingsstatus
import no.nav.aap.behandlingsflyt.behandling.tilbakekrevingsbehandling.Tilbakekrevingshendelse
import no.nav.aap.behandlingsflyt.help.FakePdlGateway
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.behandling.tilbakekrevingsbehandling.TilbakekrevingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.postgresRepositoryRegistry
import no.nav.aap.behandlingsflyt.repository.sak.PersonRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.SakRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovOgÅrsak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonOgSakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.test.ident
import no.nav.aap.behandlingsflyt.test.januar
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

class SakOgBehandlingServiceTest {

    @Test
    fun `sak uten behandlinger`() {
        dataSource.transaction { connection ->
            val sakOgBehandlingService = SakOgBehandlingService(postgresRepositoryRegistry.provider(connection))
            val sak = sak(connection)
            val resultat = sakOgBehandlingService.finnSakOgBehandlinger(sak.saksnummer)
            assertThat(resultat.behandlinger).hasSize(0)
        }
    }

    @Test
    fun `sak med en vanlig behandling`() {
        dataSource.transaction { connection ->
            val sakOgBehandlingService = SakOgBehandlingService(postgresRepositoryRegistry.provider(connection))
            val sak = sak(connection)
            val behandlingId1 = behandling(connection, sak)
            val resultat = sakOgBehandlingService.finnSakOgBehandlinger(sak.saksnummer)
            assertThat(resultat.behandlinger).hasSize(1)
        }
    }

    @Test
    fun `sak med en vanlig behandling og en tilbakekrevingsbehandling`() {
        dataSource.transaction { connection ->
            val sakOgBehandlingService = SakOgBehandlingService(postgresRepositoryRegistry.provider(connection))
            val sak = sak(connection)
            behandling(connection, sak)
            tilbakekrevingBehandling(connection, sak)
            val resultat = sakOgBehandlingService.finnSakOgBehandlinger(sak.saksnummer)
            assertThat(resultat.behandlinger).hasSize(2)
        }
    }

    private fun sak(connection: DBConnection): Sak {
        return PersonOgSakService(
            FakePdlGateway,
            PersonRepositoryImpl(connection),
            SakRepositoryImpl(connection)
        ).finnEllerOpprett(ident(),
            periode
        )
    }

    private fun behandling(connection: DBConnection, sak: Sak): BehandlingId {
        val behandling = BehandlingRepositoryImpl(connection).opprettBehandling(
            sakId = sak.id,
            typeBehandling = TypeBehandling.Førstegangsbehandling,
            forrigeBehandlingId = null,
            vurderingsbehovOgÅrsak = VurderingsbehovOgÅrsak(
                vurderingsbehov = listOf(
                    VurderingsbehovMedPeriode(
                        type = Vurderingsbehov.MOTTATT_SØKNAD,
                        periode = Periode(1 januar 2025, 1 januar 2028),
                    )
                ),
                årsak = ÅrsakTilOpprettelse.SØKNAD,
            )
        )

        return behandling.id
    }

    private fun tilbakekrevingBehandling(connection: DBConnection, sak: Sak) {
        TilbakekrevingRepositoryImpl(connection).lagre(sak.id, Tilbakekrevingshendelse(
            tilbakekrevingBehandlingId = UUID.randomUUID(),
            eksternFagsakId = sak.saksnummer.toString(),
            hendelseOpprettet = LocalDateTime.now(),
            eksternBehandlingId = UUID.randomUUID().toString(),
            sakOpprettet = LocalDateTime.now(),
            varselSendt = LocalDateTime.now(),
            behandlingsstatus = TilbakekrevingBehandlingsstatus.OPPRETTET,
            totaltFeilutbetaltBeløp = Beløp(1000),
            tilbakekrevingSaksbehandlingUrl = URI.create("https://localhost"),
            fullstendigPeriode = periode,
            versjon = 1
        ))
    }

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

}

