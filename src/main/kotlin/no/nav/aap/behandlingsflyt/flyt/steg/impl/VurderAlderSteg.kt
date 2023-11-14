package no.nav.aap.behandlingsflyt.flyt.steg.impl

import no.nav.aap.behandlingsflyt.faktagrunnlag.personopplysninger.PersonopplysningRepository
import no.nav.aap.behandlingsflyt.flyt.FlytKontekst
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.flyt.vilkår.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.flyt.vilkår.Vilkårtype
import no.nav.aap.behandlingsflyt.flyt.vilkår.alder.Aldersgrunnlag
import no.nav.aap.behandlingsflyt.flyt.vilkår.alder.Aldersvilkåret

class VurderAlderSteg(
    private val vilkårsresultatRepository: VilkårsresultatRepository,
    private val periodeTilVurderingService: PeriodeTilVurderingService,
    private val personopplysningRepository: PersonopplysningRepository
) : BehandlingSteg {

    override fun utfør(kontekst: FlytKontekst): StegResultat {
        val periodeTilVurdering =
            periodeTilVurderingService.utled(kontekst = kontekst, vilkår = Vilkårtype.ALDERSVILKÅRET)

        if (periodeTilVurdering.isNotEmpty()) {
            val personopplysningGrunnlag = personopplysningRepository.hentHvisEksisterer(kontekst.behandlingId)
                ?: throw IllegalStateException("Forventet å finne personopplysninger")

            val vilkårsresultat = vilkårsresultatRepository.hent(kontekst.behandlingId)
            for (periode in periodeTilVurdering) {
                val aldersgrunnlag = Aldersgrunnlag(periode, personopplysningGrunnlag.personopplysning.fødselsdato)
                Aldersvilkåret(vilkårsresultat).vurder(aldersgrunnlag)
            }
            vilkårsresultatRepository.lagre(kontekst.behandlingId, vilkårsresultat)
        }

        return StegResultat()
    }
}
