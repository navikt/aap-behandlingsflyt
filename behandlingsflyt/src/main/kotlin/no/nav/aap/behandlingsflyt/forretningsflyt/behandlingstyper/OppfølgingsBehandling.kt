package no.nav.aap.behandlingsflyt.forretningsflyt.behandlingstyper

import no.nav.aap.behandlingsflyt.flyt.BehandlingFlyt
import no.nav.aap.behandlingsflyt.flyt.BehandlingFlytBuilder
import no.nav.aap.behandlingsflyt.flyt.BehandlingType
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.oppfølgingsbehandling.AvklarOppfølgingSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.oppfølgingsbehandling.StartOppfølgingsBehandlingSteg
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov

object OppfølgingsBehandling : BehandlingType {
    override fun flyt(): BehandlingFlyt {
        return BehandlingFlytBuilder()
            .medSteg(StartOppfølgingsBehandlingSteg, vurderingsbehovRelevanteForSteg = Vurderingsbehov.alle())
            .medSteg(AvklarOppfølgingSteg, vurderingsbehovRelevanteForSteg = Vurderingsbehov.alle())
            .build()
    }
}