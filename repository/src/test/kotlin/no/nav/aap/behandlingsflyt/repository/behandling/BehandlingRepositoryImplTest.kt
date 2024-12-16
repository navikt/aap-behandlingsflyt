package no.nav.aap.behandlingsflyt.repository.behandling

import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.repository.avklaringsbehov.FakePdlGateway
import no.nav.aap.behandlingsflyt.repository.sak.PersonRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.SakRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Årsak
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.ÅrsakTilBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonOgSakService
import no.nav.aap.behandlingsflyt.test.ident
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class BehandlingRepositoryImplTest {
    @Test
    fun `kan lagre og hente ut behandling med uuid`() {
        val skapt = InitTestDatabase.dataSource.transaction { connection ->
            val sak = PersonOgSakService(
                FakePdlGateway,
                PersonRepositoryImpl(connection),
                SakRepositoryImpl(connection)
            ).finnEllerOpprett(
                ident(),
                Periode(LocalDate.now(), LocalDate.now().plusYears(3))
            )
            val repo = BehandlingRepositoryImpl(connection)

            // Opprett
            repo.opprettBehandling(
                sakId = sak.id,
                årsaker = listOf(Årsak(type = ÅrsakTilBehandling.MOTTATT_SØKNAD)),
                typeBehandling = TypeBehandling.Førstegangsbehandling,
                forrigeBehandlingId = null
            )
        }

        InitTestDatabase.dataSource.transaction { connection ->
            val repo = BehandlingRepositoryImpl(connection)

            // Hent ut igjen
            val hententMedReferanse = repo.hent(skapt.referanse)

            assertThat(hententMedReferanse.referanse).isEqualTo(skapt.referanse)
            assertThat(hententMedReferanse.årsaker()).containsExactlyElementsOf(skapt.årsaker())
            assertThat(hententMedReferanse.årsaker()).containsExactlyElementsOf(listOf(Årsak(type = ÅrsakTilBehandling.MOTTATT_SØKNAD)))
            assertThat(hententMedReferanse.typeBehandling()).isEqualTo(TypeBehandling.Førstegangsbehandling)
        }
    }

    @Test
    fun `oppretet dato lagres på behandling og hentes ut korrekt`() {
        val skapt = InitTestDatabase.dataSource.transaction { connection ->
            val sak = PersonOgSakService(
                FakePdlGateway,
                PersonRepositoryImpl(connection),
                SakRepositoryImpl(connection)
            ).finnEllerOpprett(
                ident(),
                Periode(LocalDate.now(), LocalDate.now().plusYears(3))
            )
            val repo = BehandlingRepositoryImpl(connection)

            // Opprett
            repo.opprettBehandling(
                sakId = sak.id,
                årsaker = listOf(Årsak(type = ÅrsakTilBehandling.MOTTATT_SØKNAD)),
                typeBehandling = TypeBehandling.Førstegangsbehandling,
                forrigeBehandlingId = null
            )
        }

        InitTestDatabase.dataSource.transaction { connection ->
            val repo = BehandlingRepositoryImpl(connection)

            // Hent ut igjen
            val hententMedReferanse = repo.hent(skapt.referanse)

            assertThat(hententMedReferanse.opprettetTidspunkt).isEqualTo(skapt.opprettetTidspunkt);
        }
    }
}