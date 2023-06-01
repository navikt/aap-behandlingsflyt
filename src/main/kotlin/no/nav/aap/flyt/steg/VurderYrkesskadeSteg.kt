package no.nav.aap.flyt.steg

import no.nav.aap.domene.behandling.avklaringsbehov.Definisjon
import no.nav.aap.domene.behandling.grunnlag.yrkesskade.YrkesskadeTjeneste
import no.nav.aap.flyt.StegType

class VurderYrkesskadeSteg : BehandlingSteg {
    override fun utfør(input: StegInput): StegResultat {
        val grunnlagOptional = YrkesskadeTjeneste.hentHvisEksisterer(input.kontekst.behandlingId)

        if (grunnlagOptional.map { it.yrkesskader }.map { it.harYrkesskade() }.orElse(false)) {
            return StegResultat(listOf(Definisjon.AVKLAR_YRKESSKADE))
        }
        return StegResultat()
    }

    override fun type(): StegType {
        return StegType.AVKLAR_YRKESSKADE
    }
}
