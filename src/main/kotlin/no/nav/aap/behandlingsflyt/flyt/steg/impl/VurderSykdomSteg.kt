package no.nav.aap.behandlingsflyt.flyt.steg.impl

import no.nav.aap.behandlingsflyt.domene.behandling.BehandlingTjeneste
import no.nav.aap.behandlingsflyt.domene.behandling.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.faktagrunnlag.student.StudentRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.sykdom.SykdomsRepository
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.StegInput
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.flyt.vilkår.Vilkårtype
import no.nav.aap.behandlingsflyt.flyt.vilkår.sykdom.SykdomsFaktagrunnlag
import no.nav.aap.behandlingsflyt.flyt.vilkår.sykdom.Sykdomsvilkår

class VurderSykdomSteg(
    private val behandlingTjeneste: BehandlingTjeneste,
    private val studentRepository: StudentRepository
) : BehandlingSteg {

    override fun utfør(input: StegInput): StegResultat {
        val behandling = behandlingTjeneste.hent(input.kontekst.behandlingId)

        val periodeTilVurdering =
            PeriodeTilVurderingTjeneste.utled(behandling = behandling, vilkår = Vilkårtype.SYKDOMSVILKÅRET)

        if (periodeTilVurdering.isNotEmpty()) {
            val sykdomsGrunnlag = SykdomsRepository.hentHvisEksisterer(behandlingId = behandling.id)
            val studentGrunnlag = studentRepository.hentHvisEksisterer(behandlingId = behandling.id)

            //TODO: Skrive om til å være lik uttrykket på linje 46
            if (sykdomsGrunnlag != null && sykdomsGrunnlag.erKonsistent() || studentGrunnlag?.studentvurdering?.oppfyller11_14 == true) {
                for (periode in periodeTilVurdering) {
                    val faktagrunnlag = SykdomsFaktagrunnlag(
                        periode.fom,
                        periode.tom,
                        sykdomsGrunnlag?.yrkesskadevurdering,
                        sykdomsGrunnlag?.sykdomsvurdering,
                        studentGrunnlag?.studentvurdering
                    )
                    Sykdomsvilkår(behandling.vilkårsresultat()).vurder(faktagrunnlag)
                }
            }
            val sykdomsvilkåret = behandling.vilkårsresultat().finnVilkår(Vilkårtype.SYKDOMSVILKÅRET)

            if (sykdomsvilkåret.harPerioderSomIkkeErVurdert(periodeTilVurdering)
                || (studentGrunnlag?.studentvurdering?.oppfyller11_14 == false && sykdomsGrunnlag?.erKonsistent() != true)) {
                return StegResultat(listOf(Definisjon.AVKLAR_SYKDOM))
            }
        }
        return StegResultat()
    }
}
