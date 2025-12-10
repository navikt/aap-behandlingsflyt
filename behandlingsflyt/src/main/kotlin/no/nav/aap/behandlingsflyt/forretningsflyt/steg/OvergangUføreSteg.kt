package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovService
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderingerImpl
import no.nav.aap.behandlingsflyt.behandling.vilkår.overganguføre.OvergangUføreFaktagrunnlag
import no.nav.aap.behandlingsflyt.behandling.vilkår.overganguføre.OvergangUføreVilkår
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.BistandRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.Bistandsvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangufore.OvergangUføreRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangufore.OvergangUføreVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.Sykdomsvurdering
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Status
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.orEmpty
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.lookup.repository.RepositoryProvider
import java.time.LocalDate

class OvergangUføreSteg private constructor(
    private val vilkårsresultatRepository: VilkårsresultatRepository,
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
    private val sykdomRepository: SykdomRepository,
    private val overgangUføreRepository: OvergangUføreRepository,
    private val tidligereVurderinger: TidligereVurderinger,
    private val bistandRepository: BistandRepository,
    private val behandlingRepository: BehandlingRepository,
    private val avklaringsbehovService: AvklaringsbehovService,
) : BehandlingSteg {
    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        vilkårsresultatRepository = repositoryProvider.provide(),
        avklaringsbehovRepository = repositoryProvider.provide(),
        overgangUføreRepository = repositoryProvider.provide(),
        sykdomRepository = repositoryProvider.provide(),
        tidligereVurderinger = TidligereVurderingerImpl(repositoryProvider),
        bistandRepository = repositoryProvider.provide(),
        behandlingRepository = repositoryProvider.provide(),
        avklaringsbehovService = AvklaringsbehovService(repositoryProvider),
    )

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)
        avklaringsbehovService.oppdaterAvklaringsbehovForPeriodisertYtelsesvilkårTilstrekkeligVurdert(
            behandlingRepository = behandlingRepository,
            vilkårsresultatRepository = vilkårsresultatRepository,
            kontekst = kontekst,
            avklaringsbehovene = avklaringsbehovene,
            definisjon = Definisjon.AVKLAR_OVERGANG_UFORE,
            tvingerAvklaringsbehov = setOf(
                Vurderingsbehov.SYKDOM_ARBEVNE_BEHOV_FOR_BISTAND,
                Vurderingsbehov.MOTTATT_SØKNAD
            ),
            nårVurderingErRelevant = { perioderOvergangUføreErRelevant(kontekst) },
            perioderSomIkkeErTilstrekkeligVurdert = { perioderSomIkkeErTilstrekkeligVurdert(kontekst) },
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

        val vilkårsresultat = vilkårsresultatRepository.hent(kontekst.behandlingId)
        if (avklaringsbehovene.hentBehovForDefinisjon(Definisjon.AVKLAR_OVERGANG_UFORE)
                ?.status() in listOf(Status.AVSLUTTET, Status.AVBRUTT)
        ) {
            val grunnlag = OvergangUføreFaktagrunnlag(
                rettighetsperiode = kontekst.rettighetsperiode,
                overgangUføreGrunnlag = overgangUføreRepository.hentHvisEksisterer(kontekst.behandlingId),
            )
            OvergangUføreVilkår(vilkårsresultat).vurder(grunnlag = grunnlag)
        } else {
            vilkårsresultat.leggTilHvisIkkeEksisterer(Vilkårtype.OVERGANGUFØREVILKÅRET)
        }
        vilkårsresultatRepository.lagre(kontekst.behandlingId, vilkårsresultat)

        return Fullført
    }

    private fun perioderOvergangUføreErRelevant(kontekst: FlytKontekstMedPerioder): Tidslinje<Boolean> {
        val utfall = tidligereVurderinger.behandlingsutfall(kontekst, type())
        val sykdomsvurderinger =
            sykdomRepository.hentHvisEksisterer(kontekst.behandlingId)?.somSykdomsvurderingstidslinje().orEmpty()
        val bistandsvurderinger =
            bistandRepository.hentHvisEksisterer(kontekst.behandlingId)?.somBistandsvurderingstidslinje().orEmpty()

        return Tidslinje.map3(
            utfall,
            sykdomsvurderinger,
            bistandsvurderinger
        ) { segmentPeriode, utfall, sykdomsvurdering, bistandsvurdering ->
            when (utfall) {
                null -> false
                TidligereVurderinger.Behandlingsutfall.IKKE_BEHANDLINGSGRUNNLAG -> false
                TidligereVurderinger.Behandlingsutfall.UUNGÅELIG_AVSLAG -> false
                TidligereVurderinger.Behandlingsutfall.UKJENT -> {
                    sykdomErOppfyltOgBistandErIkkeOppfylt(
                        kontekst.rettighetsperiode.fom, segmentPeriode, sykdomsvurdering, bistandsvurdering
                    )
                }
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
        val sykdomsdtidslinje =
            sykdomRepository.hentHvisEksisterer(kontekst.behandlingId)?.somSykdomsvurderingstidslinje().orEmpty()
        val bistandstidslinje =
            bistandRepository.hentHvisEksisterer(kontekst.behandlingId)?.somBistandsvurderingstidslinje().orEmpty()

        val nårVurderingErKonsistent = nårVurderingErKonsistentMedSykdomOgBistand(
            overgangUføreTidslinje, sykdomsdtidslinje, bistandstidslinje, kontekst.rettighetsperiode.fom
        )

        val nårPåkrevdVurderingMangler =
            perioderOvergangUføreErRelevant(kontekst).leftJoin(overgangUføreTidslinje) { erRelevant, overgangUføreVurdering ->
                erRelevant && overgangUføreVurdering == null
            }

        return Tidslinje.map2(nårPåkrevdVurderingMangler, nårVurderingErKonsistent) { vurderingMangler, erKonsistent ->
            vurderingMangler == true || erKonsistent == false
        }.komprimer().filter { erUtilstrekkelig -> erUtilstrekkelig.verdi }.perioder().toSet()
    }

    private fun nårVurderingErKonsistentMedSykdomOgBistand(
        overgangUføreTidslinje: Tidslinje<OvergangUføreVurdering>,
        sykdomstidslinje: Tidslinje<Sykdomsvurdering>,
        bistandstidslinje: Tidslinje<Bistandsvurdering>,
        kravdato: LocalDate
    ): Tidslinje<Boolean> {
        return Tidslinje.map3(
            overgangUføreTidslinje, sykdomstidslinje, bistandstidslinje
        ) { segmentPeriode, overgangUføreVurdering, sykdomsvurdering, bistandsvurdering ->
            overgangUføreVurdering == null 
                    || Periode(kravdato.minusMonths(8), kravdato).inneholder(segmentPeriode) // Det er tillatt å vurdere 11-18 før kravdato
                    || overgangUføreVurdering.brukerRettPåAAP == false // Nei-vurdering er uavhengig av bistand og sykdom
                    || sykdomErOppfyltOgBistandErIkkeOppfylt(kravdato, segmentPeriode, sykdomsvurdering, bistandsvurdering)
        }.komprimer()
    }

    private fun sykdomErOppfyltOgBistandErIkkeOppfylt(
        kravdato: LocalDate,
        segmentPeriode: Periode,
        sykdomsvurdering: Sykdomsvurdering?,
        bistandsvurdering: Bistandsvurdering?
    ): Boolean {
        return sykdomsvurdering?.erOppfyltOrdinær(
            kravdato, segmentPeriode
        ) == true && bistandsvurdering != null && !bistandsvurdering.erBehovForBistand()
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
