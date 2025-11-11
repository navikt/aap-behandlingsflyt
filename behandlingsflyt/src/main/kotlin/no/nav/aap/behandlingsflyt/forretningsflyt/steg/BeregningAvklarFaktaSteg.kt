package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovService
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderingerImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Innvilgelsesårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsresultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade.YrkesskadeRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningVurderingRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.YrkesskadeSak
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FantAvklaringsbehov
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider

class BeregningAvklarFaktaSteg private constructor(
    private val beregningVurderingRepository: BeregningVurderingRepository,
    private val sykdomRepository: SykdomRepository,
    private val vilkårsresultatRepository: VilkårsresultatRepository,
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
    private val yrkesskadeRepository: YrkesskadeRepository,
    private val tidligereVurderinger: TidligereVurderinger,
    private val avklaringsbehovService: AvklaringsbehovService,
    private val unleashGateway: UnleashGateway
) : BehandlingSteg {
    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        beregningVurderingRepository = repositoryProvider.provide(),
        sykdomRepository = repositoryProvider.provide(),
        vilkårsresultatRepository = repositoryProvider.provide(),
        avklaringsbehovRepository = repositoryProvider.provide(),
        yrkesskadeRepository = repositoryProvider.provide(),
        tidligereVurderinger = TidligereVurderingerImpl(repositoryProvider),
        avklaringsbehovService = AvklaringsbehovService(repositoryProvider),
        unleashGateway = gatewayProvider.provide()
    )

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        if (unleashGateway.isDisabled(BehandlingsflytFeature.NyBeregningAvklarFaktaSteg)) {
            return gammelUtfør(kontekst)
        }

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
                    VurderingType.EFFEKTUER_AKTIVITETSPLIKT_11_9,
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
                    VurderingType.EFFEKTUER_AKTIVITETSPLIKT_11_9,
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

    fun gammelUtfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        val behandlingId = kontekst.behandlingId

        if (tidligereVurderinger.girAvslagEllerIngenBehandlingsgrunnlag(kontekst, type())) {
            avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)
                .avbrytForSteg(type())
            return Fullført
        }

        when (kontekst.vurderingType) {
            VurderingType.FØRSTEGANGSBEHANDLING -> {
                if (skalAvbryteForStegPgaIngenBehandlingsgrunnlag(kontekst)) {
                    return Fullført
                }

                val beregningVurdering = beregningVurderingRepository.hentHvisEksisterer(behandlingId)

                val vilkårsresultat = vilkårsresultatRepository.hent(behandlingId)
                val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(behandlingId)
                if (beregningVurdering == null && erIkkeStudent(vilkårsresultat)) {
                    return FantAvklaringsbehov(Definisjon.FASTSETT_BEREGNINGSTIDSPUNKT)
                }
                val avklaringsbehov = avklaringsbehovene.hentBehovForDefinisjon(Definisjon.FASTSETT_YRKESSKADEINNTEKT)
                if (erBehovForÅAvklareYrkesskade(behandlingId, beregningVurdering)) {
                    return FantAvklaringsbehov(Definisjon.FASTSETT_YRKESSKADEINNTEKT)
                } else if (avklaringsbehov != null && avklaringsbehov.erÅpent()) {
                    avklaringsbehovene.avbryt(Definisjon.FASTSETT_YRKESSKADEINNTEKT)
                }
            }

            VurderingType.REVURDERING -> {
                if (skalAvbryteForStegPgaIngenBehandlingsgrunnlag(kontekst)) {
                    return Fullført
                }

                val beregningVurdering = beregningVurderingRepository.hentHvisEksisterer(behandlingId)

                val vilkårsresultat = vilkårsresultatRepository.hent(behandlingId)
                val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(behandlingId)
                if ((beregningVurdering == null || erIkkeVurdertTidligereIBehandlingen(
                        avklaringsbehovene,
                        Definisjon.FASTSETT_BEREGNINGSTIDSPUNKT
                    )) && erIkkeStudent(vilkårsresultat)
                ) {
                    return FantAvklaringsbehov(Definisjon.FASTSETT_BEREGNINGSTIDSPUNKT)
                }
                val avklaringsbehov = avklaringsbehovene.hentBehovForDefinisjon(Definisjon.FASTSETT_YRKESSKADEINNTEKT)
                if (erBehovForÅAvklareYrkesskadeRevurdering(behandlingId, beregningVurdering, avklaringsbehovene)) {
                    return FantAvklaringsbehov(Definisjon.FASTSETT_YRKESSKADEINNTEKT)
                } else if (avklaringsbehov != null && avklaringsbehov.erÅpent()) {
                    avklaringsbehovene.avbryt(Definisjon.FASTSETT_YRKESSKADEINNTEKT)
                }
            }

            VurderingType.MELDEKORT,
            VurderingType.EFFEKTUER_AKTIVITETSPLIKT,
            VurderingType.EFFEKTUER_AKTIVITETSPLIKT_11_9,
            VurderingType.IKKE_RELEVANT -> {
                // Always do nothing
            }
        }
        return Fullført
    }

    private fun erIkkeVurdertTidligereIBehandlingen(
        avklaringsbehovene: Avklaringsbehovene,
        definisjon: Definisjon
    ): Boolean {
        return !avklaringsbehovene.erVurdertTidligereIBehandlingen(definisjon)
    }

    private fun erIkkeStudent(vilkårsresultat: Vilkårsresultat): Boolean {
        return vilkårsresultat.finnVilkår(Vilkårtype.SYKDOMSVILKÅRET).vilkårsperioder()
            .firstOrNull()?.innvilgelsesårsak != Innvilgelsesårsak.STUDENT
    }

    private fun erBehovForÅAvklareYrkesskade(
        behandlingId: BehandlingId,
        beregningGrunnlag: BeregningGrunnlag?
    ): Boolean {
        val yrkesskadeVurdering = sykdomRepository.hentHvisEksisterer(behandlingId)?.yrkesskadevurdering
        val yrkesskader = yrkesskadeRepository.hentHvisEksisterer(behandlingId)
        return yrkesskader?.yrkesskader?.harYrkesskade() == true && yrkesskadeVurdering?.erÅrsakssammenheng == true && harIkkeFastsattBeløpForAlle(
            yrkesskadeVurdering.relevanteSaker,
            beregningGrunnlag
        )
    }

    private fun erBehovForÅAvklareYrkesskadeRevurdering(
        behandlingId: BehandlingId,
        beregningGrunnlag: BeregningGrunnlag?,
        avklaringsbehovene: Avklaringsbehovene
    ): Boolean {
        val yrkesskadeVurdering = sykdomRepository.hentHvisEksisterer(behandlingId)?.yrkesskadevurdering
        val yrkesskader = yrkesskadeRepository.hentHvisEksisterer(behandlingId)
        return yrkesskader?.yrkesskader?.harYrkesskade() == true && yrkesskadeVurdering?.erÅrsakssammenheng == true && (harIkkeFastsattBeløpForAlle(
            yrkesskadeVurdering.relevanteSaker,
            beregningGrunnlag
        ) || erIkkeVurdertTidligereIBehandlingen(avklaringsbehovene, Definisjon.FASTSETT_YRKESSKADEINNTEKT))
    }

    private fun harIkkeFastsattBeløpForAlle(
        relevanteSaker: List<YrkesskadeSak>,
        beregningGrunnlag: BeregningGrunnlag?
    ): Boolean {
        val vurderteSaker = beregningGrunnlag?.yrkesskadeBeløpVurdering?.vurderinger.orEmpty()
        if (relevanteSaker.isEmpty()) {
            return false
        }
        return !relevanteSaker.all { sak -> vurderteSaker.any { it.referanse == sak.referanse } }
    }

    private fun skalAvbryteForStegPgaIngenBehandlingsgrunnlag(kontekst: FlytKontekstMedPerioder): Boolean {
        if (tidligereVurderinger.girIngenBehandlingsgrunnlag(kontekst, type())) {
            avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)
                .avbrytForSteg(type())
            return true
        }
        return false
    }

    companion object : FlytSteg {
        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): BehandlingSteg {
            return BeregningAvklarFaktaSteg(repositoryProvider, gatewayProvider)
        }

        override val rekkefølge: List<Definisjon> = listOf(
            Definisjon.FASTSETT_BEREGNINGSTIDSPUNKT,
            Definisjon.FASTSETT_YRKESSKADEINNTEKT,
        )

        override fun type(): StegType {
            return StegType.FASTSETT_BEREGNINGSTIDSPUNKT
        }
    }
}
