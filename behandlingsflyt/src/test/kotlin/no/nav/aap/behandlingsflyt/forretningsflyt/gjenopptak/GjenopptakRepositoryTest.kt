package no.nav.aap.behandlingsflyt.forretningsflyt.gjenopptak

import no.nav.aap.behandlingsflyt.behandling.søknad.TrukketSøknadService
import no.nav.aap.behandlingsflyt.faktagrunnlag.FakePdlGateway
import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.repository.avklaringsbehov.AvklaringsbehovRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.PersonRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.SakRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonOgSakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.test.ident
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryAvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryTrukketSøknadRepository
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class GjenopptakRepositoryTest {

    companion object {
        private val dataSource = InitTestDatabase.freshDatabase()
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

    private fun sak(connection: DBConnection): Sak {
        return PersonOgSakService(
            FakePdlGateway,
            PersonRepositoryImpl(connection),
            SakRepositoryImpl(connection),
            BehandlingRepositoryImpl(connection),
            TrukketSøknadService(
                InMemoryAvklaringsbehovRepository,
                InMemoryTrukketSøknadRepository
            ),
        ).finnEllerOpprett(
            ident(),
            Periode(LocalDate.now().minusYears(1), LocalDate.now().plusYears(2))
        )
    }
}