package no.nav.aap.behandlingsflyt.flyt.steg.impl

import no.nav.aap.behandlingsflyt.behandling.BehandlingService
import no.nav.aap.behandlingsflyt.faktagrunnlag.personopplysninger.PersoninformasjonRepository
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.StegInput
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.flyt.vilkår.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.flyt.vilkår.Vilkårtype
import no.nav.aap.behandlingsflyt.flyt.vilkår.alder.Aldersgrunnlag
import no.nav.aap.behandlingsflyt.flyt.vilkår.alder.Aldersvilkåret

class VurderAlderSteg(
    private val behandlingService: BehandlingService,
    private val periodeTilVurderingService: PeriodeTilVurderingService
) : BehandlingSteg {

    override fun utfør(input: StegInput): StegResultat {

        val behandling = behandlingService.hent(input.kontekst.behandlingId)

        //TODO: Trekk ut PeriodeTilVurderinTjeneste som en dependency
        val periodeTilVurdering =
            periodeTilVurderingService.utled(behandling = behandling, vilkår = Vilkårtype.ALDERSVILKÅRET)

        if (periodeTilVurdering.isNotEmpty()) {
            val personinfoGrunnlag = PersoninformasjonRepository.hentHvisEksisterer(input.kontekst.behandlingId)
                ?: throw IllegalStateException("Forventet å finne personopplysninger")

            for (periode in periodeTilVurdering) {
                val aldersgrunnlag = Aldersgrunnlag(periode, personinfoGrunnlag.personinfo.fødselsdato)
                Aldersvilkåret(VilkårsresultatRepository.hent(behandling.id)).vurder(aldersgrunnlag)
            }
        }

        return StegResultat()
    }
}
