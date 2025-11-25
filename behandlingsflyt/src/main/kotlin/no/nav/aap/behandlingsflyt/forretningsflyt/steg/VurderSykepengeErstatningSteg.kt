package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovService
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderingerImpl
import no.nav.aap.behandlingsflyt.behandling.vilkår.sykdom.SykepengeerstatningVilkår
import no.nav.aap.behandlingsflyt.behandling.vilkår.sykdom.SykepengerErstatningFaktagrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.BistandRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykepengerErstatningRepository
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.orEmpty
import no.nav.aap.lookup.repository.RepositoryProvider

class VurderSykepengeErstatningSteg private constructor(
    private val vilkårsresultatRepository: VilkårsresultatRepository,
    private val sykepengerErstatningRepository: SykepengerErstatningRepository,
    private val sykdomRepository: SykdomRepository,
    private val bistandRepository: BistandRepository,
    private val behandlingRepository: BehandlingRepository,
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
    private val tidligereVurderinger: TidligereVurderinger,
    private val avklaringsbehovService: AvklaringsbehovService
) : BehandlingSteg {
    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        vilkårsresultatRepository = repositoryProvider.provide(),
        sykepengerErstatningRepository = repositoryProvider.provide(),
        sykdomRepository = repositoryProvider.provide(),
        bistandRepository = repositoryProvider.provide(),
        behandlingRepository = repositoryProvider.provide(),
        avklaringsbehovRepository = repositoryProvider.provide(),
        tidligereVurderinger = TidligereVurderingerImpl(repositoryProvider),
        avklaringsbehovService = AvklaringsbehovService(repositoryProvider)
    )

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)

        avklaringsbehovService.oppdaterAvklaringsbehovForPeriodisertYtelsesvilkårGammel(
            avklaringsbehovene = avklaringsbehovene,
            behandlingRepository = behandlingRepository,
            vilkårsresultatRepository = vilkårsresultatRepository,
            definisjon = Definisjon.AVKLAR_SYKEPENGEERSTATNING,
            tvingerAvklaringsbehov = setOf(),
            nårVurderingErRelevant = ::perioderMedVurderingsbehov,
            kontekst = kontekst,
            perioderSomIkkeErTilstrekkeligVurdert = { emptySet() }, // Denne må minst sjekke at man har vurderinger for perioder når vurdering er relevant
            tilbakestillGrunnlag = {
                val vedtatteVurderinger =
                    kontekst.forrigeBehandlingId?.let { sykepengerErstatningRepository.hentHvisEksisterer(it) }
                        ?.vurderinger.orEmpty()

                val aktiveVurderinger =
                    sykepengerErstatningRepository.hentHvisEksisterer(kontekst.behandlingId)
                        ?.vurderinger.orEmpty()

                if (vedtatteVurderinger.toSet() != aktiveVurderinger.toSet()) {
                    sykepengerErstatningRepository.lagre(kontekst.behandlingId, vedtatteVurderinger)
                }
            }
        )

        when (kontekst.vurderingType) {
            VurderingType.FØRSTEGANGSBEHANDLING, VurderingType.REVURDERING -> {
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
            VurderingType.EFFEKTUER_AKTIVITETSPLIKT,
            VurderingType.EFFEKTUER_AKTIVITETSPLIKT_11_9,
            VurderingType.IKKE_RELEVANT -> {
                /* noop */
            }
        }

        return Fullført
    }

    private fun perioderMedVurderingsbehov(kontekst: FlytKontekstMedPerioder): Tidslinje<Boolean> {
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

        return Tidslinje.map4(
            tidligereVurderingsutfall,
            sykdomsvurderinger,
            bistandvurderinger,
            yrkesskadevurderinger
        ) { behandlingsutfall, sykdomsvurdering, bistandvurdering, yrkesskadevurdering ->
            // TODO: implementer overload for map4 som også gir perioden man vurderer
            when (behandlingsutfall) {
                null -> false
                TidligereVurderinger.Behandlingsutfall.IKKE_BEHANDLINGSGRUNNLAG -> false
                TidligereVurderinger.Behandlingsutfall.UUNGÅELIG_AVSLAG -> false
                TidligereVurderinger.Behandlingsutfall.UKJENT -> {
                    when {
                        /* ikke nødvendig å vurdere SPE fordi medlemmet oppfyller vilkårene for ordinær aap */
                        // TODO: må sjekke kravdato opp mot perioden på segmentet vi er i, ikke opp mot fom til vurderingen
                        sykdomsvurdering?.erOppfylt(kravDato) == true && bistandvurdering?.erBehovForBistand() != true ->
                            /* TODO: Her burde vi også se på 11-17 og 11-18 før vi sier ja */
                            true

                        /* i dette caset oppfyller ikke medlemmet vilkåret for ordinær AAP, men SPE er mulig */
                        sykdomsvurdering?.erOppfyltOrdinærtEllerMedYrkesskadeMenIkkeVissVarighet(kravDato, yrkesskadevurdering) == true ->
                            true

                        else -> false
                    }
                }
            }
        }
    }

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
