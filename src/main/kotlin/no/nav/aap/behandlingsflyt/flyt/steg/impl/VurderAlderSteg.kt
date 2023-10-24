package no.nav.aap.behandlingsflyt.flyt.steg.impl

import no.nav.aap.behandlingsflyt.domene.behandling.BehandlingTjeneste
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.StegInput
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.flyt.steg.StegType
import no.nav.aap.behandlingsflyt.flyt.vilkår.Vilkårtype
import no.nav.aap.behandlingsflyt.flyt.vilkår.alder.Aldersgrunnlag
import no.nav.aap.behandlingsflyt.flyt.vilkår.alder.Aldersvilkåret
import no.nav.aap.behandlingsflyt.faktagrunnlag.personopplysninger.PersoninformasjonRepository

class VurderAlderSteg(private val behandlingTjeneste: BehandlingTjeneste) : BehandlingSteg {

    override fun utfør(input: StegInput): StegResultat {

        val behandling = behandlingTjeneste.hent(input.kontekst.behandlingId)

        val periodeTilVurdering =
            PeriodeTilVurderingTjeneste.utled(behandling = behandling, vilkår = Vilkårtype.ALDERSVILKÅRET)

        if (periodeTilVurdering.isNotEmpty()) {
            val personinfoGrunnlag = PersoninformasjonRepository.hentHvisEksisterer(input.kontekst.behandlingId)
                ?: throw IllegalStateException("Forventet å finne personopplysninger")

            for (periode in periodeTilVurdering) {
                val aldersgrunnlag = Aldersgrunnlag(periode, personinfoGrunnlag.personinfo.fødselsdato)
                Aldersvilkåret(behandling.vilkårsresultat()).vurder(aldersgrunnlag)
            }
        }

        return StegResultat()
    }

    override fun type() = StegType.VURDER_ALDER
}
