package no.nav.aap.behandlingsflyt

import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.StegTilstand
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.StegStatus
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class KanLøseBehovSomSkalVæreLåstEtterKvalitetssikringTest {

    private fun lagBehandling(aktivtSteg: StegType): Behandling {
        return Behandling(
            id = BehandlingId(1),
            forrigeBehandlingId = null,
            sakId = SakId(1),
            typeBehandling = TypeBehandling.Førstegangsbehandling,
            årsakTilOpprettelse = ÅrsakTilOpprettelse.SØKNAD,
            versjon = 1,
            stegTilstand = StegTilstand(
                stegType = aktivtSteg,
                stegStatus = StegStatus.AVKLARINGSPUNKT,
                aktiv = true
            )
        )
    }

    @Test
    fun `Skal kunne løse behov når aktivsteg er før kvalitetssikring`() {
        val behandling = lagBehandling(StegType.VURDER_BISTANDSBEHOV)
        
        // Sykdom (AVKLAR_SYKDOM) er før aktivt steg, så den SKAL kunne løses
        val kanLøseSykdom = kanLøseBehovSomSkalVæreLåstEtterKvalitetssikring(
            løsesISteg = StegType.AVKLAR_SYKDOM,
            behandling = behandling
        )
        assertThat(kanLøseSykdom).isTrue()

        // Sykdom (AVKLAR_SYKDOM) er før aktivt steg, så den SKAL kunne løses
        val kanLøseBistand = kanLøseBehovSomSkalVæreLåstEtterKvalitetssikring(
            løsesISteg = StegType.VURDER_BISTANDSBEHOV,
            behandling = behandling
        )
        assertThat(kanLøseBistand).isTrue()
    }

    @Test
    fun `Skal ikke kunne løse behov etter aktivt steg`() {
        val behandling = lagBehandling(StegType.AVKLAR_SYKDOM)

        // Bistand (VURDER_BISTANDSBEHOV) er etter aktivt steg (AVKLAR_SYKDOM), så den skal IKKE kunne løses
        val kanLøseBistand = kanLøseBehovSomSkalVæreLåstEtterKvalitetssikring(
            løsesISteg = StegType.VURDER_BISTANDSBEHOV,
            behandling = behandling
        )
        assertThat(kanLøseBistand).isFalse()
    }


    @Test
    fun `Skal ikke kunne løse behov dersom aktivt steg er kvalitetssikring eller senere`() {
        val behandling = lagBehandling(StegType.KVALITETSSIKRING)
        // Aktivt steg er ETTER kvalitetssikring (f.eks. VURDER_YRKESSKADE)
        val behandling2 = lagBehandling(StegType.VURDER_YRKESSKADE)

        // Selv om løsesISteg er før aktivt steg, er aktivt steg ETTER kvalitetssikring, så den skal returnere false
        val kanLøseSykdomIKvalitetssikring = kanLøseBehovSomSkalVæreLåstEtterKvalitetssikring(
            løsesISteg = StegType.AVKLAR_SYKDOM,
            behandling = behandling
        )
        assertThat(kanLøseSykdomIKvalitetssikring).isFalse()

        val kanLøseSykdomPassertKvalitetssikring= kanLøseBehovSomSkalVæreLåstEtterKvalitetssikring(
            løsesISteg = StegType.VURDER_BISTANDSBEHOV,
            behandling = behandling2
        )
        assertThat(kanLøseSykdomPassertKvalitetssikring).isFalse()
    }

}

