package no.nav.aap.behandlingsflyt.faktagrunnlag.register.arena

import no.nav.aap.behandlingsflyt.help.flytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.test.FakeArenaOppslagGateway
import no.nav.aap.behandlingsflyt.test.FakeUnleashBaseWithDefaultDisabled
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryArenaMaksdatoRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemorySakRepository
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ArenaMaksdatoInformasjonskravTest {

    private val førstegangsbehandling = flytKontekstMedPerioder {
        behandlingType = TypeBehandling.Førstegangsbehandling
    }

    @Test
    fun `er ikke relevant i forvaltningsmeldingssteget når toggle for ny forvaltningsmelding er av`() {
        val krav = krav(BehandlingsflytFeature.HandtereMaksdatoForKantTilKant)

        assertThat(krav.erRelevant(førstegangsbehandling, StegType.SEND_FORVALTNINGSMELDING, null)).isFalse()
    }

    @Test
    fun `er relevant i forvaltningsmeldingssteget når begge toggles er på`() {
        val krav = krav(
            BehandlingsflytFeature.HandtereMaksdatoForKantTilKant,
            BehandlingsflytFeature.NykantIkantForvaltningsmelding
        )

        assertThat(krav.erRelevant(førstegangsbehandling, StegType.SEND_FORVALTNINGSMELDING, null)).isTrue()
    }

    @Test
    fun `er relevant i stønadsperiodesteget uavhengig av toggle for ny forvaltningsmelding`() {
        val krav = krav(BehandlingsflytFeature.HandtereMaksdatoForKantTilKant)

        assertThat(krav.erRelevant(førstegangsbehandling, StegType.AVKLAR_STØNADSPERIODE, null)).isTrue()
    }

    @Test
    fun `er ikke relevant i noen steg når toggle for maksdato er av`() {
        val krav = krav(BehandlingsflytFeature.NykantIkantForvaltningsmelding)

        assertThat(krav.erRelevant(førstegangsbehandling, StegType.SEND_FORVALTNINGSMELDING, null)).isFalse()
        assertThat(krav.erRelevant(førstegangsbehandling, StegType.AVKLAR_STØNADSPERIODE, null)).isFalse()
    }

    @Test
    fun `er ikke relevant i revurdering`() {
        val krav = krav(
            BehandlingsflytFeature.HandtereMaksdatoForKantTilKant,
            BehandlingsflytFeature.NykantIkantForvaltningsmelding
        )
        val revurdering = flytKontekstMedPerioder { behandlingType = TypeBehandling.Revurdering }

        assertThat(krav.erRelevant(revurdering, StegType.SEND_FORVALTNINGSMELDING, null)).isFalse()
        assertThat(krav.erRelevant(revurdering, StegType.AVKLAR_STØNADSPERIODE, null)).isFalse()
    }

    private fun krav(vararg påskrudd: BehandlingsflytFeature) = ArenaMaksdatoInformasjonskrav(
        sakRepository = InMemorySakRepository,
        arenaMaksdatoRepository = InMemoryArenaMaksdatoRepository,
        arenaOppslagGateway = FakeArenaOppslagGateway(),
        unleashGateway = object : FakeUnleashBaseWithDefaultDisabled(påskrudd.toList()) {},
    )
}
