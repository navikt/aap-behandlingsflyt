package no.nav.aap.behandlingsflyt.faktagrunnlag

import no.nav.aap.behandlingsflyt.help.opprettSak
import no.nav.aap.behandlingsflyt.integrasjon.createGatewayProvider
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.postgresRepositoryRegistry
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingService
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovOgÅrsak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
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
    fun `hver søknad om opphold i utlandet gir en egen behandling uavhengig av ytelsesbehandlingen`() {
        dataSource.transaction { connection ->
            val behandlingService =
                BehandlingService(postgresRepositoryRegistry.provider(connection), gatewayProvider)
            val behandlingRepository = BehandlingRepositoryImpl(connection)
            val sak = opprettSak(connection, 1 januar 2020)

            val førstegangsbehandling = behandlingService.finnEllerOpprettBehandling(
                sak.id,
                VurderingsbehovOgÅrsak(
                    listOf(VurderingsbehovMedPeriode(Vurderingsbehov.MOTTATT_SØKNAD)),
                    ÅrsakTilOpprettelse.SØKNAD,
                )
            ).åpenBehandling!!

            val utvalg = List(2) {
                behandlingService.finnEllerOpprettBehandling(
                    sak.id,
                    VurderingsbehovOgÅrsak(
                        listOf(VurderingsbehovMedPeriode(Vurderingsbehov.SØKNAD_UTENLANDSOPPHOLD)),
                        ÅrsakTilOpprettelse.SØKNAD_UTENLANDSOPPHOLD,
                    )
                ).åpenBehandling!!
            }

            assertThat(utvalg.map { it.id }).doesNotHaveDuplicates().doesNotContain(førstegangsbehandling.id)
            assertThat(utvalg).allSatisfy {
                assertThat(it.typeBehandling()).isEqualTo(TypeBehandling.Utenlandsopphold)
                assertThat(it.forrigeBehandlingId).isNull()
            }
            assertThat(behandlingRepository.hentAlleFor(sak.id)).hasSize(3)
            assertThat(behandlingRepository.hent(førstegangsbehandling.id).vurderingsbehov().map { it.type })
                .doesNotContain(Vurderingsbehov.SØKNAD_UTENLANDSOPPHOLD)
        }
    }
}
