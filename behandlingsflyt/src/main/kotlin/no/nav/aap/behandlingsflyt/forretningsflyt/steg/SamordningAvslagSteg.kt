package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.samordning.SamordningService
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderingerImpl
import no.nav.aap.behandlingsflyt.behandling.vilkår.samordning.annenfullytelse.SamordningAnnenFullYtelseFaktagrunnlag
import no.nav.aap.behandlingsflyt.behandling.vilkår.samordning.annenfullytelse.SamordningAnnenFullYtelseVilkår
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.SamordningYtelseVurderingGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.uførevurdering.SamordningUføreRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre.UføreRepository
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider


class SamordningAvslagSteg(
    private val samordningService: SamordningService,
    private val uføreRepository: UføreRepository,
    private val samordningUføreRepository: SamordningUføreRepository,
    private val vilkårsresultatRepository: VilkårsresultatRepository,
    private val tidligereVurderinger: TidligereVurderinger,
) : BehandlingSteg {
    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        samordningService = SamordningService(repositoryProvider),
        uføreRepository = repositoryProvider.provide(),
        samordningUføreRepository = repositoryProvider.provide(),
        vilkårsresultatRepository = repositoryProvider.provide(),
        tidligereVurderinger = TidligereVurderingerImpl(repositoryProvider, gatewayProvider),
    )

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        if (kontekst.vurderingType == VurderingType.FØRSTEGANGSBEHANDLING &&
            tidligereVurderinger.girIngenBehandlingsgrunnlag(kontekst, type())
        ) {
            return Fullført
        }

        val vilkårsresultat = vilkårsresultatRepository.hent(kontekst.behandlingId)
        val grunnlag = utledFaktagrunnlag(kontekst)
        SamordningAnnenFullYtelseVilkår(vilkårsresultat).vurder(grunnlag)
        vilkårsresultatRepository.lagre(kontekst.behandlingId, vilkårsresultat)

        return Fullført
    }

    private fun utledFaktagrunnlag(kontekst: FlytKontekstMedPerioder) =
        SamordningAnnenFullYtelseFaktagrunnlag(
            rettighetsperiode = kontekst.rettighetsperiode,
            samordningTidslinje = samordningService.tidslinje(kontekst.behandlingId),
            samordningGrunnlag = SamordningYtelseVurderingGrunnlag(
                ytelseGrunnlag = samordningService.hentYtelser(kontekst.behandlingId),
                vurderingGrunnlag = samordningService.hentVurderinger(kontekst.behandlingId),
            ),
            uføreRegisterGrunnlag = uføreRepository.hentHvisEksisterer(kontekst.behandlingId),
            uføreVurderingGrunnlag = samordningUføreRepository.hentHvisEksisterer(kontekst.behandlingId),
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