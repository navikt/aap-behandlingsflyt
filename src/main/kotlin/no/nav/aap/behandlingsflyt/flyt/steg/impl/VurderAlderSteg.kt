package no.nav.aap.behandlingsflyt.flyt.steg.impl

import no.nav.aap.behandlingsflyt.behandling.BehandlingService
import no.nav.aap.behandlingsflyt.faktagrunnlag.personopplysninger.PersoninformasjonRepository
import no.nav.aap.behandlingsflyt.flyt.FlytKontekst
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.flyt.vilkår.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.flyt.vilkår.Vilkårtype
import no.nav.aap.behandlingsflyt.flyt.vilkår.alder.Aldersgrunnlag
import no.nav.aap.behandlingsflyt.flyt.vilkår.alder.Aldersvilkåret

class VurderAlderSteg(
    private val behandlingService: BehandlingService,
    private val vilkårsresultatRepository: VilkårsresultatRepository,
    private val periodeTilVurderingService: PeriodeTilVurderingService,
    private val personinformasjonRepository: PersoninformasjonRepository
) : BehandlingSteg {

    override fun utfør(kontekst: FlytKontekst): StegResultat {
        val behandling = behandlingService.hent(kontekst.behandlingId)

        val periodeTilVurdering =
            periodeTilVurderingService.utled(behandling = behandling, vilkår = Vilkårtype.ALDERSVILKÅRET)

        if (periodeTilVurdering.isNotEmpty()) {
            val personinfoGrunnlag = personinformasjonRepository.hentHvisEksisterer(kontekst.behandlingId)
                ?: throw IllegalStateException("Forventet å finne personopplysninger")

            val vilkårsresultat = vilkårsresultatRepository.hent(kontekst.behandlingId)
            for (periode in periodeTilVurdering) {
                val aldersgrunnlag = Aldersgrunnlag(periode, personinfoGrunnlag.personinfo.fødselsdato)
                Aldersvilkåret(vilkårsresultat).vurder(aldersgrunnlag)
            }
            vilkårsresultatRepository.lagre(kontekst.behandlingId, vilkårsresultat)
        }

        return StegResultat()
    }
}
