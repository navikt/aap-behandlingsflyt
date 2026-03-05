package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovMetadataUtleder
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovService
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderingerImpl
import no.nav.aap.behandlingsflyt.behandling.vilkår.sykdom.SykepengeerstatningVilkår
import no.nav.aap.behandlingsflyt.behandling.vilkår.sykdom.SykepengerErstatningFaktagrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.BistandRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykepengerErstatningRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.somTidslinje
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.orEmpty
import no.nav.aap.lookup.repository.RepositoryProvider

class VurderSykepengeErstatningSteg private constructor(
    private val vilkårsresultatRepository: VilkårsresultatRepository,
    private val sykepengerErstatningRepository: SykepengerErstatningRepository,
    private val sykdomRepository: SykdomRepository,
    private val bistandRepository: BistandRepository,
    private val tidligereVurderinger: TidligereVurderinger,
    private val avklaringsbehovService: AvklaringsbehovService,
) : BehandlingSteg, AvklaringsbehovMetadataUtleder {
    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        vilkårsresultatRepository = repositoryProvider.provide(),
        sykepengerErstatningRepository = repositoryProvider.provide(),
        sykdomRepository = repositoryProvider.provide(),
        bistandRepository = repositoryProvider.provide(),
        tidligereVurderinger = TidligereVurderingerImpl(repositoryProvider, gatewayProvider),
        avklaringsbehovService = AvklaringsbehovService(repositoryProvider),
    )

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        val vedtatteVurderinger =
            kontekst.forrigeBehandlingId?.let { sykepengerErstatningRepository.hentHvisEksisterer(it) }
                ?.vurderinger.orEmpty()

        val aktiveVurderinger =
            sykepengerErstatningRepository.hentHvisEksisterer(kontekst.behandlingId)
                ?.vurderinger.orEmpty()

        avklaringsbehovService.oppdaterAvklaringsbehovForPeriodisertYtelsesvilkår(
            definisjon = Definisjon.AVKLAR_SYKEPENGEERSTATNING,
            tvingerAvklaringsbehov = setOf(Vurderingsbehov.REVURDER_SYKEPENGEERSTATNING),
            nårVurderingErRelevant = ::nårVurderingErRelevant,
            kontekst = kontekst,
            nårVurderingErGyldig = { aktiveVurderinger.somTidslinje().mapValue { true } },
            tilbakestillGrunnlag = {
                if (vedtatteVurderinger.toSet() != aktiveVurderinger.toSet()) {
                    sykepengerErstatningRepository.lagre(kontekst.behandlingId, vedtatteVurderinger)
                }
            },
        )

        when (kontekst.vurderingType) {
            VurderingType.FØRSTEGANGSBEHANDLING, VurderingType.REVURDERING, VurderingType.MIGRER_RETTIGHETSPERIODE -> {
                val vilkårsresultat = vilkårsresultatRepository.hent(kontekst.behandlingId)
                val grunnlag = SykepengerErstatningFaktagrunnlag(
                    rettighetsperiode = kontekst.rettighetsperiode,
                    sykdomGrunnlag = sykdomRepository.hentHvisEksisterer(kontekst.behandlingId),
                    sykepengeerstatningGrunnlag = sykepengerErstatningRepository.hentHvisEksisterer(kontekst.behandlingId)
                )
                SykepengeerstatningVilkår(vilkårsresultat).vurder(grunnlag = grunnlag)

                vilkårsresultatRepository.lagre(kontekst.behandlingId, vilkårsresultat)
            }

            VurderingType.MELDEKORT,
            VurderingType.UTVID_VEDTAKSLENGDE,
            VurderingType.AUTOMATISK_BREV,
            VurderingType.EFFEKTUER_AKTIVITETSPLIKT,
            VurderingType.EFFEKTUER_AKTIVITETSPLIKT_11_9,
            VurderingType.IKKE_RELEVANT -> {
                /* noop */
            }
        }

        return Fullført
    }

    override fun nårVurderingErRelevant(kontekst: FlytKontekstMedPerioder): Tidslinje<Boolean> {
        val tidligereVurderingsutfall = tidligereVurderinger.behandlingsutfall(kontekst, type())

        val kravDato = kontekst.rettighetsperiode.fom

        val sykdomGrunnlag = sykdomRepository.hentHvisEksisterer(kontekst.behandlingId)

        val sykdomsvurderinger = sykdomGrunnlag
            ?.somSykdomsvurderingstidslinje()
            .orEmpty()

        val bistandvurderinger =
            bistandRepository.hentHvisEksisterer(kontekst.behandlingId)?.somBistandsvurderingstidslinje()
                ?: Tidslinje.empty()

        val yrkesskadevurderinger = sykdomGrunnlag?.yrkesskadevurdringTidslinje(kontekst.rettighetsperiode).orEmpty()

        val vilkårsresultat = vilkårsresultatRepository.hent(kontekst.behandlingId)
        val overganguføreVilkår =
            vilkårsresultat.optionalVilkår(Vilkårtype.OVERGANGUFØREVILKÅRET)?.tidslinje().orEmpty()
        val overgangarbeidVilkår =
            vilkårsresultat.optionalVilkår(Vilkårtype.OVERGANGARBEIDVILKÅRET)?.tidslinje().orEmpty()

        return Tidslinje.map6(
            tidligereVurderingsutfall,
            sykdomsvurderinger,
            bistandvurderinger,
            yrkesskadevurderinger,
            overganguføreVilkår,
            overgangarbeidVilkår
        ) { segmentPeriode, behandlingsutfall, sykdomsvurdering, bistandvurdering, yrkesskadevurdering, overgangUføreVilkårsvurdering, overgangarbeidVilkår ->
            when (behandlingsutfall) {
                null -> false
                is TidligereVurderinger.IkkeBehandlingsgrunnlag -> false
                is TidligereVurderinger.UunngåeligAvslag -> false
                is TidligereVurderinger.PotensieltOppfylt -> {
                    when {
                        sykdomsvurdering?.erOppfyltOrdinær(kravDato, segmentPeriode) == true
                                && bistandvurdering?.erBehovForBistand() != true
                                && overgangUføreVilkårsvurdering?.utfall != Utfall.OPPFYLT
                                && overgangarbeidVilkår?.utfall != Utfall.OPPFYLT -> true

                        /* caset oppfyller ikke medlemmet vilkåret for ordinær AAP, men SPE er mulig */
                        sykdomsvurdering?.erOppfyltOrdinærtEllerMedYrkesskadeMenIkkeVissVarighet(yrkesskadevurdering) == true ->
                            true

                        else -> false
                    }
                }
            }
        }
    }

    override val stegType = type()

    companion object : FlytSteg {
        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): BehandlingSteg {
            return VurderSykepengeErstatningSteg(repositoryProvider, gatewayProvider)
        }

        override fun type(): StegType {
            return StegType.VURDER_SYKEPENGEERSTATNING
        }
    }
}
