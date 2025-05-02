package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
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
import no.nav.aap.lookup.repository.RepositoryProvider

class BeregningAvklarFaktaSteg private constructor(
    private val beregningVurderingRepository: BeregningVurderingRepository,
    private val sykdomRepository: SykdomRepository,
    private val vilkårsresultatRepository: VilkårsresultatRepository,
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
    private val yrkesskadeRepository: YrkesskadeRepository,
    private val tidligereVurderinger: TidligereVurderinger,
) : BehandlingSteg {
    constructor(repositoryProvider: RepositoryProvider) : this(
        beregningVurderingRepository = repositoryProvider.provide(),
        sykdomRepository = repositoryProvider.provide(),
        vilkårsresultatRepository = repositoryProvider.provide(),
        avklaringsbehovRepository = repositoryProvider.provide(),
        yrkesskadeRepository = repositoryProvider.provide(),
        tidligereVurderinger = TidligereVurderingerImpl(repositoryProvider),
    )

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        val behandlingId = kontekst.behandlingId

        if (tidligereVurderinger.girAvslagEllerIngenBehandlingsgrunnlag(kontekst, type())) {
            // TODO: Avbryte eventuelle avklaringsbehov som henger her hvis de er aktive
            return Fullført
        }

        when (kontekst.vurdering.vurderingType) {
            VurderingType.FØRSTEGANGSBEHANDLING -> {
                if (tidligereVurderinger.girIngenBehandlingsgrunnlag(kontekst, type())) {
                    avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)
                        .avbrytForSteg(type())
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
                } else if (avklaringsbehov != null) {
                    avklaringsbehovene.avbryt(Definisjon.FASTSETT_YRKESSKADEINNTEKT)
                }
            }

            VurderingType.REVURDERING -> {
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
                } else if (avklaringsbehov != null) {
                    avklaringsbehovene.avbryt(Definisjon.FASTSETT_YRKESSKADEINNTEKT)
                }
            }

            VurderingType.FORLENGELSE,
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
        val avklaringsbehov = avklaringsbehovene.hentBehovForDefinisjon(definisjon)
        return (avklaringsbehov == null || avklaringsbehov.erÅpent())
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
        relevanteSaker: List<String>,
        beregningGrunnlag: BeregningGrunnlag?
    ): Boolean {
        val vurderteSaker = beregningGrunnlag?.yrkesskadeBeløpVurdering?.vurderinger ?: emptyList()
        if (relevanteSaker.isEmpty()) {
            return false
        }
        return !relevanteSaker.all { sak -> vurderteSaker.any { it.referanse == sak } }
    }

    companion object : FlytSteg {
        override fun konstruer(repositoryProvider: RepositoryProvider): BehandlingSteg {
            return BeregningAvklarFaktaSteg(repositoryProvider)
        }

        override fun type(): StegType {
            return StegType.FASTSETT_BEREGNINGSTIDSPUNKT
        }
    }
}
