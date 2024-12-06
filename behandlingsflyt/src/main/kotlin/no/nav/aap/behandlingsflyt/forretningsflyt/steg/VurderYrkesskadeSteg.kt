package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepositoryImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsresultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepositoryImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade.YrkesskadeGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade.YrkesskadeRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomRepository
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FantAvklaringsbehov
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.komponenter.dbconnect.DBConnection

class VurderYrkesskadeSteg private constructor(
    private val vilkårsresultatRepository: VilkårsresultatRepositoryImpl,
    private val sykdomRepository: SykdomRepository,
    private val yrkesskadeRepository: YrkesskadeRepository,
    private val avklaringsbehovRepository: AvklaringsbehovRepositoryImpl
) : BehandlingSteg {

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        val behandlingId = kontekst.behandlingId

        val vilkårsresultat = vilkårsresultatRepository.hent(behandlingId)
        val yrkesskader = yrkesskadeRepository.hentHvisEksisterer(behandlingId)
        val sykdomsgrunnlag = sykdomRepository.hentHvisEksisterer(behandlingId)
        val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(behandlingId)

        val erBehovForAvklaring = erBehovForAvklaring(vilkårsresultat, yrkesskader, sykdomsgrunnlag)
        if (erBehovForAvklaring && sykdomsgrunnlag?.yrkesskadevurdering == null) {
            return FantAvklaringsbehov(Definisjon.AVKLAR_YRKESSKADE)
        } else if (!erBehovForAvklaring && avklaringsbehovene.hentBehovForDefinisjon(Definisjon.AVKLAR_YRKESSKADE) != null) {
            avklaringsbehovene.avbryt(Definisjon.AVKLAR_YRKESSKADE)
        }

        return Fullført
    }

    private fun erBehovForAvklaring(
        vilkårsresultat: Vilkårsresultat,
        yrkesskadeGrunnlag: YrkesskadeGrunnlag?,
        sykdomGrunnlag: SykdomGrunnlag?
    ): Boolean {
        if (!vilkårsresultat.finnVilkår(Vilkårtype.ALDERSVILKÅRET).harPerioderSomErOppfylt()) {
            return false
        }
        if (yrkesskadeGrunnlag?.yrkesskader?.harYrkesskade() != true) {
            return false
        }
        if (sykdomGrunnlag?.sykdomsvurdering?.erNedsettelseIArbeidsevneMerEnnHalvparten == false && sykdomGrunnlag.sykdomsvurdering?.erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense == false) {
            return false
        }

        return true
    }

    companion object : FlytSteg {
        override fun konstruer(connection: DBConnection): BehandlingSteg {
            return VurderYrkesskadeSteg(
                VilkårsresultatRepositoryImpl(connection),
                SykdomRepository(connection),
                YrkesskadeRepository(connection),
                AvklaringsbehovRepositoryImpl(connection)
            )
        }

        override fun type(): StegType {
            return StegType.VURDER_YRKESSKADE
        }
    }
}
