package no.nav.aap.behandlingsflyt.flyt.steg.impl

import no.nav.aap.behandlingsflyt.behandling.BehandlingService
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.faktagrunnlag.student.StudentRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.sykdom.SykdomsRepository
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.StegInput
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.flyt.vilkår.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.flyt.vilkår.Vilkårtype
import no.nav.aap.behandlingsflyt.flyt.vilkår.sykdom.SykdomsFaktagrunnlag
import no.nav.aap.behandlingsflyt.flyt.vilkår.sykdom.Sykdomsvilkår

class VurderSykdomSteg(
    private val behandlingService: BehandlingService,
    private val sykdomsRepository: SykdomsRepository,
    private val studentRepository: StudentRepository,
    private val vilkårsresultatRepository: VilkårsresultatRepository,
    private val periodeTilVurderingService: PeriodeTilVurderingService
) : BehandlingSteg {

    override fun utfør(input: StegInput): StegResultat {
        val behandling = behandlingService.hent(input.kontekst.behandlingId)

        val periodeTilVurdering =
            periodeTilVurderingService.utled(behandling = behandling, vilkår = Vilkårtype.SYKDOMSVILKÅRET)

        if (periodeTilVurdering.isNotEmpty()) {
            val sykdomsGrunnlag = sykdomsRepository.hentHvisEksisterer(behandlingId = behandling.id)
            val studentGrunnlag = studentRepository.hentHvisEksisterer(behandlingId = behandling.id)

            //TODO: Skrive om til å være lik uttrykket på linje 46
            val vilkårResultat = vilkårsresultatRepository.hent(behandling.id)
            if (sykdomsGrunnlag != null && sykdomsGrunnlag.erKonsistent() || studentGrunnlag?.studentvurdering?.oppfyller11_14 == true) {
                for (periode in periodeTilVurdering) {
                    val faktagrunnlag = SykdomsFaktagrunnlag(
                        periode.fom,
                        periode.tom,
                        sykdomsGrunnlag?.yrkesskadevurdering,
                        sykdomsGrunnlag?.sykdomsvurdering,
                        studentGrunnlag?.studentvurdering
                    )
                    Sykdomsvilkår(vilkårResultat).vurder(faktagrunnlag)
                }
            }
            vilkårsresultatRepository.lagre(behandling.id, vilkårResultat)
            val sykdomsvilkåret = vilkårResultat.finnVilkår(Vilkårtype.SYKDOMSVILKÅRET)

            if (sykdomsvilkåret.harPerioderSomIkkeErVurdert(periodeTilVurdering)
                || (studentGrunnlag?.studentvurdering?.oppfyller11_14 == false && sykdomsGrunnlag?.erKonsistent() != true)
            ) {
                return StegResultat(listOf(Definisjon.AVKLAR_SYKDOM))
            }
        }
        return StegResultat()
    }
}
