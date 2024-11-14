package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepositoryImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsresultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade.YrkesskadeRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.StudentRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomRepository
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FantAvklaringsbehov
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.verdityper.flyt.FlytKontekstMedPerioder

class VurderSykdomSteg private constructor(
    private val sykdomRepository: SykdomRepository,
    private val studentRepository: StudentRepository,
    private val yrkesskadeRepository: YrkesskadeRepository,
    private val vilkårsresultatRepository: VilkårsresultatRepository,
    private val avklaringsbehovRepository: AvklaringsbehovRepository
) : BehandlingSteg {

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {

        if (kontekst.perioderTilVurdering.isNotEmpty()) {
            val sykdomsGrunnlag = sykdomRepository.hentHvisEksisterer(behandlingId = kontekst.behandlingId)
            val studentGrunnlag = studentRepository.hentHvisEksisterer(behandlingId = kontekst.behandlingId)
            val yrkesskadeGrunnlag = yrkesskadeRepository.hentHvisEksisterer(behandlingId = kontekst.behandlingId)

            val vilkårResultat = vilkårsresultatRepository.hent(kontekst.behandlingId)
            val studentVurdering = studentGrunnlag?.studentvurdering

            val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)

            if (erIkkeAvslagPåVilkårTidligere(vilkårResultat) && (studentVurdering?.erOppfylt() != true && sykdomsGrunnlag?.erKonsistentForSykdom(yrkesskadeGrunnlag?.yrkesskader?.harYrkesskade() == true) != true)
            ) {
                return FantAvklaringsbehov(Definisjon.AVKLAR_SYKDOM)
            } else {
                val avklaringsbehov = avklaringsbehovene.hentBehovForDefinisjon(Definisjon.AVKLAR_SYKDOM)
                if (avklaringsbehov != null && avklaringsbehov.erÅpent() && studentVurdering?.erOppfylt() == true) {
                    avklaringsbehovene.avbryt(Definisjon.AVKLAR_SYKDOM)
                }
            }
        }
        return Fullført
    }

    private fun erIkkeAvslagPåVilkårTidligere(vilkårsresultat: Vilkårsresultat): Boolean {
        return vilkårsresultat.finnVilkår(Vilkårtype.ALDERSVILKÅRET).harPerioderSomErOppfylt()
    }

    companion object : FlytSteg {
        override fun konstruer(connection: DBConnection): BehandlingSteg {
            return VurderSykdomSteg(
                SykdomRepository(connection),
                StudentRepository(connection),
                YrkesskadeRepository(connection),
                VilkårsresultatRepository(connection),
                AvklaringsbehovRepositoryImpl(connection)
            )
        }

        override fun type(): StegType {
            return StegType.AVKLAR_SYKDOM
        }
    }
}
