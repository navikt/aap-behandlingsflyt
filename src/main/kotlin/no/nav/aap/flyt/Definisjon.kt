package no.nav.aap.flyt

import no.nav.aap.domene.behandling.BehandlingType
import no.nav.aap.domene.behandling.EndringType

object Definisjon {

    var førstegangsbehandling: BehandlingFlyt = BehandlingFlytBuilder(BehandlingType.FØRSTEGANGSBEHANDLING)
        .medSteg(StegType.START_BEHANDLING, EndringType.NY_SØKNAD)
        .medSteg(StegType.INNGANGSVILKÅR)
        .medSteg(StegType.FASTSETT_GRUNNLAG)
        .medSteg(StegType.FASTSETT_UTTAK)
        .medSteg(StegType.SIMULERING)
        .medSteg(StegType.BEREGN_TILKJENT_YTELSE)
        .medSteg(StegType.FORESLÅ_VEDTAK) // en-trinn
        .medSteg(StegType.FATTE_VEDTAK) // to-trinn
        .medSteg(StegType.IVERKSETT_VEDTAK)
        .build()

    var revurdering: BehandlingFlyt = BehandlingFlytBuilder(BehandlingType.REVURDERING)
        .medSteg(StegType.START_BEHANDLING)
        .medSteg(StegType.FORESLÅ_VEDTAK)
        .medSteg(StegType.FATTE_VEDTAK)
        .medSteg(StegType.IVERKSETT_VEDTAK)
        .build()
}
