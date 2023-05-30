package no.nav.aap.flyt.steg

import no.nav.aap.domene.behandling.avklaringsbehov.Definisjon
import no.nav.aap.flyt.StegType

class VurderYrkesskadeSteg : BehandlingSteg {
    override fun utfør(input: StegInput): StegResultat {
        return StegResultat(listOf(Definisjon.AVKLAR_YRKESSKADE))
    }

    override fun type(): StegType {
        return StegType.AVKLAR_YRKESSKADE
    }
}
