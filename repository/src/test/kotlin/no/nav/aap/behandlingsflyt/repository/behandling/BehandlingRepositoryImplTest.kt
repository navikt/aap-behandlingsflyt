package no.nav.aap.behandlingsflyt.repository.behandling

import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.repository.avklaringsbehov.FakePdlGateway
import no.nav.aap.behandlingsflyt.repository.behandling.vedtak.VedtakRepositoryImpl
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
import java.time.LocalDateTime

internal class BehandlingRepositoryImplTest {

    private val dataSource = InitTestDatabase.freshDatabase()

    @Test
    fun `kan lagre og hente ut behandling med uuid`() {
        val skapt = dataSource.transaction { connection ->
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

        dataSource.transaction { connection ->
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
        val skapt = dataSource.transaction { connection ->
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

        dataSource.transaction { connection ->
            val repo = BehandlingRepositoryImpl(connection)

            // Hent ut igjen
            val hententMedReferanse = repo.hent(skapt.referanse)

            assertThat(hententMedReferanse.opprettetTidspunkt).isEqualTo(skapt.opprettetTidspunkt);
        }
    }

    @Test
    fun `kan hente ut behandlinger for sak filtrert på type`() {
        val (sak, førstegang, klage) = dataSource.transaction { connection ->
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
            val førstegang = repo.opprettBehandling(
                sakId = sak.id,
                årsaker = listOf(Årsak(type = ÅrsakTilBehandling.MOTTATT_SØKNAD)),
                typeBehandling = TypeBehandling.Førstegangsbehandling,
                forrigeBehandlingId = null
            )

            val klage = repo.opprettBehandling(
                sakId = sak.id,
                årsaker = listOf(Årsak(type = ÅrsakTilBehandling.MOTATT_KLAGE)),
                typeBehandling = TypeBehandling.Klage,
                forrigeBehandlingId = null
            )
            Triple(sak, førstegang, klage)
        }

        dataSource.transaction { connection ->
            val repo = BehandlingRepositoryImpl(connection)

            // Hent ut igjen
            val alleDefault = repo.hentAlleFor(sak.id)
            assertThat(alleDefault).hasSize(2)

            val alleFørstegang = repo.hentAlleFor(sak.id, listOf(TypeBehandling.Førstegangsbehandling))
            assertThat(alleFørstegang).hasSize(1)
            assertThat(alleFørstegang[0].referanse).isEqualTo(førstegang.referanse)

            val alleKlage = repo.hentAlleFor(sak.id, listOf(TypeBehandling.Klage))
            assertThat(alleKlage).hasSize(1)
            assertThat(alleKlage[0].referanse).isEqualTo(klage.referanse)
        }
    }
    
    @Test
    fun `kan hente ut behandlinger med vedtak`() {
        val vedtakstidspunkt = LocalDateTime.now()
        val virkningstidspunkt = LocalDate.now().plusMonths(1)
        
        val (sak, førstegang, klage) = dataSource.transaction { connection ->
            val sak = PersonOgSakService(
                FakePdlGateway,
                PersonRepositoryImpl(connection),
                SakRepositoryImpl(connection)
            ).finnEllerOpprett(
                ident(),
                Periode(LocalDate.now(), LocalDate.now().plusYears(3))
            )
            val repo = BehandlingRepositoryImpl(connection)
            val vedtakRepo = VedtakRepositoryImpl(connection)

            // Opprett
            val førstegang = repo.opprettBehandling(
                sakId = sak.id,
                årsaker = listOf(Årsak(type = ÅrsakTilBehandling.MOTTATT_SØKNAD)),
                typeBehandling = TypeBehandling.Førstegangsbehandling,
                forrigeBehandlingId = null
            )
            
            vedtakRepo.lagre(
                behandlingId = førstegang.id,
                vedtakstidspunkt = vedtakstidspunkt,
                virkningstidspunkt = virkningstidspunkt,
            )

            val klage = repo.opprettBehandling(
                sakId = sak.id,
                årsaker = listOf(Årsak(type = ÅrsakTilBehandling.MOTATT_KLAGE)),
                typeBehandling = TypeBehandling.Klage,
                forrigeBehandlingId = null
            )
            Triple(sak, førstegang, klage)
        }

        dataSource.transaction { connection ->
            val repo = BehandlingRepositoryImpl(connection)

            // Hent ut igjen
            val alleDefault = repo.hentAlleMedVedtakFor(sak.id)
            assertThat(alleDefault).hasSize(1)

            val alleFørstegang = repo.hentAlleMedVedtakFor(sak.id, listOf(TypeBehandling.Førstegangsbehandling))
            assertThat(alleFørstegang).hasSize(1)
            assertThat(alleFørstegang[0].referanse).isEqualTo(førstegang.referanse)
            assertThat(alleFørstegang[0].vedtakstidspunkt).isEqualToIgnoringNanos(vedtakstidspunkt)
            assertThat(alleFørstegang[0].virkningstidspunkt).isEqualTo(virkningstidspunkt)
        }
    }
}