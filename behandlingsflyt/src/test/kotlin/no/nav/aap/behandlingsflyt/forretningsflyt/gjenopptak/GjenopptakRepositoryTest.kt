package no.nav.aap.behandlingsflyt.forretningsflyt.gjenopptak

import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.help.sak
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.repository.avklaringsbehov.AvklaringsbehovRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class GjenopptakRepositoryTest {
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
    fun `skal finne hvilke behandlinger hvor fristen har utløpt`() {
        dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)
            BehandlingRepositoryImpl(connection).oppdaterBehandlingStatus(
                behandlingId = behandling.id,
                status = Status.UTREDES
            )
            val avklaringsbehovRepository = AvklaringsbehovRepositoryImpl(connection)

            val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(behandling.id)

            avklaringsbehovene.leggTil(listOf(Definisjon.MANUELT_SATT_PÅ_VENT), StegType.START_BEHANDLING)
        }

        dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)
            BehandlingRepositoryImpl(connection).oppdaterBehandlingStatus(
                behandlingId = behandling.id,
                status = Status.UTREDES
            )
            val avklaringsbehovRepository = AvklaringsbehovRepositoryImpl(connection)

            val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(behandling.id)

            avklaringsbehovene.leggTil(
                listOf(Definisjon.MANUELT_SATT_PÅ_VENT),
                StegType.START_BEHANDLING,
                frist = LocalDate.now().minusDays(1)
            )
        }

        dataSource.transaction { connection ->
            val kandidater = GjenopptakRepositoryImpl(connection).finnBehandlingerForGjennopptak()

            assertThat(kandidater).hasSize(1)
        }
    }
}