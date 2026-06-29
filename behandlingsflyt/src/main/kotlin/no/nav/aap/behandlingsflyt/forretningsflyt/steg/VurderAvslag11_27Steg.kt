package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovService
import no.nav.aap.behandlingsflyt.behandling.avslag11_27.Avslag11_27Repository
import no.nav.aap.behandlingsflyt.behandling.samordning.SamordningService
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderingerImpl
import no.nav.aap.behandlingsflyt.behandling.vilkår.samordning.annenfullytelse.SamordningAnnenFullYtelseFaktagrunnlag
import no.nav.aap.behandlingsflyt.behandling.vilkår.samordning.annenfullytelse.SamordningAnnenFullYtelseVilkår
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.SamordningYtelseVurderingGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.uførevurdering.SamordningUføreRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Avslagsårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre.UføreRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.KravGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.KravRepository
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider

class VurderAvslag11_27Steg(
    private val samordningService: SamordningService,
    private val uføreRepository: UføreRepository,
    private val samordningUføreRepository: SamordningUføreRepository,
    private val avslag11_27repository: Avslag11_27Repository,
    private val kravRepository: KravRepository,
    private val avklaringsbehovService: AvklaringsbehovService,
    private val tidligereVurderinger: TidligereVurderinger,
    private val vilkårsresultatRepository: VilkårsresultatRepository,
    private val unleashGateway: UnleashGateway,
) : BehandlingSteg {

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        avklaringsbehovService.oppdaterAvklaringsbehov(
            kontekst = kontekst,
            definisjon = Definisjon.VURDER_AVSLAG_11_27,
            vedtakBehøverVurdering = { vedtakBehøverVurdering(kontekst) },
            erTilstrekkeligVurdert = {
                avslag11_27repository.hentHvisEksisterer(kontekst.behandlingId) != null
            },
            tilbakestillGrunnlag = {
                avslag11_27repository.tilbakestillGrunnlag(
                    kontekst.behandlingId,
                    kontekst.forrigeBehandlingId
                )
            },
        )

        settVilkårsresultat(kontekst)

        return Fullført
    }

    private fun settVilkårsresultat(kontekst: FlytKontekstMedPerioder) {

        val vilkårsresultat = vilkårsresultatRepository.hent(kontekst.behandlingId)
        val grunnlag = utledFaktagrunnlag(kontekst)
        SamordningAnnenFullYtelseVilkår(vilkårsresultat).vurder(grunnlag)

        if (unleashGateway.isEnabled(BehandlingsflytFeature.Avslag11_27)) {
            vilkårsresultatRepository.lagre(kontekst.behandlingId, vilkårsresultat)
        }
    }

    private fun vedtakBehøverVurdering(kontekst: FlytKontekstMedPerioder): Boolean {
        return when (kontekst.vurderingType) {
            VurderingType.FØRSTEGANGSBEHANDLING,
            VurderingType.REVURDERING ->
                (tidligereVurderinger.muligMedRettTilAAP(kontekst, type())
                        && Vurderingsbehov.VURDER_AVSLAG_11_27 in kontekst.vurderingsbehovRelevanteForSteg
                        && kontekst.vurderingsbehovRelevanteForSteg.isNotEmpty())

            VurderingType.MELDEKORT,
            VurderingType.AUTOMATISK_BREV,
            VurderingType.UTVID_VEDTAKSLENGDE,
            VurderingType.MIGRER_RETTIGHETSPERIODE,
            VurderingType.EFFEKTUER_AKTIVITETSPLIKT,
            VurderingType.EFFEKTUER_AKTIVITETSPLIKT_11_9,
            VurderingType.G_REGULERING,
            VurderingType.OVERGANG_UFORE_STANS,
            VurderingType.IKKE_RELEVANT ->
                false
        }
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
            avslag1127grunnlag = avslag11_27repository.hentHvisEksisterer(kontekst.behandlingId),
            kravGrunnlag = kravRepository.hentHvisEksisterer(kontekst.behandlingId)
        )

    companion object : FlytSteg {
        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): BehandlingSteg {
            return VurderAvslag11_27Steg(
                samordningService = SamordningService(repositoryProvider),
                uføreRepository = repositoryProvider.provide(),
                samordningUføreRepository = repositoryProvider.provide(),
                avslag11_27repository = repositoryProvider.provide(),
                kravRepository = repositoryProvider.provide(),
                avklaringsbehovService = AvklaringsbehovService(repositoryProvider, gatewayProvider),
                tidligereVurderinger = TidligereVurderingerImpl(repositoryProvider, gatewayProvider),
                vilkårsresultatRepository = repositoryProvider.provide(),
                unleashGateway = gatewayProvider.provide()
            )
        }

        override fun type(): StegType {
            return StegType.VURDER_AVSLAG_11_27
        }
    }
}
