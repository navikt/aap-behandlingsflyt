package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.avklaringsbehov.AvklaringsbehovRepositoryImpl
import no.nav.aap.behandlingsflyt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.StudentRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomRepository

import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.vilkår.sykdom.Sykdomsvilkår
import no.nav.aap.verdityper.flyt.FlytKontekstMedPerioder
import no.nav.aap.verdityper.flyt.StegType

class VurderSykdomSteg private constructor(
    private val sykdomRepository: SykdomRepository,
    private val studentRepository: StudentRepository,
    private val vilkårsresultatRepository: VilkårsresultatRepository,
    private val avklaringsbehovRepository: AvklaringsbehovRepository
) : BehandlingSteg {

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {

        if (kontekst.perioderTilVurdering.isNotEmpty()) {
            val sykdomsGrunnlag = sykdomRepository.hentHvisEksisterer(behandlingId = kontekst.behandlingId)
            val studentGrunnlag = studentRepository.hentHvisEksisterer(behandlingId = kontekst.behandlingId)

            val vilkårResultat = vilkårsresultatRepository.hent(kontekst.behandlingId)
            val studentVurdering = studentGrunnlag?.studentvurdering

            if (sykdomsGrunnlag != null && sykdomsGrunnlag.erKonsistent() || studentVurdering?.erOppfylt() == true) {
                for (periode in kontekst.perioderTilVurdering) {
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
            val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)

            if (sykdomsvilkåret.harPerioderSomIkkeErVurdert(kontekst.perioderTilVurdering) || (studentVurdering?.erOppfylt() == false && sykdomsGrunnlag?.erKonsistent() != true)
            ) {
                return StegResultat(listOf(Definisjon.AVKLAR_SYKDOM))
            } else {
                val avklaringsbehov = avklaringsbehovene.hentBehovForDefinisjon(Definisjon.AVKLAR_SYKDOM)
                if (avklaringsbehov != null && avklaringsbehov.erÅpent()) {
                    avklaringsbehov.avbryt()
                }
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
                AvklaringsbehovRepositoryImpl(connection)
            )
        }

        override fun type(): StegType {
            return StegType.AVKLAR_SYKDOM
        }
    }
}
