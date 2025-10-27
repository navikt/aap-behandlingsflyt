package no.nav.aap.behandlingsflyt.faktagrunnlag

import no.nav.aap.behandlingsflyt.help.opprettSak
import no.nav.aap.behandlingsflyt.integrasjon.createGatewayProvider
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.postgresRepositoryRegistry
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovOgÅrsak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.test.FakeUnleash
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AutoClose
import kotlin.test.Test

class SakOgBehandlingServiceTest {

    @AutoClose
    private val dataSource = TestDataSource()

    private val gatewayProvider = createGatewayProvider { register<FakeUnleash>() }

    @Test
    fun `gjenbruker åpen behandling hvis vi prøver å opprette enda en ny behandling etter et meldekort`() {
        dataSource.transaction { connection ->
            val sakOgBehandlingService =
                SakOgBehandlingService(postgresRepositoryRegistry.provider(connection), gatewayProvider)
            val behandlingRepository = BehandlingRepositoryImpl(connection)
            val sak = opprettSak(connection, Periode(1 januar 2020, 1 januar 2021))

            /* Førstegangsbehandling */
            sakOgBehandlingService.finnEllerOpprettBehandling(
                sak.id,
                VurderingsbehovOgÅrsak(
                    listOf(VurderingsbehovMedPeriode(Vurderingsbehov.MOTTATT_SØKNAD)),
                    ÅrsakTilOpprettelse.SØKNAD,
                )
            ).åpenBehandling!!.also {
                behandlingRepository.oppdaterBehandlingStatus(it.id, Status.AVSLUTTET)
            }

            /* Åpen revurdering */
            sakOgBehandlingService.finnEllerOpprettBehandling(
                sak.id,
                VurderingsbehovOgÅrsak(
                    listOf(VurderingsbehovMedPeriode(Vurderingsbehov.REVURDER_SAMORDNING)),
                    ÅrsakTilOpprettelse.MANUELL_OPPRETTELSE,
                )
            )

            /* Send inn meldekort */
            val meldekortbehandling = (sakOgBehandlingService.finnEllerOpprettBehandling(
                sak.id,
                VurderingsbehovOgÅrsak(
                    listOf(VurderingsbehovMedPeriode(Vurderingsbehov.MOTTATT_MELDEKORT)),
                    ÅrsakTilOpprettelse.MELDEKORT,
                )
            ) as SakOgBehandlingService.MåBehandlesAtomært).nyBehandling
            behandlingRepository.oppdaterBehandlingStatus(meldekortbehandling.id, Status.AVSLUTTET)

            /* Forsøk å opprett en til revurdering (f.eks. nye opplysninger fra register). */
            sakOgBehandlingService.finnEllerOpprettBehandling(
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
}