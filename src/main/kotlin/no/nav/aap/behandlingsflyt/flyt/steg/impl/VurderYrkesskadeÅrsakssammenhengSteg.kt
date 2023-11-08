package no.nav.aap.behandlingsflyt.flyt.steg.impl

import no.nav.aap.behandlingsflyt.behandling.BehandlingService
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.faktagrunnlag.student.StudentGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.student.StudentRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.sykdom.SykdomsGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.sykdom.SykdomsRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.yrkesskade.YrkesskadeGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.yrkesskade.YrkesskadeRepository
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.StegInput
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.flyt.vilkår.Vilkårtype

class VurderYrkesskadeÅrsakssammenhengSteg(
    private val behandlingService: BehandlingService,
    private val studentRepository: StudentRepository,
    private val periodeTilVurderingService: PeriodeTilVurderingService
) : BehandlingSteg {

    override fun utfør(input: StegInput): StegResultat {
        val behandling = behandlingService.hent(input.kontekst.behandlingId)

        val periodeTilVurdering =
            periodeTilVurderingService.utled(behandling = behandling, vilkår = Vilkårtype.SYKDOMSVILKÅRET)

        if (periodeTilVurdering.isNotEmpty()) {
            val yrkesskadeGrunnlag = YrkesskadeRepository.hentHvisEksisterer(behandlingId = input.kontekst.behandlingId)
            val sykdomsGrunnlag = SykdomsRepository.hentHvisEksisterer(behandlingId = input.kontekst.behandlingId)
            val studentGrunnlag = studentRepository.hentHvisEksisterer(behandlingId = input.kontekst.behandlingId)

            if (erBehovForAvklaring(yrkesskadeGrunnlag, sykdomsGrunnlag, studentGrunnlag)) {
                return StegResultat(listOf(Definisjon.AVKLAR_YRKESSKADE))
            }
        }
        return StegResultat()
    }

    private fun erBehovForAvklaring(
        yrkesskadeGrunnlag: YrkesskadeGrunnlag?,
        sykdomsGrunnlag: SykdomsGrunnlag?,
        studentGrunnlag: StudentGrunnlag?
    ): Boolean {
        if (studentGrunnlag?.studentvurdering?.oppfyller11_14 == true) {
            return false
        }
        return yrkesskadeGrunnlag?.yrkesskader?.yrkesskader?.isNotEmpty() == true
                && sykdomsGrunnlag?.yrkesskadevurdering == null
    }
}
