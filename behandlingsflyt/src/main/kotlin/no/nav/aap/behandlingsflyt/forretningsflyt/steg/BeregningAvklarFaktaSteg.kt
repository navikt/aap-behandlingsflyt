package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovService
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderingerImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Innvilgelsesårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade.YrkesskadeRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningVurderingRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.YrkesskadeSak
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider

class BeregningAvklarFaktaSteg private constructor(
    private val beregningVurderingRepository: BeregningVurderingRepository,
    private val sykdomRepository: SykdomRepository,
    private val vilkårsresultatRepository: VilkårsresultatRepository,
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
    private val yrkesskadeRepository: YrkesskadeRepository,
    private val tidligereVurderinger: TidligereVurderinger,
    private val avklaringsbehovService: AvklaringsbehovService
) : BehandlingSteg {
    constructor(repositoryProvider: RepositoryProvider) : this(
        beregningVurderingRepository = repositoryProvider.provide(),
        sykdomRepository = repositoryProvider.provide(),
        vilkårsresultatRepository = repositoryProvider.provide(),
        avklaringsbehovRepository = repositoryProvider.provide(),
        yrkesskadeRepository = repositoryProvider.provide(),
        tidligereVurderinger = TidligereVurderingerImpl(repositoryProvider),
        avklaringsbehovService = AvklaringsbehovService(repositoryProvider)
    )

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        val behandlingId = kontekst.behandlingId
        val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(behandlingId)

        // Beregningstidspunkt
        avklaringsbehovService.oppdaterAvklaringsbehov(
            kontekst = kontekst,
            avklaringsbehovene = avklaringsbehovene,
            definisjon = Definisjon.FASTSETT_BEREGNINGSTIDSPUNKT,
            vedtakBehøverVurdering = {
                when (kontekst.vurderingType) {
                    VurderingType.FØRSTEGANGSBEHANDLING,
                    VurderingType.REVURDERING -> {
                        when {
                            tidligereVurderinger.girAvslagEllerIngenBehandlingsgrunnlag(kontekst, type()) -> false
                            kontekst.vurderingsbehovRelevanteForSteg.isEmpty() -> false
                            manueltTriggetVurderingsbehovBeregning(kontekst) -> erIkkeStudent(behandlingId)
                            manueltTriggetVurderingsbehovYrkesskade(kontekst) -> false
                            else -> {
                                erIkkeStudent(behandlingId)
                            }
                        }
                    }
                    VurderingType.MELDEKORT,
                    VurderingType.EFFEKTUER_AKTIVITETSPLIKT,
                    VurderingType.IKKE_RELEVANT ->
                        false
                }
            },
            erTilstrekkeligVurdert = {
                val beregningVurdering = beregningVurderingRepository.hentHvisEksisterer(behandlingId)
                beregningVurdering?.tidspunktVurdering != null
            },
            tilbakestillGrunnlag = {
                val vedtattVurdering = kontekst.forrigeBehandlingId
                    ?.let { beregningVurderingRepository.hentHvisEksisterer(it) }
                    ?.tidspunktVurdering

                beregningVurderingRepository.lagre(kontekst.behandlingId, vedtattVurdering)
            },
        )

        // TODO dette føles litt rart, men må kanskje bli slik da disse bør gjøres i rekkefølge?
        if (avklaringsbehovene.hentBehovForDefinisjon(Definisjon.FASTSETT_BEREGNINGSTIDSPUNKT)?.erÅpent() == true) {
            // Hvis beregningstidspunkt fortsatt er åpent, kan vi ikke gå videre til yrkesskade
            return Fullført
        }

        // Yrkesskadeinntekt
        avklaringsbehovService.oppdaterAvklaringsbehov(
            kontekst = kontekst,
            avklaringsbehovene = avklaringsbehovene,
            definisjon = Definisjon.FASTSETT_YRKESSKADEINNTEKT,
            vedtakBehøverVurdering = {
                when (kontekst.vurderingType) {
                    VurderingType.FØRSTEGANGSBEHANDLING,
                    VurderingType.REVURDERING -> {
                        when {
                            tidligereVurderinger.girAvslagEllerIngenBehandlingsgrunnlag(kontekst, type()) -> false
                            kontekst.vurderingsbehovRelevanteForSteg.isEmpty() -> false
                            manueltTriggetVurderingsbehovYrkesskade(kontekst) -> {
                                harYrkesskadeMedÅrsakssammenheng(behandlingId)
                            }
                            manueltTriggetVurderingsbehovBeregning(kontekst) -> false
                            else -> {
                                harYrkesskadeMedÅrsakssammenheng(behandlingId)
                            }
                        }
                    }
                    VurderingType.MELDEKORT,
                    VurderingType.EFFEKTUER_AKTIVITETSPLIKT,
                    VurderingType.IKKE_RELEVANT ->
                        false
                }
            },
            erTilstrekkeligVurdert = {
                val beregningGrunnlag = beregningVurderingRepository.hentHvisEksisterer(behandlingId)
                val yrkesskadeVurdering = sykdomRepository.hentHvisEksisterer(behandlingId)?.yrkesskadevurdering
                val relevanteSaker = yrkesskadeVurdering?.relevanteSaker.orEmpty()
                harFastsattBeløpForAlleRelevanteYrkesskadesaker(relevanteSaker, beregningGrunnlag)
            },
            tilbakestillGrunnlag = {
                val vedtattVurderinger = kontekst.forrigeBehandlingId
                    ?.let { beregningVurderingRepository.hentHvisEksisterer(it) }
                    ?.yrkesskadeBeløpVurdering?.vurderinger
                    ?: emptyList()

                val nyeVurderinger = beregningVurderingRepository.hentHvisEksisterer(behandlingId)
                    ?.yrkesskadeBeløpVurdering?.vurderinger

                if (nyeVurderinger != vedtattVurderinger) {
                    beregningVurderingRepository.lagre(kontekst.behandlingId, vedtattVurderinger)
                }
            },
        )

        return Fullført
    }

    private fun manueltTriggetVurderingsbehovBeregning(kontekst: FlytKontekstMedPerioder): Boolean {
        return kontekst.vurderingsbehovRelevanteForSteg.any {
            it in listOf(
                Vurderingsbehov.MOTTATT_SØKNAD,
                Vurderingsbehov.HELHETLIG_VURDERING,
                Vurderingsbehov.VURDER_RETTIGHETSPERIODE,
                Vurderingsbehov.REVURDER_BEREGNING
            )
        }
    }

    private fun manueltTriggetVurderingsbehovYrkesskade(kontekst: FlytKontekstMedPerioder): Boolean {
        return kontekst.vurderingsbehovRelevanteForSteg.any {
            it in listOf(
                Vurderingsbehov.MOTTATT_SØKNAD,
                Vurderingsbehov.HELHETLIG_VURDERING,
                Vurderingsbehov.VURDER_RETTIGHETSPERIODE,
                Vurderingsbehov.REVURDER_YRKESSKADE
            )
        }
    }

    private fun harYrkesskadeMedÅrsakssammenheng(behandlingId: BehandlingId): Boolean {
        val yrkesskadeVurdering = sykdomRepository.hentHvisEksisterer(behandlingId)?.yrkesskadevurdering
        val yrkesskadeGrunnlag = yrkesskadeRepository.hentHvisEksisterer(behandlingId)
        return yrkesskadeGrunnlag?.yrkesskader?.harYrkesskade() == true
                && yrkesskadeVurdering?.erÅrsakssammenheng == true
    }

    private fun erIkkeStudent(behandlingId: BehandlingId): Boolean {
        val vilkårsresultat = vilkårsresultatRepository.hent(behandlingId)
        return vilkårsresultat.finnVilkår(Vilkårtype.SYKDOMSVILKÅRET).vilkårsperioder()
            .firstOrNull()?.innvilgelsesårsak != Innvilgelsesårsak.STUDENT
    }

    private fun harFastsattBeløpForAlleRelevanteYrkesskadesaker(
        relevanteSaker: List<YrkesskadeSak>,
        beregningGrunnlag: BeregningGrunnlag?
    ): Boolean {
        val vurderteSaker = beregningGrunnlag?.yrkesskadeBeløpVurdering?.vurderinger.orEmpty()
        return relevanteSaker.all { sak -> vurderteSaker.any { it.referanse == sak.referanse } }
    }

    companion object : FlytSteg {
        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): BehandlingSteg {
            return BeregningAvklarFaktaSteg(repositoryProvider)
        }

        override fun type(): StegType {
            return StegType.FASTSETT_BEREGNINGSTIDSPUNKT
        }
    }
}
