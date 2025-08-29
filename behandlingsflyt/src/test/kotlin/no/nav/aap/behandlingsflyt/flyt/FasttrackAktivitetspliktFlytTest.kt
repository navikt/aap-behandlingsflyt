package no.nav.aap.behandlingsflyt.flyt

import no.nav.aap.behandlingsflyt.faktagrunnlag.SakOgBehandlingService
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.prosessering.ProsesserBehandlingService
import no.nav.aap.behandlingsflyt.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.postgresRepositoryRegistry
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovOgÅrsak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.test.FakeUnleashFasttrackMeldekort
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.dbconnect.transaction
import org.assertj.core.api.Assertions.assertThat
import kotlin.reflect.KClass
import kotlin.test.Test

class FasttrackAktivitetspliktFlytTest :
    AbstraktFlytOrkestratorTest(FakeUnleashFasttrackMeldekort::class as KClass<UnleashGateway>) {

    @Test
    fun `Mottatt aktivitetsmelding skal behandles atomært`() {
        val sak = happyCaseFørstegangsbehandling()
        var åpenBehandling = revurdereFramTilOgMedSykdom(sak, sak.rettighetsperiode.fom)

        assertThat(åpenBehandling.vurderingsbehov().map { it.type })
            .hasSameElementsAs(listOf(Vurderingsbehov.MOTTATT_SØKNAD))
        val aktivtStegFørMeldekort = åpenBehandling.aktivtSteg()

        // TODO: Denne skal opprettes fra aktivitetsplikt-behandling 
        var effektuer11_7behandling = opprettRevurderingMedAktivitetsbrudd(sak.id)
        motor.kjørJobber()

        assertThat(effektuer11_7behandling is SakOgBehandlingService.MåBehandlesAtomært)
            .describedAs { "Effektuering av 11-7 må behandles atomært" }
            .isTrue()
        effektuer11_7behandling = effektuer11_7behandling as SakOgBehandlingService.MåBehandlesAtomært

        dataSource.transaction { connection ->
            val prosesesert11_7behandling =
                BehandlingRepositoryImpl(connection).hent(effektuer11_7behandling.nyBehandling.id)
            assertThat(prosesesert11_7behandling.status()).isEqualTo(Status.AVSLUTTET)
                .describedAs { "Effektuering av 11-7 har blitt behandlet ferdig" }

            // Åpen behandling skal forbli i samme steg
            assertThat(effektuer11_7behandling.åpenBehandling?.id).isEqualTo(åpenBehandling.id)
                .describedAs { "Fasttrack-behandling har korrekt referanse til åpen behandling" }
            
            åpenBehandling = BehandlingRepositoryImpl(connection).hent(åpenBehandling.id)
            assertThat(åpenBehandling.aktivtSteg())
                .describedAs("Prosessering av aktivitetsmelding skal ikke endre steg for åpen behandling, dersom den åpne behandlingen står i steg før informasjonskravet for aktivitetsmelding")
                .isEqualTo(aktivtStegFørMeldekort)

            // Den åpne behandlingen skal ha fått aktivitetsbruddene
            // TODO: Prosesser aktivitetsbrudd og flett inn i åpen behandling
        }

    }

    private fun opprettRevurderingMedAktivitetsbrudd(sakId: SakId) =
        dataSource.transaction { connection ->
            val effektuerAktivitetspliktBehandling = SakOgBehandlingService(
                postgresRepositoryRegistry.provider(connection),
                gatewayProvider,
            ).finnEllerOpprettBehandling(
                sakId,
                VurderingsbehovOgÅrsak(
                    vurderingsbehov = listOf(
                        VurderingsbehovMedPeriode(
                            Vurderingsbehov.MOTTATT_AKTIVITETSMELDING,
                            null
                        )
                    ),
                    årsak = ÅrsakTilOpprettelse.AKTIVITETSMELDING
                )
            )
            ProsesserBehandlingService(
                postgresRepositoryRegistry.provider(connection),
                gatewayProvider
            ).triggProsesserBehandling(
                effektuerAktivitetspliktBehandling
            )

            effektuerAktivitetspliktBehandling

        }
}