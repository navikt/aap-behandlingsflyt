package no.nav.aap.behandlingsflyt

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.PeriodisertFritakMeldepliktLøsning
import no.nav.aap.behandlingsflyt.flyt.AbstraktFlytOrkestratorTest
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedClass
import org.junit.jupiter.params.provider.MethodSource
import java.time.LocalDate
import kotlin.reflect.KClass

@ParameterizedClass
@MethodSource("unleashTestDataSource")
class FritakMeldepliktFlytTest(val unleashGateway: KClass<UnleashGateway>) :
    AbstraktFlytOrkestratorTest(unleashGateway) {

    @Test
    fun `kan løse frivillig behov i revurdering`() {
        val sak = happyCaseFørstegangsbehandling(LocalDate.now(), sendMeldekort = false)

        sak.opprettManuellRevurdering(
            no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.VURDER_FRITAK_MELDEPLIKT
        )
            .medKontekst {
                assertThat(behandling.typeBehandling()).isEqualTo(TypeBehandling.Revurdering)
                assertThat(behandling.aktivtSteg()).isEqualTo(StegType.FRITAK_MELDEPLIKT)
                assertThat(behandling.status()).isEqualTo(Status.UTREDES)
            }
            .løsAvklaringsBehov(
                PeriodisertFritakMeldepliktLøsning(
                    løsningerForPerioder = listOf() /// tom liste
                )
            )
            .medKontekst {
                assertThat(behandling.aktivtSteg()).isNotEqualTo(StegType.FRITAK_MELDEPLIKT)
                assertThat(behandling.status()).isEqualTo(Status.UTREDES)
            }
    }
}