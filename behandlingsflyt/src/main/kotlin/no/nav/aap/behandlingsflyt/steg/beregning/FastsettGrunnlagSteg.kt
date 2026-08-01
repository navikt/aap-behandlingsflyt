package no.nav.aap.behandlingsflyt.steg.beregning

import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderingerImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.vilkårsresultat.VilkårService
import no.nav.aap.behandlingsflyt.faktagrunnlag.vilkårsresultat.Vilkårsresultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.vilkårsresultat.Utfall
import no.nav.aap.vilkårsresultat.Vilkår
import no.nav.aap.vilkårsresultat.Vilkårsperiode
import no.nav.aap.vilkårsresultat.Vilkårtype
import org.slf4j.LoggerFactory

class FastsettGrunnlagSteg(
    private val beregningService: BeregningService,
    private val vilkårsresultatRepository: VilkårsresultatRepository,
    private val tidligereVurderinger: TidligereVurderinger,
    private val vilkårService: VilkårService,
) : BehandlingSteg {
    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        beregningService = BeregningService(repositoryProvider),
        vilkårsresultatRepository = repositoryProvider.provide(),
        tidligereVurderinger = TidligereVurderingerImpl(repositoryProvider, gatewayProvider),
        vilkårService = VilkårService(repositoryProvider),
    )

    private val log = LoggerFactory.getLogger(javaClass)


    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        val vilkårsresultat = vilkårsresultatRepository.hent(kontekst.behandlingId)
        val vilkår = vilkårsresultat.leggTilHvisIkkeEksisterer(Vilkårtype.GRUNNLAGET)
        val rettighetsperiode = kontekst.rettighetsperiode

        when (kontekst.vurderingType) {
            VurderingType.FØRSTEGANGSBEHANDLING, VurderingType.REVURDERING, VurderingType.MIGRER_RETTIGHETSPERIODE, VurderingType.MIGERING_FRA_ARENA -> {
                if (tidligereVurderinger.girAvslagEllerIngenBehandlingsgrunnlag(kontekst, type())) {
                    vilkårService.ingenNyeVurderinger(kontekst, Vilkårtype.GRUNNLAGET, "mangler behandlingsgrunnlag")
                    return Fullført
                }
                vurderVilkåret(kontekst, vilkår, rettighetsperiode, vilkårsresultat)
            }

            VurderingType.MELDEKORT,
            VurderingType.UTVID_VEDTAKSLENGDE,
            VurderingType.AUTOMATISK_BREV,
            VurderingType.EFFEKTUER_AKTIVITETSPLIKT,
            VurderingType.EFFEKTUER_AKTIVITETSPLIKT_11_9,
            VurderingType.G_REGULERING,
            VurderingType.OVERGANG_UFORE_STANS,
            VurderingType.IKKE_RELEVANT -> {
                // Do nothing
            }
        }

        return Fullført
    }

    private fun vurderVilkåret(
        kontekst: FlytKontekstMedPerioder,
        vilkår: Vilkår,
        rettighetsperiode: Periode,
        vilkårsresultat: Vilkårsresultat
    ) {
        if (!tidligereVurderinger.girAvslagEllerIngenBehandlingsgrunnlag(kontekst, type())) {
            val beregningsgrunnlag = beregningService.beregnGrunnlag(kontekst.behandlingId)

            vilkår.leggTilVurdering(
                Vilkårsperiode(
                    periode = rettighetsperiode,
                    utfall = Utfall.OPPFYLT,
                    manuellVurdering = false,
                    begrunnelse = null,
                    innvilgelsesårsak = null,
                    avslagsårsak = null,
                    faktagrunnlag = beregningsgrunnlag.faktagrunnlag()
                )
            )
            log.info("Beregnet grunnlag til ${beregningsgrunnlag.grunnlaget()}")
        } else {
            log.info("Deaktiverer grunnlag når det ikke er relevant å beregne")
            beregningService.deaktiverGrunnlag(kontekst.behandlingId)

            vilkår.leggTilVurdering(
                Vilkårsperiode(
                    periode = rettighetsperiode,
                    utfall = Utfall.IKKE_RELEVANT,
                    manuellVurdering = false,
                    begrunnelse = null,
                    innvilgelsesårsak = null,
                    avslagsårsak = null,
                    faktagrunnlag = null
                )
            )
        }
        vilkårsresultatRepository.lagre(kontekst.behandlingId, vilkårsresultat)
    }

    companion object : FlytSteg {
        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): BehandlingSteg {
            return FastsettGrunnlagSteg(repositoryProvider, gatewayProvider)
        }

        override fun type(): StegType {
            return StegType.FASTSETT_GRUNNLAG
        }
    }
}