package no.nav.aap.behandlingsflyt.flyt.steg.impl

import no.nav.aap.behandlingsflyt.domene.behandling.BehandlingTjeneste
import no.nav.aap.behandlingsflyt.domene.behandling.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.StegInput
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.flyt.steg.StegType
import no.nav.aap.behandlingsflyt.flyt.vilkår.Vilkårtype
import no.nav.aap.behandlingsflyt.grunnlag.sykdom.SykdomsGrunnlag
import no.nav.aap.behandlingsflyt.grunnlag.sykdom.SykdomsTjeneste
import no.nav.aap.behandlingsflyt.grunnlag.yrkesskade.YrkesskadeGrunnlag
import no.nav.aap.behandlingsflyt.grunnlag.yrkesskade.YrkesskadeTjeneste

class VurderYrkesskadeÅrsakssammenhengSteg : BehandlingSteg {
    override fun utfør(input: StegInput): StegResultat {
        val behandling = BehandlingTjeneste.hent(input.kontekst.behandlingId)

        val periodeTilVurdering =
            PeriodeTilVurderingTjeneste.utled(behandling = behandling, vilkår = Vilkårtype.SYKDOMSVILKÅRET)

        if (periodeTilVurdering.isNotEmpty()) {
            val yrkesskadeGrunnlag = YrkesskadeTjeneste.hentHvisEksisterer(behandlingId = behandling.id)
            val sykdomsGrunnlag = SykdomsTjeneste.hentHvisEksisterer(behandlingId = behandling.id)

            if (erBehovForAvklaring(yrkesskadeGrunnlag, sykdomsGrunnlag)) {
                return StegResultat(listOf(Definisjon.AVKLAR_YRKESSKADE))
            }
        }
        return StegResultat()
    }

    private fun erBehovForAvklaring(
        yrkesskadeGrunnlag: YrkesskadeGrunnlag?,
        sykdomsGrunnlag: SykdomsGrunnlag?
    ): Boolean {
        return yrkesskadeGrunnlag?.yrkesskader?.yrkesskader?.isNotEmpty() == true
                && sykdomsGrunnlag?.yrkesskadevurdering == null
    }

    override fun type(): StegType {
        return StegType.AVKLAR_YRKESSKADE
    }
}
