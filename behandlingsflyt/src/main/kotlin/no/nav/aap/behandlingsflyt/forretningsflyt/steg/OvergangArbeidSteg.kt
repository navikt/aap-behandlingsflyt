package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderingerImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårService
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
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
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import org.slf4j.LoggerFactory

class OvergangArbeidSteg private constructor(
    private val vilkårsresultatRepository: VilkårsresultatRepository,
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
    private val tidligereVurderinger: TidligereVurderinger,
    private val vilkårService: VilkårService,
    private val unleashGateway: UnleashGateway,
) : BehandlingSteg {
    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        vilkårsresultatRepository = repositoryProvider.provide(),
        avklaringsbehovRepository = repositoryProvider.provide(),
        tidligereVurderinger = TidligereVurderingerImpl(repositoryProvider),
        vilkårService = VilkårService(repositoryProvider),
        unleashGateway = gatewayProvider.provide(),
    )

    private val log = LoggerFactory.getLogger(javaClass)

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        if (unleashGateway.isEnabled(BehandlingsflytFeature.NyeSykdomVilkar)) {
            val vilkårsresultat = vilkårsresultatRepository.hent(kontekst.behandlingId)
            val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)
            when (kontekst.vurderingType) {
                VurderingType.FØRSTEGANGSBEHANDLING -> {
                    if (tidligereVurderinger.girIngenBehandlingsgrunnlag(kontekst, type())) {
                        log.info("Ingen behandlingsgrunnlag for vilkårtype ${Vilkårtype.OVERGANGARBEIDVILKÅRET} for behandlingId ${kontekst.behandlingId}. Avbryter steg.")
                        avklaringsbehovene.avbrytForSteg(type())
                        vilkårService.ingenNyeVurderinger(
                            kontekst.behandlingId,
                            Vilkårtype.OVERGANGARBEIDVILKÅRET,
                            kontekst.rettighetsperiode,
                            "mangler behandlingsgrunnlag",
                        )
                        return Fullført
                    }
                    if (harVurdertBistandsVilkår(avklaringsbehovene) && !bistandsVilkårErOppfylt(kontekst.behandlingId) && harIkkeVurdert1118tidligere(
                            avklaringsbehovene
                        )
                    ) {
                        return FantAvklaringsbehov(Definisjon.AVKLAR_OVERGANG_ARBEID)
                    }
                }

                VurderingType.REVURDERING -> {
                    if (tidligereVurderinger.girIngenBehandlingsgrunnlag(kontekst, type())) {
                        log.info("Ingen behandlingsgrunnlag for vilkårtype ${Vilkårtype.OVERGANGARBEIDVILKÅRET} for behandlingId ${kontekst.behandlingId}. Avbryter steg.")
                        avklaringsbehovene.avbrytForSteg(type())
                        vilkårService.ingenNyeVurderinger(
                            kontekst.behandlingId,
                            Vilkårtype.OVERGANGARBEIDVILKÅRET,
                            kontekst.rettighetsperiode,
                            "mangler behandlingsgrunnlag",
                        )
                        return Fullført
                    }
                    if (harVurdertBistandsVilkår(avklaringsbehovene) && !bistandsVilkårErOppfylt(kontekst.behandlingId) && harIkkeVurdert1118tidligere(
                            avklaringsbehovene
                        )
                    ) {
                        return FantAvklaringsbehov(Definisjon.AVKLAR_OVERGANG_ARBEID)
                    } else {
                        return Fullført
                    }
                }

                VurderingType.MELDEKORT,
                VurderingType.IKKE_RELEVANT -> {
                    // Skal ikke gjøre noe
                }
            }
            if (kontekst.harNoeTilBehandling()) {
                vilkårsresultatRepository.lagre(kontekst.behandlingId, vilkårsresultat)
            }

            return Fullført
        }
        return Fullført
    }


    private fun harVurdertBistandsVilkår(avklaringsbehovene: Avklaringsbehovene): Boolean {
        return avklaringsbehovene.erVurdertTidligereIBehandlingen(Definisjon.AVKLAR_BISTANDSBEHOV)
    }

    private fun bistandsVilkårErOppfylt(behandlingId: BehandlingId): Boolean {
        val alleBistandsVilkårOppfylt =
            vilkårsresultatRepository.hent(behandlingId).finnVilkår(Vilkårtype.BISTANDSVILKÅRET).vilkårsperioder()
                .all { it.erOppfylt() }
        return alleBistandsVilkårOppfylt
    }

    private fun harIkkeVurdert1118tidligere(avklaringsbehovene: Avklaringsbehovene): Boolean {
        return !avklaringsbehovene.erVurdertTidligereIBehandlingen(Definisjon.AVKLAR_OVERGANG_ARBEID)
    }


    companion object : FlytSteg {
        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): BehandlingSteg {
            return OvergangArbeidSteg(repositoryProvider, gatewayProvider)
        }

        override fun type(): StegType {
            return StegType.OVERGANG_ARBEID
        }
    }
}