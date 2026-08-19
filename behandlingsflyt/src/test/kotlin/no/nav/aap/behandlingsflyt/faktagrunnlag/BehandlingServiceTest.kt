package no.nav.aap.behandlingsflyt.faktagrunnlag

import no.nav.aap.behandlingsflyt.help.opprettSak
import no.nav.aap.behandlingsflyt.integrasjon.createGatewayProvider
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.postgresRepositoryRegistry
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingService
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovOgÅrsak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.test.AlleAvskruddUnleash
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import kotlin.test.Test

class BehandlingServiceTest {
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


    private val gatewayProvider = createGatewayProvider { register<AlleAvskruddUnleash>() }

    @Test
    fun `gjenbruker åpen behandling hvis vi prøver å opprette enda en ny behandling etter et meldekort`() {
        dataSource.transaction { connection ->
            val behandlingService =
                BehandlingService(postgresRepositoryRegistry.provider(connection), gatewayProvider)
            val behandlingRepository = BehandlingRepositoryImpl(connection)
            val sak = opprettSak(connection, 1 januar 2020)

            /* Førstegangsbehandling */
            behandlingService.finnEllerOpprettBehandling(
                sak.id,
                VurderingsbehovOgÅrsak(
                    listOf(VurderingsbehovMedPeriode(Vurderingsbehov.MOTTATT_SØKNAD)),
                    ÅrsakTilOpprettelse.SØKNAD,
                )
            ).åpenBehandling!!.also {
                behandlingRepository.oppdaterBehandlingStatus(it.id, Status.AVSLUTTET)
            }

            /* Åpen revurdering */
            behandlingService.finnEllerOpprettBehandling(
                sak.id,
                VurderingsbehovOgÅrsak(
                    listOf(VurderingsbehovMedPeriode(Vurderingsbehov.REVURDER_SAMORDNING)),
                    ÅrsakTilOpprettelse.MANUELL_OPPRETTELSE,
                )
            )

            /* Send inn meldekort */
            val meldekortbehandling = (behandlingService.finnEllerOpprettBehandling(
                sak.id,
                VurderingsbehovOgÅrsak(
                    listOf(VurderingsbehovMedPeriode(Vurderingsbehov.MOTTATT_MELDEKORT)),
                    ÅrsakTilOpprettelse.MELDEKORT,
                )
            ) as BehandlingService.MåBehandlesAtomært).nyBehandling
            behandlingRepository.oppdaterBehandlingStatus(meldekortbehandling.id, Status.AVSLUTTET)

            /* Forsøk å opprett en til revurdering (f.eks. nye opplysninger fra register). */
            behandlingService.finnEllerOpprettBehandling(
                sak.id,
                VurderingsbehovOgÅrsak(
                    listOf(VurderingsbehovMedPeriode(Vurderingsbehov.INSTITUSJONSOPPHOLD)),
                    ÅrsakTilOpprettelse.ENDRING_I_REGISTERDATA,
                )
            )

            val behandlinger = behandlingRepository.hentAlleFor(sak.id)
            assertThat(behandlinger)
                .hasSize(3)
        }
    }

    @Test
    fun `med to åpne behandlinger samtidig skal det ikke være ulik sortering på hva som er forrige behandlingId`() {
        dataSource.transaction { connection ->
            val behandlingService =
                BehandlingService(postgresRepositoryRegistry.provider(connection), gatewayProvider)

            val b1 = Behandling(
                id = BehandlingId(1L),
                forrigeBehandlingId = null,
                typeBehandling = TypeBehandling.Førstegangsbehandling,
                sakId = SakId(1L),
                årsakTilOpprettelse = ÅrsakTilOpprettelse.SØKNAD,
                versjon = 1L
            )
            val b2 = Behandling(
                id = BehandlingId(2L),
                forrigeBehandlingId = b1.id,
                typeBehandling = TypeBehandling.Revurdering,
                sakId = SakId(1L),
                årsakTilOpprettelse = ÅrsakTilOpprettelse.SØKNAD,
                versjon = 1L
            )
            val b3 = Behandling(
                id = BehandlingId(3L),
                forrigeBehandlingId = b2.id,
                typeBehandling = TypeBehandling.Revurdering,
                sakId = SakId(1L),
                årsakTilOpprettelse = ÅrsakTilOpprettelse.SØKNAD,
                versjon = 1L
            )
            val b3_1 = Behandling(
                id = BehandlingId(4L),
                forrigeBehandlingId = b3.id,
                typeBehandling = TypeBehandling.Revurdering,
                sakId = SakId(1L),
                årsakTilOpprettelse = ÅrsakTilOpprettelse.SØKNAD,
                versjon = 1L
            )
            val b3_2 = Behandling(
                id = BehandlingId(5L),
                forrigeBehandlingId = b3.id,
                typeBehandling = TypeBehandling.Revurdering,
                sakId = SakId(1L),
                årsakTilOpprettelse = ÅrsakTilOpprettelse.SØKNAD,
                versjon = 1L
            )

            val alleBehandlinger = listOf(b1, b2, b3, b3_1, b3_2)
            val alleBehandlingerAnnetUtplukk = listOf(b1, b3, b2, b3_1, b3_2)
            val alleBehandlingerSortert = alleBehandlinger.sortedWith(behandlingService.comparator(alleBehandlinger))
            val alleBehandlingerSortertAlternativt =
                alleBehandlingerAnnetUtplukk.sortedWith(behandlingService.comparator(alleBehandlingerAnnetUtplukk))

            assertThat(alleBehandlingerSortert).isEqualTo(alleBehandlingerSortertAlternativt)

        }
    }
}