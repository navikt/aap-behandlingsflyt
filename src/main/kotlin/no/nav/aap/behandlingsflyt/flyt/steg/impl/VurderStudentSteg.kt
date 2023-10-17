package no.nav.aap.behandlingsflyt.flyt.steg.impl

import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.StegInput
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.flyt.steg.StegType
import no.nav.aap.behandlingsflyt.flyt.vilkår.Vilkårtype
import no.nav.aap.behandlingsflyt.domene.behandling.BehandlingTjeneste
import no.nav.aap.behandlingsflyt.domene.behandling.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.grunnlag.student.StudentTjeneste
import no.nav.aap.behandlingsflyt.flyt.vilkår.student.StudentFaktagrunnlag
import no.nav.aap.behandlingsflyt.flyt.vilkår.student.Studentvilkår

class VurderStudentSteg : BehandlingSteg {

    override fun utfør(input: StegInput): StegResultat {
        val behandling = BehandlingTjeneste.hent(input.kontekst.behandlingId)

        val periodeTilVurdering = PeriodeTilVurderingTjeneste.utled(behandling = behandling, vilkår = Vilkårtype.STUDENTVILKÅRET)
        if (periodeTilVurdering.isNotEmpty()) {
            val studentGrunnlag = StudentTjeneste.hentHvisEksisterer(behandlingId = behandling.id)

            if (studentGrunnlag != null && studentGrunnlag.erKonsistent()) {
                for (periode in periodeTilVurdering) {
                    val faktagrunnlag = StudentFaktagrunnlag(
                        periode.fom,
                        periode.tom,
                        studentGrunnlag.studentvurdering!!
                    )
                    Studentvilkår(behandling.vilkårsresultat()).vurder(faktagrunnlag)
                }
            }
            val sykdomsvilkåret = behandling.vilkårsresultat().finnVilkår(Vilkårtype.SYKDOMSVILKÅRET)

            if (sykdomsvilkåret.harPerioderSomIkkeErVurdert(periodeTilVurdering) || studentGrunnlag?.erKonsistent() != true) {
                return StegResultat(listOf(Definisjon.AVKLAR_SYKDOM))
            }
        }
    }

    override fun type(): StegType {
        return StegType.AVKLAR_STUDENT
    }
}
