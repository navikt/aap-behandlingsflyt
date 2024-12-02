package no.nav.aap.behandlingsflyt.forretningsflyt.gjenopptak

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepositoryImpl
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.FakePdlGateway
import no.nav.aap.behandlingsflyt.test.ident
import no.nav.aap.behandlingsflyt.faktagrunnlag.SakOgBehandlingService
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingFlytRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Årsak
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonOgSakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.ÅrsakTilBehandling
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class GjenopptakRepositoryTest {

    companion object {
        val dataSource = InitTestDatabase.dataSource
    }

    @Test
    fun `skal finne hvilke behandlinger hvor fristen har utløpt`() {
        dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = behandling(connection, sak)
            BehandlingFlytRepository(connection).oppdaterBehandlingStatus(
                behandlingId = behandling.id,
                status = Status.UTREDES
            )
            val avklaringsbehovRepository = AvklaringsbehovRepositoryImpl(connection)

            val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(behandling.id)

            avklaringsbehovene.leggTil(listOf(Definisjon.MANUELT_SATT_PÅ_VENT), StegType.START_BEHANDLING)
        }

        dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = behandling(connection, sak)
            BehandlingFlytRepository(connection).oppdaterBehandlingStatus(
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
            val kandidater = GjenopptakRepository(connection).finnBehandlingerForGjennopptak()

            assertThat(kandidater).hasSize(1)
        }
    }

    private fun sak(connection: DBConnection): Sak {
        return PersonOgSakService(connection, FakePdlGateway).finnEllerOpprett(
            ident(),
            Periode(LocalDate.now().minusYears(1), LocalDate.now().plusYears(2))
        )
    }

    private fun behandling(connection: DBConnection, sak: Sak): Behandling {
        return SakOgBehandlingService(connection).finnEllerOpprettBehandling(
            sak.saksnummer,
            listOf(Årsak(ÅrsakTilBehandling.MOTTATT_SØKNAD))
        ).behandling
    }
}