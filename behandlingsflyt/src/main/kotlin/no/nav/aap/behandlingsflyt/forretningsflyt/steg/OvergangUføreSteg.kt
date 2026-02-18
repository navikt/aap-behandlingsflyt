package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovMetadataUtleder
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovService
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderingerImpl
import no.nav.aap.behandlingsflyt.behandling.vilkår.overganguføre.OvergangUføreFaktagrunnlag
import no.nav.aap.behandlingsflyt.behandling.vilkår.overganguføre.OvergangUføreVilkår
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.BistandRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangufore.OvergangUføreRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangufore.OvergangUføreValidering.nårVurderingErKonsistentMedSykdomOgBistand
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomRepository
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
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.lookup.repository.RepositoryProvider

class OvergangUføreSteg private constructor(
    private val vilkårsresultatRepository: VilkårsresultatRepository,
    private val sykdomRepository: SykdomRepository,
    private val overgangUføreRepository: OvergangUføreRepository,
    private val tidligereVurderinger: TidligereVurderinger,
    private val bistandRepository: BistandRepository,
    private val avklaringsbehovService: AvklaringsbehovService,
) : BehandlingSteg, AvklaringsbehovMetadataUtleder {
    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        vilkårsresultatRepository = repositoryProvider.provide(),
        overgangUføreRepository = repositoryProvider.provide(),
        sykdomRepository = repositoryProvider.provide(),
        tidligereVurderinger = TidligereVurderingerImpl(repositoryProvider),
        bistandRepository = repositoryProvider.provide(),
        avklaringsbehovService = AvklaringsbehovService(repositoryProvider),
    )

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        val perioderSomIkkeErTilstrekkeligVurdert: () -> Set<Periode> =
            { perioderSomIkkeErTilstrekkeligVurdert(kontekst) }

        avklaringsbehovService.oppdaterAvklaringsbehovForPeriodisertYtelsesvilkårTilstrekkeligVurdert(
            kontekst = kontekst,
            definisjon = Definisjon.AVKLAR_OVERGANG_UFORE,
            tvingerAvklaringsbehov = setOf(
                Vurderingsbehov.SYKDOM_ARBEVNE_BEHOV_FOR_BISTAND,
            ),
            nårVurderingErRelevant = ::nårVurderingErRelevant,
            perioderSomIkkeErTilstrekkeligVurdert = perioderSomIkkeErTilstrekkeligVurdert,
            tilbakestillGrunnlag = {
                val vedtatteVurderinger =
                    kontekst.forrigeBehandlingId?.let { overgangUføreRepository.hentHvisEksisterer(it) }?.vurderinger.orEmpty()
                val aktiveVurderinger =
                    overgangUføreRepository.hentHvisEksisterer(kontekst.behandlingId)?.vurderinger.orEmpty()
                if (vedtatteVurderinger.toSet() != aktiveVurderinger.toSet()) {
                    overgangUføreRepository.lagre(kontekst.behandlingId, vedtatteVurderinger)
                }
            },
        )

        when (kontekst.vurderingType) {
            VurderingType.FØRSTEGANGSBEHANDLING, VurderingType.REVURDERING -> {
                val vilkårsresultat = vilkårsresultatRepository.hent(kontekst.behandlingId)
                vilkårsresultat.leggTilHvisIkkeEksisterer(Vilkårtype.OVERGANGUFØREVILKÅRET)
                val grunnlag = OvergangUføreFaktagrunnlag(
                    rettighetsperiode = kontekst.rettighetsperiode,
                    overgangUføreGrunnlag = overgangUføreRepository.hentHvisEksisterer(kontekst.behandlingId),
                )
                OvergangUføreVilkår(vilkårsresultat).vurder(grunnlag = grunnlag)
                vilkårsresultatRepository.lagre(kontekst.behandlingId, vilkårsresultat)
            }

            else -> {} // Do nothing
        }

        return Fullført
    }

    override fun nårVurderingErRelevant(kontekst: FlytKontekstMedPerioder): Tidslinje<Boolean> {
        val utfall = tidligereVurderinger.behandlingsutfall(kontekst, type())
        val sykdomsvurderinger =
            sykdomRepository.hentHvisEksisterer(kontekst.behandlingId)?.somSykdomsvurderingstidslinje().orEmpty()
 
        return Tidslinje.map2(
            utfall,
            sykdomsvurderinger
        ) { segmentPeriode, utfall, sykdomsvurering ->
            when (utfall) {
                TidligereVurderinger.IkkeBehandlingsgrunnlag, TidligereVurderinger.UunngåeligAvslag -> false
                is TidligereVurderinger.PotensieltOppfylt -> {
                    val erSykdomOppfyltOrdinærEllerPotensieltYrkesskade =
                        sykdomsvurering?.erOppfyltForYrkesskadeSettBortIfraÅrsakssammenheng(
                            kontekst.rettighetsperiode.fom,
                            segmentPeriode
                        ) == true || sykdomsvurering?.erOppfyltOrdinær(
                            kontekst.rettighetsperiode.fom,
                            segmentPeriode
                        ) == true

                   erSykdomOppfyltOrdinærEllerPotensieltYrkesskade && utfall.rettighetstype == null
                }
                else -> false
            }
        }
    }

    /**
     * 1. Det må finnes en vurdering for alle relevante perioder
     *      Selv om man har samordning i starten av perioden så skal ikke 8-mnd perioden endres - skal derfor ha en vurdering i alle perioder det kan være en vurdering
     * 2. Ingen vurderinger med oppfylt 11-18 utenfor perioden der 11-5 er oppfylt og 11-6 ikke er oppfylt
     *      Kan innvilge 11-18 før kravdato
     */
    private fun perioderSomIkkeErTilstrekkeligVurdert(kontekst: FlytKontekstMedPerioder): Set<Periode> {
        val overgangUføreTidslinje = overgangUføreRepository.hentHvisEksisterer(kontekst.behandlingId)
            ?.somOvergangUforevurderingstidslinje().orEmpty()
        val sykdomstidslinje =
            sykdomRepository.hentHvisEksisterer(kontekst.behandlingId)?.somSykdomsvurderingstidslinje().orEmpty()
        val bistandstidslinje =
            bistandRepository.hentHvisEksisterer(kontekst.behandlingId)?.somBistandsvurderingstidslinje().orEmpty()

        val nårVurderingErKonsistent = nårVurderingErKonsistentMedSykdomOgBistand(
            overgangUføreTidslinje, sykdomstidslinje, bistandstidslinje, kontekst.rettighetsperiode.fom
        )

        val nårPåkrevdVurderingMangler =
            nårVurderingErRelevant(kontekst).leftJoin(overgangUføreTidslinje) { erRelevant, overgangUføreVurdering ->
                erRelevant && overgangUføreVurdering == null
            }

        return Tidslinje.map2(nårPåkrevdVurderingMangler, nårVurderingErKonsistent) { vurderingMangler, erKonsistent ->
            vurderingMangler == true || erKonsistent == false
        }.komprimer().filter { erUtilstrekkelig -> erUtilstrekkelig.verdi }.perioder().toSet()
    }

    companion object : FlytSteg {
        override fun konstruer(
            repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider
        ): BehandlingSteg {
            return OvergangUføreSteg(repositoryProvider, gatewayProvider)
        }

        override fun type(): StegType {
            return StegType.OVERGANG_UFORE
        }
    }
}
