package no.nav.aap.behandlingsflyt

import no.nav.aap.behandlingsflyt.behandling.avbrytrevurdering.AvbrytRevurderingVurdering
import no.nav.aap.behandlingsflyt.behandling.avbrytrevurdering.AvbrytRevurderingÅrsak
import no.nav.aap.behandlingsflyt.behandling.søknad.AarsakTilTrekkSoknad
import no.nav.aap.behandlingsflyt.behandling.søknad.TrukketSøknadVurdering
import no.nav.aap.behandlingsflyt.datadeling.SakStatus
import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.help.opprettRevurdering
import no.nav.aap.behandlingsflyt.help.sak
import no.nav.aap.behandlingsflyt.hendelse.datadeling.ApiInternGateway
import no.nav.aap.behandlingsflyt.integrasjon.createGatewayProvider
import no.nav.aap.behandlingsflyt.prosessering.datadeling.DatadelingMeldePerioderOgSakStatusJobbUtfører
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.avbrytrevurdering.AvbrytRevurderingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.søknad.TrukketSøknadRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.postgresRepositoryRegistry
import no.nav.aap.behandlingsflyt.test.AlleAvskruddUnleash
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.motor.testutil.ManuellMotorImpl
import no.nav.aap.verdityper.dokument.JournalpostId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant

class BackfillSakstatusDatadelingTest {
    companion object {
        private lateinit var dataSource: TestDataSource
        private lateinit var motor: ManuellMotorImpl

        @BeforeAll
        @JvmStatic
        fun setUp() {
            dataSource = TestDataSource()
            motor = ManuellMotorImpl(
                dataSource,
                jobber = listOf(DatadelingMeldePerioderOgSakStatusJobbUtfører),
                repositoryRegistry = postgresRepositoryRegistry,
                gatewayProvider = createGatewayProvider {
                    register<AlleAvskruddUnleash>()
                    register<FakeCapturingApiInternGateway>()
                }
            )
            motor.start()
        }

        @AfterAll
        @JvmStatic
        fun tearDown() {
            motor.stop()
            dataSource.close()
        }
    }

    @BeforeEach
    fun clearFakeGateway() {
        FakeCapturingApiInternGateway.sendteSakStatuser.clear()
    }

    @Test
    fun `oppretter to jobber for sak med både trukket søknad og avbrutt revurdering, og kaller api-intern ved kjøring`() {
        val sak = dataSource.transaction { sak(it) }
        val førstegangsbehandling = dataSource.transaction { finnEllerOpprettBehandling(it, sak) }

        val revurdering = dataSource.transaction { connection -> opprettRevurdering(connection, sak) }

        dataSource.transaction { connection ->
            TrukketSøknadRepositoryImpl(connection).lagreTrukketSøknadVurdering(
                førstegangsbehandling.id,
                TrukketSøknadVurdering(
                    journalpostId = JournalpostId("123"),
                    begrunnelse = "Bruker ønsker ikke lenger søknaden behandlet",
                    skalTrekkes = true,
                    vurdertAv = Bruker("Z999999"),
                    vurdert = Instant.now(),
                    aarsak = AarsakTilTrekkSoknad.BRUKER_ONSKER_IKKE_SOKE_LENGER,
                )
            )
        }

        dataSource.transaction { connection ->
            AvbrytRevurderingRepositoryImpl(connection).lagre(
                revurdering.id,
                AvbrytRevurderingVurdering(
                    årsak = AvbrytRevurderingÅrsak.REVURDERINGEN_BLE_OPPRETTET_VED_EN_FEIL,
                    begrunnelse = "Revurderingen er ikke lenger aktuell",
                    vurdertAv = Bruker("Z999999"),
                )
            )
        }

        val antallJobber = dataSource.transaction { connection ->
            BackfillSakstatusDatadeling.enqueueBackfillJobberForSak(connection, sak.id)
        }
        assertThat(antallJobber).isEqualTo(2)

        motor.kjørJobber()

        assertThat(FakeCapturingApiInternGateway.sendteSakStatuser).hasSize(2)
    }
}
