package no.nav.aap.behandlingsflyt.flyt.steg.impl

import no.nav.aap.behandlingsflyt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.faktagrunnlag.student.StudentGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.student.StudentRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.sykdom.SykdomGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.sykdom.SykdomRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.yrkesskade.YrkesskadeGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.yrkesskade.YrkesskadeService
import no.nav.aap.behandlingsflyt.flyt.FlytKontekst
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.flyt.steg.StegType
import no.nav.aap.behandlingsflyt.flyt.vilkår.Vilkårtype
import no.nav.aap.behandlingsflyt.sak.SakService

class VurderYrkesskadeÅrsakssammenhengSteg private constructor(
    private val yrkesskadeService: YrkesskadeService,
    private val sykdomRepository: SykdomRepository,
    private val studentRepository: StudentRepository,
    private val periodeTilVurderingService: PeriodeTilVurderingService
) : BehandlingSteg {

    override fun utfør(kontekst: FlytKontekst): StegResultat {
        val periodeTilVurdering =
            periodeTilVurderingService.utled(kontekst = kontekst, vilkår = Vilkårtype.SYKDOMSVILKÅRET)

        if (periodeTilVurdering.isNotEmpty()) {
            val yrkesskadeGrunnlag = yrkesskadeService.hentHvisEksisterer(behandlingId = kontekst.behandlingId)
            val sykdomsGrunnlag = sykdomRepository.hentHvisEksisterer(behandlingId = kontekst.behandlingId)
            val studentGrunnlag = studentRepository.hentHvisEksisterer(behandlingId = kontekst.behandlingId)

            if (erBehovForAvklaring(yrkesskadeGrunnlag, sykdomsGrunnlag, studentGrunnlag)) {
                return StegResultat(listOf(Definisjon.AVKLAR_YRKESSKADE))
            }
        }
        return StegResultat()
    }

    private fun erBehovForAvklaring(
        yrkesskadeGrunnlag: YrkesskadeGrunnlag?,
        sykdomGrunnlag: SykdomGrunnlag?,
        studentGrunnlag: StudentGrunnlag?
    ): Boolean {
        if (studentGrunnlag?.studentvurdering?.oppfyller11_14 == true) {
            return false
        }
        return yrkesskadeGrunnlag?.yrkesskader?.yrkesskader?.isNotEmpty() == true
                && sykdomGrunnlag?.yrkesskadevurdering == null
    }

    companion object : FlytSteg {
        override fun konstruer(connection: DBConnection): VurderYrkesskadeÅrsakssammenhengSteg {
            return VurderYrkesskadeÅrsakssammenhengSteg(
                YrkesskadeService.konstruer(connection),
                SykdomRepository(connection),
                StudentRepository(connection),
                PeriodeTilVurderingService(SakService(connection))
            )
        }

        override fun type(): StegType {
            return StegType.AVKLAR_YRKESSKADE
        }
    }
}
