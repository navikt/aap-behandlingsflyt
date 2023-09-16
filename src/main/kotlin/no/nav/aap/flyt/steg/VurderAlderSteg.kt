package no.nav.aap.flyt.steg

import no.nav.aap.domene.behandling.BehandlingTjeneste
import no.nav.aap.domene.behandling.Vilkårsperiode
import no.nav.aap.domene.behandling.Vilkårstype
import no.nav.aap.domene.behandling.grunnlag.person.PersoninformasjonTjeneste
import no.nav.aap.domene.vilkår.alder.Aldersgrunnlag
import no.nav.aap.domene.vilkår.alder.Aldersvilkåret
import no.nav.aap.flate.behandling.periode.PeriodeTilVurderingTjeneste
import no.nav.aap.flyt.StegType

class VurderAlderSteg : BehandlingSteg {
    override fun utfør(input: StegInput): StegResultat {
        val behandling = BehandlingTjeneste.hent(input.kontekst.behandlingId)

        val periodeTilVurdering =
            PeriodeTilVurderingTjeneste.utled(behandling = behandling, vilkår = Vilkårstype.ALDERSVILKÅRET)

        if (periodeTilVurdering.isNotEmpty()) {
            val personinfoGrunnlag = PersoninformasjonTjeneste.hentHvisEksisterer(input.kontekst.behandlingId)
                ?: throw IllegalStateException("Forventet å finne personopplysninger")

            for (periode in periodeTilVurdering) {
                val aldersgrunnlag = Aldersgrunnlag(
                    periode.fraOgMed(),
                    personinfoGrunnlag.personinfo.fødselsdato
                )
                val vurdering = Aldersvilkåret.vurder(
                    aldersgrunnlag
                )
                val aldersvilkåret = behandling.vilkårsresultat().finnVilkår(Vilkårstype.ALDERSVILKÅRET)
                aldersvilkåret.leggTilVurdering(
                    Vilkårsperiode(
                        periode = periode,
                        utfall = vurdering.utfall,
                        begrunnelse = null,
                        faktagrunnlag = aldersgrunnlag,
                        beslutningstre = vurdering.beslutningstre
                    )
                )
            }
        }

        return StegResultat()
    }

    override fun type() = StegType.VURDER_ALDER
}
