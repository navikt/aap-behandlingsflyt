package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.samordning.SamordningService
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderingerImpl
import no.nav.aap.behandlingsflyt.behandling.vilkår.samordning.annenfullytelse.SamordningAnnenFullYtelseFaktagrunnlag
import no.nav.aap.behandlingsflyt.behandling.vilkår.samordning.annenfullytelse.SamordningAnnenFullYtelseVilkår
import no.nav.aap.behandlingsflyt.faktagrunnlag.Faktagrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.SamordningYtelseVurderingGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.uførevurdering.SamordningUføreGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Avslagsårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre.UføreGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre.UføreService
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.verdityper.Prosent.Companion.`100_PROSENT`
import no.nav.aap.lookup.repository.RepositoryProvider

@Suppress("unused")
class SamordningAvslagGrunnlag(
    val samordningGrunnlag: SamordningYtelseVurderingGrunnlag?,
    val uføreRegisterGrunnlag: UføreGrunnlag?,
    val uføreVurderingGrunnlag: SamordningUføreGrunnlag?,
) : Faktagrunnlag

class SamordningAvslagSteg(
    private val samordningService: SamordningService,
    private val uføreService: UføreService,
    private val vilkårsresultatRepository: VilkårsresultatRepository,
    private val tidligereVurderinger: TidligereVurderinger,
) : BehandlingSteg {
    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        samordningService = SamordningService(repositoryProvider),
        uføreService = UføreService(repositoryProvider.provide(), repositoryProvider.provide()),
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

        if (tidligereVurderinger.girAvslag(kontekst, type())) {
            vilkårsresultat.leggTilHvisIkkeEksisterer(Vilkårtype.SAMORDNING).leggTilVurdering(
                Vilkårsperiode(
                    kontekst.rettighetsperiode,
                    Vilkårsvurdering(
                        utfall = Utfall.IKKE_VURDERT,
                        manuellVurdering = false,
                        begrunnelse = "Avslag tidligere i flyt",
                        faktagrunnlag = null,
                    )
                )
            )
            return Fullført
        }

        val grunnlag = SamordningAnnenFullYtelseFaktagrunnlag(
            rettighetsperiode = kontekst.rettighetsperiode,
            samordningTidslinje = samordningService.tidslinje(kontekst.behandlingId),
            uføreTidslinje = uføreService.tidslinje(kontekst.behandlingId),
            samordningAvslagGrunnlag = utledFaktagrunnlag(kontekst)
        )

        SamordningAnnenFullYtelseVilkår(vilkårsresultat).vurder(grunnlag)
        vilkårsresultatRepository.lagre(kontekst.behandlingId, vilkårsresultat)
        return Fullført
    }

    private fun utledFaktagrunnlag(kontekst: FlytKontekstMedPerioder) =
        SamordningAvslagGrunnlag(
            samordningGrunnlag = SamordningYtelseVurderingGrunnlag(
                ytelseGrunnlag = samordningService.hentYtelser(kontekst.behandlingId),
                vurderingGrunnlag = samordningService.hentVurderinger(kontekst.behandlingId),
            ),
            uføreRegisterGrunnlag = uføreService.hentRegisterGrunnlagHvisEksisterer(kontekst.behandlingId),
            uføreVurderingGrunnlag = uføreService.hentVurderingGrunnlagHvisEksisterer(kontekst.behandlingId),
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