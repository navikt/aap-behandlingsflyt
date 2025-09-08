package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.samordning.SamordningService
import no.nav.aap.behandlingsflyt.behandling.samordning.Ytelse
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderingerImpl
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
    constructor(repositoryProvider: RepositoryProvider) : this(
        samordningService = SamordningService(repositoryProvider),
        uføreService = UføreService(repositoryProvider.provide(), repositoryProvider.provide()),
        vilkårsresultatRepository = repositoryProvider.provide(),
        tidligereVurderinger = TidligereVurderingerImpl(repositoryProvider),
    )

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        if (kontekst.vurderingType == VurderingType.FØRSTEGANGSBEHANDLING) {
            if (tidligereVurderinger.girIngenBehandlingsgrunnlag(kontekst, type())) {
                return Fullført
            }
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

        val rettighetsperiode = kontekst.rettighetsperiode

        val samordningTidslinje = samordningService.tidslinje(kontekst.behandlingId)
        val samordningUføreTidslinje = uføreService.tidslinje(kontekst.behandlingId)

        /* NB: bevisst valg å ikke gi avslag selv om summen av samordninger blir til 100%. */
        val samordningsVurderinger =
            samordningTidslinje.outerJoinNotNull(samordningUføreTidslinje) { andreYtelserSamordning, samordningUføreGradering ->
                val samordningerYtelser =
                    andreYtelserSamordning?.ytelsesGraderinger.orEmpty()
                        .filter { it.ytelse != Ytelse.UKJENT_SLUTTDATO_PÅ_YTELSE }
                        .map { it.ytelse.toString() to it.gradering }
                val samordningUføre = listOfNotNull(samordningUføreGradering?.let { "UFØRE" to it })
                val samordninger = (samordningerYtelser + samordningUføre)
                    .filter { (_, prosent) -> prosent == `100_PROSENT` }

                if (samordninger.isEmpty())
                    Vilkårsvurdering(
                        utfall = Utfall.IKKE_VURDERT,
                        manuellVurdering = false,
                        begrunnelse = "Ikke full ytelse av samordninger",
                        avslagsårsak = null,
                        faktagrunnlag = utledFaktagrunnlag(kontekst),
                    )
                else
                    Vilkårsvurdering(
                        utfall = Utfall.IKKE_OPPFYLT,
                        manuellVurdering = false,
                        begrunnelse = "Full ytelse ${samordninger.joinToString { (navn, _) -> navn }}",
                        avslagsårsak = Avslagsårsak.ANNEN_FULL_YTELSE,
                        faktagrunnlag = utledFaktagrunnlag(kontekst),
                    )
            }


        val vilkår = vilkårsresultat.leggTilHvisIkkeEksisterer(Vilkårtype.SAMORDNING).nullstillTidslinje()
        vilkår.leggTilVurderinger(samordningsVurderinger.begrensetTil(rettighetsperiode))
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
            return SamordningAvslagSteg(repositoryProvider)
        }

        override fun type(): StegType {
            return StegType.SAMORDNING_AVSLAG
        }
    }
}