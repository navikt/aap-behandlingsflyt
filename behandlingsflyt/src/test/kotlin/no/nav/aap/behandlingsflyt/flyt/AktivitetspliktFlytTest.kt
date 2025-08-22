package no.nav.aap.behandlingsflyt.flyt

import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.test.FakeUnleashFasttrackMeldekort
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import kotlin.reflect.KClass

class AktivitetspliktFlytTest :
    AbstraktFlytOrkestratorTest(FakeUnleashFasttrackMeldekort::class as KClass<UnleashGateway>) {

    @Test
    fun `Happy-case flyt for aktivitetsplikt 11_7`() {
        val person = TestPersoner.STANDARD_PERSON()
        val sak = happyCaseFørstegangsbehandling(person = person)
        val førstegangsbehandling = hentNyesteBehandlingForSak(sak.id)

        // TODO: Mekanisme for opprettelse og automatisk prosessering
        var aktivitetspliktBehandling = opprettBehandling(
            sakId = sak.id,
            typeBehandling = TypeBehandling.Aktivitetsplikt,
            vurderingsbehov = listOf(
                VurderingsbehovMedPeriode(
                    Vurderingsbehov.AKTIVITETSPLIKT_11_7,
                    periode = sak.rettighetsperiode
                )
            ),
            forrigeBehandlingId = førstegangsbehandling.id
        )

        assertThat(aktivitetspliktBehandling.status()).isEqualTo(Status.OPPRETTET)
        
        prosesserBehandling(aktivitetspliktBehandling)
        
        aktivitetspliktBehandling = hentBehandling(aktivitetspliktBehandling.referanse)
        assertThat(aktivitetspliktBehandling)
            .extracting { it.aktivtSteg() }
            .isEqualTo(StegType.VURDER_AKTIVITETSPLIKT_11_7)
        
        assertThat(aktivitetspliktBehandling.status()).isEqualTo(Status.AVSLUTTET)
    }
}