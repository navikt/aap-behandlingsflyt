package no.nav.aap.behandlingsflyt.flyt.steg.impl

import no.nav.aap.behandlingsflyt.Periode
import no.nav.aap.behandlingsflyt.behandling.BehandlingService
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.faktagrunnlag.bistand.BistandsRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.student.StudentGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.student.StudentRepository
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.StegInput
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.flyt.vilkår.Innvilgelsesårsak
import no.nav.aap.behandlingsflyt.flyt.vilkår.Vilkår
import no.nav.aap.behandlingsflyt.flyt.vilkår.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.flyt.vilkår.Vilkårtype
import no.nav.aap.behandlingsflyt.flyt.vilkår.bistand.BistandFaktagrunnlag
import no.nav.aap.behandlingsflyt.flyt.vilkår.bistand.Bistandsvilkåret

class VurderBistandsbehovSteg(
    private val behandlingService: BehandlingService,
    private val studentRepository: StudentRepository,
    private val periodeTilVurderingService: PeriodeTilVurderingService
) : BehandlingSteg {

    override fun utfør(input: StegInput): StegResultat {
        val behandling = behandlingService.hent(input.kontekst.behandlingId)

        val periodeTilVurdering =
            periodeTilVurderingService.utled(behandling = behandling, vilkår = Vilkårtype.BISTANDSVILKÅRET)

        if (periodeTilVurdering.isNotEmpty()) {

            val bistandsGrunnlag = BistandsRepository.hentHvisEksisterer(behandling.id)
            val studentGrunnlag = studentRepository.hentHvisEksisterer(behandling.id)

            if (studentGrunnlag?.studentvurdering?.oppfyller11_14 == true || bistandsGrunnlag != null) {
                for (periode in periodeTilVurdering) {
                    val grunnlag = BistandFaktagrunnlag(
                        periode.fom,
                        periode.tom,
                        bistandsGrunnlag?.vurdering,
                        studentGrunnlag?.studentvurdering
                    )
                    Bistandsvilkåret(VilkårsresultatRepository.hent(behandling.id)).vurder(grunnlag = grunnlag)
                }
            }

            val vilkår = VilkårsresultatRepository.hent(behandling.id).finnVilkår(Vilkårtype.BISTANDSVILKÅRET)

            if (harBehovForAvklaring(vilkår, periodeTilVurdering, studentGrunnlag)) {
                return StegResultat(listOf(Definisjon.AVKLAR_BISTANDSBEHOV))
            }
        }

        return StegResultat()
    }

    private fun harBehovForAvklaring(
        vilkår: Vilkår,
        periodeTilVurdering: Set<Periode>,
        studentGrunnlag: StudentGrunnlag?
    ): Boolean {
        return (vilkår.harPerioderSomIkkeErVurdert(periodeTilVurdering)
                || harInnvilgetForStudentUtenÅVæreStudent(vilkår, studentGrunnlag))
    }

    private fun harInnvilgetForStudentUtenÅVæreStudent(vilkår: Vilkår, studentGrunnlag: StudentGrunnlag?): Boolean {
        return studentGrunnlag?.studentvurdering?.oppfyller11_14 == false && vilkår.vilkårsperioder()
            .any { it.innvilgelsesårsak == Innvilgelsesårsak.STUDENT }
    }
}
