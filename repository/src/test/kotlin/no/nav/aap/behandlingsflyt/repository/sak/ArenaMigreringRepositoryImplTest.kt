package no.nav.aap.behandlingsflyt.repository.sak

import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.help.opprettSak
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovOgÅrsak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.ArenaMigrering
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
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

    @Test
    fun `slett fjerner migreringen for saken til behandlingen og er idempotent`() {
        val (sak, behandlingId) = dataSource.transaction { connection ->
            val sak = opprettSak(connection, LocalDate.now())
            sak to finnEllerOpprettBehandling(connection, sak).id
        }
        val annenSak = dataSource.transaction { connection -> opprettSak(connection, LocalDate.now()) }

        dataSource.transaction { connection ->
            val repo = ArenaMigreringRepositoryImpl(connection)
            repo.lagre(migrering(sak.id, "2018-111111", sak.person.aktivIdent().identifikator))
            repo.lagre(migrering(annenSak.id, "2018-222222", annenSak.person.aktivIdent().identifikator))
        }

        dataSource.transaction { connection ->
            val repo = ArenaMigreringRepositoryImpl(connection)
            repo.slett(behandlingId)
            repo.slett(behandlingId)
        }

        dataSource.transaction { connection ->
            val repo = ArenaMigreringRepositoryImpl(connection)
            assertThat(repo.hentForSakHvisEksisterer(sak.id)).isNull()
            assertThat(repo.hentForSakHvisEksisterer(annenSak.id)).isNotNull
        }
    }

    @Test
    fun `slett med en annen behandling enn førstegangsbehandlingen beholder migreringen`() {
        val (sak, klagebehandlingId) = dataSource.transaction { connection ->
            val sak = opprettSak(connection, LocalDate.now())
            finnEllerOpprettBehandling(connection, sak)
            val klage = BehandlingRepositoryImpl(connection).opprettBehandling(
                sakId = sak.id,
                typeBehandling = TypeBehandling.Klage,
                forrigeBehandlingId = null,
                vurderingsbehovOgÅrsak = VurderingsbehovOgÅrsak(
                    vurderingsbehov = listOf(VurderingsbehovMedPeriode(Vurderingsbehov.MOTATT_KLAGE)),
                    årsak = ÅrsakTilOpprettelse.KLAGE,
                ),
            )
            sak to klage.id
        }

        dataSource.transaction { connection ->
            ArenaMigreringRepositoryImpl(connection)
                .lagre(migrering(sak.id, "2018-333333", sak.person.aktivIdent().identifikator))
        }

        dataSource.transaction { connection ->
            ArenaMigreringRepositoryImpl(connection).slett(klagebehandlingId)
        }

        val hentet = dataSource.transaction { connection ->
            ArenaMigreringRepositoryImpl(connection).hentForSakHvisEksisterer(sak.id)
        }
        assertThat(hentet).isNotNull
    }

    private fun migrering(sakId: SakId, saksnummerArena: String, ident: String) = ArenaMigrering(
        sakId = sakId,
        saksnummerArena = saksnummerArena,
        ident = ident,
        migrertTidspunkt = LocalDateTime.of(2024, 6, 15, 12, 30, 0),
    )
}
