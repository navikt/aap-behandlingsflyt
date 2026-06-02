package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.krav

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.Gjenopptak
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.MuligRettFraÅrsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.NyttKrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.SøknadsdatoÅrsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.TrukketSøknad
import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.help.sak
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovOgÅrsak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.verdityper.dokument.JournalpostId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.time.LocalDateTime

internal class KravRepositoryImplTest {

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

        private fun nyttKrav(behandlingId: BehandlingId) = NyttKrav(
            journalpostId = JournalpostId("JP-001"),
            vurdertAv = "Z123456",
            vurdertTidspunkt = LocalDateTime.of(2024, 1, 15, 10, 0),
            begrunnelse = "Standard krav om AAP",
            vurdertIBehandling = behandlingId,
            opprettet = java.time.Instant.now(),
            soknadsdato = 1 januar 2024,
            soknadsdatoÅrsak = SøknadsdatoÅrsak.BrukerHarSøktTidligere,
            muligRettFra = 15 januar 2024,
            muligRettFraÅrsak = MuligRettFraÅrsak.IkkeIStandTilÅSøkeTidligere,
            kravdato = 1 januar 2024,
        )

        private fun gjenopptak(behandlingId: BehandlingId) = Gjenopptak(
            journalpostId = JournalpostId("JP-002"),
            vurdertAv = "Kelvin",
            vurdertTidspunkt = LocalDateTime.of(2024, 2, 1, 9, 0),
            begrunnelse = "",
            vurdertIBehandling = behandlingId,
            opprettet = java.time.Instant.now(),
            soknadsdato = null,
            soknadsdatoÅrsak = null,
            muligRettFra = null,
            muligRettFraÅrsak = null,
            kravdato = 1 januar 2024,
        )
    }

    @Test
    fun `kan lagre og hente tom liste`() {
        dataSource.transaction { connection ->
            val repo = KravRepositoryImpl(connection)
            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)

            repo.lagre(behandling.id, emptyList())

            assertThat(repo.hent(behandling.id).vurderinger).isEmpty()
        }
    }

    @Test
    fun `kan lagre og hente NyttKrav med alle felt`() {
        dataSource.transaction { connection ->
            val repo = KravRepositoryImpl(connection)
            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)
            val vurdering = nyttKrav(behandling.id)

            repo.lagre(behandling.id, listOf(vurdering))

            val hentet = repo.hent(behandling.id)
            assertThat(hentet.vurderinger).usingRecursiveComparison()
                .ignoringFields("opprettet")
                .isEqualTo(listOf(vurdering))
        }
    }

    @Test
    fun `kan lagre og hente Gjenopptak med bare obligatoriske felt`() {
        dataSource.transaction { connection ->
            val repo = KravRepositoryImpl(connection)
            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)
            val vurdering = gjenopptak(behandling.id)

            repo.lagre(behandling.id, listOf(vurdering))

            val hentet = repo.hent(behandling.id)
            assertThat(hentet.vurderinger).usingRecursiveComparison()
                .ignoringFields("opprettet")
                .isEqualTo(listOf(vurdering))
        }
    }

    @Test
    fun `kan lagre og hente flere vurderinger av ulik type`() {
        dataSource.transaction { connection ->
            val repo = KravRepositoryImpl(connection)
            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)
            val v1 = nyttKrav(behandling.id)
            val v2 = gjenopptak(behandling.id)

            repo.lagre(behandling.id, listOf(v1, v2))

            val hentet = repo.hent(behandling.id)
            assertThat(hentet.vurderinger).usingRecursiveComparison()
                .ignoringFields("opprettet")
                .isEqualTo(listOf(v1, v2))
        }
    }

    @Test
    fun `lagre ny versjon deaktiverer forrige grunnlag`() {
        dataSource.transaction { connection ->
            val repo = KravRepositoryImpl(connection)
            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)

            val v1 = nyttKrav(behandling.id)
            repo.lagre(behandling.id, listOf(v1))

            val oppdatert = v1.copy(begrunnelse = "Oppdatert begrunnelse")
            repo.lagre(behandling.id, listOf(oppdatert))

            val hentet = repo.hent(behandling.id)
            assertThat(hentet.vurderinger).hasSize(1)
            assertThat(hentet.vurderinger.first().begrunnelse).isEqualTo("Oppdatert begrunnelse")
        }
    }

    @Test
    fun `hentHvisEksisterer returnerer null når ingen grunnlag finnes`() {
        dataSource.transaction { connection ->
            val repo = KravRepositoryImpl(connection)
            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)

            assertThat(repo.hentHvisEksisterer(behandling.id)).isNull()
        }
    }

    @Test
    fun `kopier kopierer aktivt grunnlag til ny behandling`() {
        val (fraBehandling, vurdering) = dataSource.transaction { connection ->
            val repo = KravRepositoryImpl(connection)
            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)
            val vurdering = nyttKrav(behandling.id)

            repo.lagre(behandling.id, listOf(vurdering))
            Pair(behandling, vurdering)
        }

        dataSource.transaction { connection ->
            val repo = KravRepositoryImpl(connection)
            val nyBehandling = revurdering(connection, fraBehandling.id)

            repo.kopier(fraBehandling.id, nyBehandling.id)

            val kopiert = repo.hentHvisEksisterer(nyBehandling.id)
            assertThat(kopiert).isNotNull()
            assertThat(kopiert!!.vurderinger).usingRecursiveComparison()
                .ignoringFields("opprettet")
                .isEqualTo(listOf(vurdering))
        }
    }

    @Test
    fun `slett fjerner alle rader uten feil`() {
        TestDataSource().use { ds ->
            ds.transaction { connection ->
                val repo = KravRepositoryImpl(connection)
                val sak = sak(connection)
                val behandling = finnEllerOpprettBehandling(connection, sak)

                repo.lagre(
                    behandling.id, listOf(
                        TrukketSøknad(
                            journalpostId = JournalpostId("JP-SLETT"),
                            vurdertAv = "Z000001",
                            vurdertTidspunkt = LocalDateTime.now(),
                            begrunnelse = "Søker trakk søknaden",
                            vurdertIBehandling = behandling.id,
                            opprettet = java.time.Instant.now(),
                        )
                    )
                )

                assertDoesNotThrow { repo.slett(behandling.id) }
                assertThat(repo.hentHvisEksisterer(behandling.id)).isNull()
            }
        }
    }

    private fun revurdering(connection: DBConnection, fraBehandlingId: BehandlingId) =
        BehandlingRepositoryImpl(connection).opprettBehandling(
            BehandlingRepositoryImpl(connection).hent(fraBehandlingId).sakId,
            typeBehandling = TypeBehandling.Revurdering,
            forrigeBehandlingId = fraBehandlingId,
            vurderingsbehovOgÅrsak = VurderingsbehovOgÅrsak(
                vurderingsbehov = listOf(VurderingsbehovMedPeriode(Vurderingsbehov.MOTTATT_SØKNAD)),
                årsak = ÅrsakTilOpprettelse.MANUELL_OPPRETTELSE
            )
        )
}

