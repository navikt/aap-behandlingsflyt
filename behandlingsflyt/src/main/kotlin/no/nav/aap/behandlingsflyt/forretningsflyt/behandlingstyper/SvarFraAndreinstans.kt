package no.nav.aap.behandlingsflyt.forretningsflyt.behandlingstyper

import no.nav.aap.behandlingsflyt.flyt.BehandlingFlyt
import no.nav.aap.behandlingsflyt.flyt.BehandlingFlytBuilder
import no.nav.aap.behandlingsflyt.flyt.BehandlingType
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.svarfraandreinstans.IverksettKonsekvensSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.svarfraandreinstans.SvarFraAndreinstansSteg
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov

object SvarFraAndreinstans : BehandlingType {
    override fun flyt(): BehandlingFlyt {
        return BehandlingFlytBuilder()
            .medSteg(steg = SvarFraAndreinstansSteg, vurderingsbehovRelevanteForSteg = Vurderingsbehov.alle())
            .medSteg(steg = IverksettKonsekvensSteg, vurderingsbehovRelevanteForSteg = Vurderingsbehov.alle())
            .build()
    }
}