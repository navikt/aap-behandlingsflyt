package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderingerImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsresultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade.YrkesskadeGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade.YrkesskadeRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.Sykdomsvurdering
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FantAvklaringsbehov
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.lookup.repository.RepositoryProvider
import java.time.LocalDate

class VurderYrkesskadeSteg private constructor(
    private val vilkårsresultatRepository: VilkårsresultatRepository,
    private val sykdomRepository: SykdomRepository,
    private val yrkesskadeRepository: YrkesskadeRepository,
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
    private val tidligereVurderinger: TidligereVurderinger,
) : BehandlingSteg {
    constructor(repositoryProvider: RepositoryProvider) : this(
        vilkårsresultatRepository = repositoryProvider.provide(),
        sykdomRepository = repositoryProvider.provide(),
        yrkesskadeRepository = repositoryProvider.provide(),
        avklaringsbehovRepository = repositoryProvider.provide(),
        tidligereVurderinger = TidligereVurderingerImpl(repositoryProvider),
    )

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        val behandlingId = kontekst.behandlingId
        val vilkårsresultat = vilkårsresultatRepository.hent(behandlingId)
        val yrkesskader = yrkesskadeRepository.hentHvisEksisterer(behandlingId)
        val sykdomsgrunnlag = sykdomRepository.hentHvisEksisterer(behandlingId)
        val sykdomsvurderingTidslinje = sykdomsgrunnlag?.somSykdomsvurderingstidslinje(LocalDate.MIN)
        val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(behandlingId)

        when (kontekst.vurdering.vurderingType) {
            VurderingType.FØRSTEGANGSBEHANDLING -> {
                if (tidligereVurderinger.girIngenBehandlingsgrunnlag(kontekst, type())) {
                    avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)
                        .avbrytForSteg(type())
                    return Fullført
                }

                val erBehovForAvklaring = erBehovForAvklaring(vilkårsresultat, yrkesskader, sykdomsvurderingTidslinje)
                if (erBehovForAvklaring && sykdomsgrunnlag?.yrkesskadevurdering == null) {
                    return FantAvklaringsbehov(Definisjon.AVKLAR_YRKESSKADE)
                } else if (!erBehovForAvklaring && avklaringsbehovene.hentBehovForDefinisjon(Definisjon.AVKLAR_YRKESSKADE) != null) {
                    avklaringsbehovene.avbryt(Definisjon.AVKLAR_YRKESSKADE)
                }
            }

            VurderingType.REVURDERING -> {
                val erBehovForAvklaring = erBehovForAvklaring(vilkårsresultat, yrkesskader, sykdomsvurderingTidslinje)
                if (erBehovForAvklaring && sykdomsgrunnlag?.yrkesskadevurdering == null) {
                    return FantAvklaringsbehov(Definisjon.AVKLAR_YRKESSKADE)
                } else if (!erBehovForAvklaring && avklaringsbehovene.hentBehovForDefinisjon(Definisjon.AVKLAR_YRKESSKADE) != null) {
                    avklaringsbehovene.avbryt(Definisjon.AVKLAR_YRKESSKADE)
                }
            }

            VurderingType.MELDEKORT,
            VurderingType.IKKE_RELEVANT -> {
                // Do nothing
            }
        }

        return Fullført
    }

    private fun erBehovForAvklaring(
        vilkårsresultat: Vilkårsresultat,
        yrkesskadeGrunnlag: YrkesskadeGrunnlag?,
        sykdomGrunnlagTidslinje: Tidslinje<Sykdomsvurdering>?
    ): Boolean {
        if (!vilkårsresultat.finnVilkår(Vilkårtype.ALDERSVILKÅRET).harPerioderSomErOppfylt()) {
            return false
        }
        if (yrkesskadeGrunnlag?.yrkesskader?.harYrkesskade() != true) {
            return false
        }
        if (sykdomGrunnlagTidslinje?.any { it.verdi.erOppfyltSettBortIfraVissVarighet() } != true) {
            return false
        }

        return true
    }

    companion object : FlytSteg {
        override fun konstruer(repositoryProvider: RepositoryProvider): BehandlingSteg {
            return VurderYrkesskadeSteg(repositoryProvider)
        }

        override fun type(): StegType {
            return StegType.VURDER_YRKESSKADE
        }
    }
}
