package no.nav.aap.behandlingsflyt.flyt.steg.impl

import no.nav.aap.behandlingsflyt.Periode
import no.nav.aap.behandlingsflyt.behandling.BehandlingService
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.faktagrunnlag.bistand.BistandsRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.student.StudentGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.student.StudentRepository
import no.nav.aap.behandlingsflyt.flyt.FlytKontekst
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
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
    private val vilkårsresultatRepository: VilkårsresultatRepository,
    private val periodeTilVurderingService: PeriodeTilVurderingService
) : BehandlingSteg {

    override fun utfør(kontekst: FlytKontekst): StegResultat {
        val behandling = behandlingService.hent(kontekst.behandlingId)

        val periodeTilVurdering =
            periodeTilVurderingService.utled(kontekst = kontekst, vilkår = Vilkårtype.BISTANDSVILKÅRET)

        if (periodeTilVurdering.isNotEmpty()) {

            val bistandsGrunnlag = BistandsRepository.hentHvisEksisterer(kontekst.behandlingId)
            val studentGrunnlag = studentRepository.hentHvisEksisterer(kontekst.behandlingId)

            val vilkårsresultat = vilkårsresultatRepository.hent(kontekst.behandlingId)
            if (studentGrunnlag?.studentvurdering?.oppfyller11_14 == true || bistandsGrunnlag != null) {
                for (periode in periodeTilVurdering) {
                    val grunnlag = BistandFaktagrunnlag(
                        periode.fom,
                        periode.tom,
                        bistandsGrunnlag?.vurdering,
                        studentGrunnlag?.studentvurdering
                    )
                    Bistandsvilkåret(vilkårsresultat).vurder(grunnlag = grunnlag)
                }
            }
            vilkårsresultatRepository.lagre(kontekst.behandlingId, vilkårsresultat)

            val vilkår = vilkårsresultat.finnVilkår(Vilkårtype.BISTANDSVILKÅRET)

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
