package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.faktagrunnlag.student.StudentRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.sykdom.SykdomRepository
import no.nav.aap.verdityper.flyt.FlytKontekst
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.verdityper.flyt.StegType
import no.nav.aap.behandlingsflyt.faktagrunnlag.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.vilkår.sykdom.Sykdomsvilkår
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService

class VurderSykdomSteg private constructor(
    private val sykdomRepository: SykdomRepository,
    private val studentRepository: StudentRepository,
    private val vilkårsresultatRepository: VilkårsresultatRepository,
    private val periodeTilVurderingService: PeriodeTilVurderingService
) : BehandlingSteg {

    override fun utfør(kontekst: FlytKontekst): StegResultat {
        val periodeTilVurdering =
            periodeTilVurderingService.utled(kontekst = kontekst, vilkår = Vilkårtype.SYKDOMSVILKÅRET)

        if (periodeTilVurdering.isNotEmpty()) {
            val sykdomsGrunnlag = sykdomRepository.hentHvisEksisterer(behandlingId = kontekst.behandlingId)
            val studentGrunnlag = studentRepository.hentHvisEksisterer(behandlingId = kontekst.behandlingId)

            val vilkårResultat = vilkårsresultatRepository.hent(kontekst.behandlingId)
            if (sykdomsGrunnlag != null && sykdomsGrunnlag.erKonsistent() || studentGrunnlag?.studentvurdering?.oppfyller11_14 == true) {
                for (periode in periodeTilVurdering) {
                    val faktagrunnlag = no.nav.aap.behandlingsflyt.vilkår.sykdom.SykdomsFaktagrunnlag(
                        periode.fom,
                        periode.tom,
                        sykdomsGrunnlag?.yrkesskadevurdering,
                        sykdomsGrunnlag?.sykdomsvurdering,
                        studentGrunnlag?.studentvurdering
                    )
                    Sykdomsvilkår(vilkårResultat).vurder(faktagrunnlag)
                }
            }
            vilkårsresultatRepository.lagre(kontekst.behandlingId, vilkårResultat)
            val sykdomsvilkåret = vilkårResultat.finnVilkår(Vilkårtype.SYKDOMSVILKÅRET)

            if (sykdomsvilkåret.harPerioderSomIkkeErVurdert(periodeTilVurdering)
                || (studentGrunnlag?.studentvurdering?.oppfyller11_14 == false && sykdomsGrunnlag?.erKonsistent() != true)
            ) {
                return StegResultat(listOf(Definisjon.AVKLAR_SYKDOM))
            }
        }
        return StegResultat()
    }

    companion object : FlytSteg {
        override fun konstruer(connection: DBConnection): BehandlingSteg {
            return VurderSykdomSteg(
                SykdomRepository(connection),
                StudentRepository(connection),
                VilkårsresultatRepository(connection),
                PeriodeTilVurderingService(SakService(connection))
            )
        }

        override fun type(): StegType {
            return StegType.AVKLAR_SYKDOM
        }
    }
}
