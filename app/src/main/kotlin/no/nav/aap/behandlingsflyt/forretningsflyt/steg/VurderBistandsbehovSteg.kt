package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.faktagrunnlag.bistand.BistandRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.student.StudentGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.student.StudentRepository
import no.nav.aap.verdityper.flyt.FlytKontekst
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.verdityper.flyt.StegType
import no.nav.aap.behandlingsflyt.faktagrunnlag.vilkårsresultat.Innvilgelsesårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.vilkårsresultat.Vilkår
import no.nav.aap.behandlingsflyt.faktagrunnlag.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.vilkår.bistand.BistandFaktagrunnlag
import no.nav.aap.behandlingsflyt.vilkår.bistand.Bistandsvilkåret
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.verdityper.Periode

class VurderBistandsbehovSteg private constructor(
    private val bistandRepository: BistandRepository,
    private val studentRepository: StudentRepository,
    private val vilkårsresultatRepository: VilkårsresultatRepository,
    private val periodeTilVurderingService: PeriodeTilVurderingService
) : BehandlingSteg {

    override fun utfør(kontekst: FlytKontekst): StegResultat {
        val periodeTilVurdering =
            periodeTilVurderingService.utled(kontekst = kontekst, vilkår = Vilkårtype.BISTANDSVILKÅRET)

        if (periodeTilVurdering.isNotEmpty()) {
            val bistandsGrunnlag = bistandRepository.hentHvisEksisterer(kontekst.behandlingId)
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
        return studentGrunnlag?.studentvurdering?.oppfyller11_14 == false &&
                vilkår.vilkårsperioder().any { it.innvilgelsesårsak == Innvilgelsesårsak.STUDENT }
    }

    companion object : FlytSteg {
        override fun konstruer(connection: DBConnection): BehandlingSteg {
            return VurderBistandsbehovSteg(
                BistandRepository(connection),
                StudentRepository(connection),
                VilkårsresultatRepository(connection),
                PeriodeTilVurderingService(SakService(connection))
            )
        }

        override fun type(): StegType {
            return StegType.VURDER_BISTANDSBEHOV
        }
    }
}
