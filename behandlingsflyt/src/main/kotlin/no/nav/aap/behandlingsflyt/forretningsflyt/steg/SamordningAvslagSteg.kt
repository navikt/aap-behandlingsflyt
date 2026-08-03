package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.avslag11_27.Avslag11_27Repository
import no.nav.aap.behandlingsflyt.behandling.samordning.SamordningService
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderingerImpl
import no.nav.aap.behandlingsflyt.behandling.vilkår.samordning.annenfullytelse.SamordningAnnenFullYtelseFaktagrunnlag
import no.nav.aap.behandlingsflyt.behandling.vilkår.samordning.annenfullytelse.SamordningAnnenFullYtelseVilkår
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.uførevurdering.SamordningUføreRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårService
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre.UføreRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.KravRepository
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider


class SamordningAvslagSteg(
    private val samordningService: SamordningService,
    private val uføreRepository: UføreRepository,
    private val samordningUføreRepository: SamordningUføreRepository,
    private val avslag1127repository: Avslag11_27Repository,
    private val kravRepository: KravRepository,
    private val vilkårService: VilkårService,
    private val tidligereVurderinger: TidligereVurderinger,
    private val unleashGateway: UnleashGateway,
) : BehandlingSteg {
    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        samordningService = SamordningService(repositoryProvider),
        uføreRepository = repositoryProvider.provide(),
        samordningUføreRepository = repositoryProvider.provide(),
        avslag1127repository = repositoryProvider.provide(),
        kravRepository = repositoryProvider.provide(),
        vilkårService = VilkårService(repositoryProvider),
        tidligereVurderinger = TidligereVurderingerImpl(repositoryProvider, gatewayProvider),
        unleashGateway = gatewayProvider.provide()
    )

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        if (kontekst.vurderingType == VurderingType.FØRSTEGANGSBEHANDLING &&
            tidligereVurderinger.girIngenBehandlingsgrunnlag(kontekst, type())
        ) {
            return Fullført
        }

        val grunnlag = utledFaktagrunnlag(kontekst)
        vilkårService.vurderVilkår(kontekst.behandlingId, grunnlag, SamordningAnnenFullYtelseVilkår)

        return Fullført
    }

    private fun utledFaktagrunnlag(kontekst: FlytKontekstMedPerioder) =
        SamordningAnnenFullYtelseFaktagrunnlag(
            rettighetsperiode = kontekst.rettighetsperiode,
            samordningGrunnlag = samordningService.samordningGrunnlag(kontekst.behandlingId),
            uføreRegisterGrunnlag = uføreRepository.hentHvisEksisterer(kontekst.behandlingId),
            uføreVurderingGrunnlag = samordningUføreRepository.hentHvisEksisterer(kontekst.behandlingId),
            avslag1127grunnlag = avslag1127repository.hentHvisEksisterer(kontekst.behandlingId),
            kravGrunnlag = kravRepository.hentHvisEksisterer(kontekst.behandlingId),
            strekkAvslagOverHelger = unleashGateway.isEnabled(BehandlingsflytFeature.StrekkAvslagOverHelg)
        )

    companion object : FlytSteg {
        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): BehandlingSteg {
            return SamordningAvslagSteg(repositoryProvider, gatewayProvider)
        }

        override fun type(): StegType {
            return StegType.SAMORDNING_AVSLAG
        }
    }
}