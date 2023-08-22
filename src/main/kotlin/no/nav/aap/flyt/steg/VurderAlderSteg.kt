package no.nav.aap.flyt.steg

import no.nav.aap.domene.behandling.BehandlingTjeneste
import no.nav.aap.domene.behandling.Vilkårsperiode
import no.nav.aap.domene.behandling.Vilkårstype
import no.nav.aap.domene.behandling.grunnlag.person.PersoninformasjonTjeneste
import no.nav.aap.domene.sak.Sakslager
import no.nav.aap.domene.vilkår.alder.Aldersgrunnlag
import no.nav.aap.domene.vilkår.alder.Aldersvilkåret
import no.nav.aap.flyt.StegType

class VurderAlderSteg : BehandlingSteg {
    override fun utfør(input: StegInput): StegResultat {
        val sak = Sakslager.hent(input.kontekst.sakId)
        val behandling = BehandlingTjeneste.hent(input.kontekst.behandlingId)

        val personinfoGrunnlag = PersoninformasjonTjeneste.hentHvisEksisterer(input.kontekst.behandlingId)
            ?: throw IllegalStateException("Forventet å finne personopplysninger")

        val aldersgrunnlag = Aldersgrunnlag(
            sak.rettighetsperiode.fraOgMed(),
            personinfoGrunnlag.personinfo.fødselsdato
        )
        val vurdering = Aldersvilkåret.vurder(
            aldersgrunnlag
        )
        val aldersvilkåret = behandling.vilkårsresultat().finnVilkår(Vilkårstype.ALDERSVILKÅRET)
        aldersvilkåret.leggTilVurdering(
            Vilkårsperiode(
                sak.rettighetsperiode,
                vurdering.utfall,
                aldersgrunnlag,
                vurdering.beslutningstre
            )
        )

        return StegResultat()
    }

    override fun type() = StegType.VURDER_ALDER
}
