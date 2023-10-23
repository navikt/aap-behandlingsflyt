package no.nav.aap.behandlingsflyt.flyt.steg.impl

import no.nav.aap.behandlingsflyt.domene.behandling.BehandlingTjeneste
import no.nav.aap.behandlingsflyt.domene.behandling.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.StegInput
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.flyt.steg.StegType
import no.nav.aap.behandlingsflyt.flyt.vilkår.Innvilgelsesårsak
import no.nav.aap.behandlingsflyt.flyt.vilkår.Vilkår
import no.nav.aap.behandlingsflyt.flyt.vilkår.Vilkårtype
import no.nav.aap.behandlingsflyt.flyt.vilkår.bistand.BistandFaktagrunnlag
import no.nav.aap.behandlingsflyt.flyt.vilkår.bistand.Bistandsvilkåret
import no.nav.aap.behandlingsflyt.grunnlag.bistand.BistandsTjeneste
import no.nav.aap.behandlingsflyt.grunnlag.student.StudentGrunnlag
import no.nav.aap.behandlingsflyt.grunnlag.student.StudentRepository

class VurderBistandsbehovSteg(
    private val behandlingTjeneste: BehandlingTjeneste,
    private val studentRepository: StudentRepository
) : BehandlingSteg {

    override fun utfør(input: StegInput): StegResultat {
        val behandling = behandlingTjeneste.hent(input.kontekst.behandlingId)

        val periodeTilVurdering =
            PeriodeTilVurderingTjeneste.utled(behandling = behandling, vilkår = Vilkårtype.BISTANDSVILKÅRET)

        if (periodeTilVurdering.isNotEmpty()) {

            val bistandsGrunnlag = BistandsTjeneste.hentHvisEksisterer(behandling.id)
            val studentGrunnlag = studentRepository.hentHvisEksisterer(behandling.id)

            if (studentGrunnlag?.studentvurdering?.oppfyller11_14 == true || bistandsGrunnlag != null) {
                for (periode in periodeTilVurdering) {
                    val grunnlag = BistandFaktagrunnlag(
                        periode.fom,
                        periode.tom,
                        bistandsGrunnlag?.vurdering,
                        studentGrunnlag?.studentvurdering
                    )
                    Bistandsvilkåret(behandling.vilkårsresultat()).vurder(grunnlag = grunnlag)
                }
            }

            val vilkår = behandling.vilkårsresultat().finnVilkår(Vilkårtype.BISTANDSVILKÅRET)

            if (vilkår.harPerioderSomIkkeErVurdert(periodeTilVurdering) || harInnvilgetForStudentUtenÅVæreStudent(
                    vilkår,
                    studentGrunnlag
                )
            ) {
                return StegResultat(listOf(Definisjon.AVKLAR_BISTANDSBEHOV))
            }
        }

        return StegResultat()
    }

    private fun harInnvilgetForStudentUtenÅVæreStudent(vilkår: Vilkår, studentGrunnlag: StudentGrunnlag?): Boolean {
        return studentGrunnlag?.studentvurdering?.oppfyller11_14 == false && vilkår.vilkårsperioder()
            .any { it.innvilgelsesårsak == Innvilgelsesårsak.STUDENT }
    }

    override fun type(): StegType {
        return StegType.VURDER_BISTANDSBEHOV
    }
}
