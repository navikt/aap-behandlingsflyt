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
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangufore.OvergangUføreRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomRepository
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Status
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.orEmpty
import no.nav.aap.lookup.repository.RepositoryProvider

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
        avklaringsbehovService.oppdaterAvklaringsbehov(
            kontekst = kontekst,
            avklaringsbehovene = avklaringsbehovene,
            definisjon = Definisjon.AVKLAR_OVERGANG_UFORE,
            vedtakBehøverVurdering = { vedtakBehøverVurdering(kontekst) },
            erTilstrekkeligVurdert = { true },
            tilbakestillGrunnlag = {
                val vedtatteVurderinger = kontekst.forrigeBehandlingId
                    ?.let { overgangUføreRepository.hentHvisEksisterer(it) }
                    ?.vurderinger.orEmpty()
                val aktiveVurderinger = overgangUføreRepository.hentHvisEksisterer(kontekst.behandlingId)
                    ?.vurderinger.orEmpty()
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
                vurderinger = overgangUføreRepository.hentHvisEksisterer(kontekst.behandlingId)?.vurderinger.orEmpty(),
            )
            OvergangUføreVilkår(vilkårsresultat).vurder(grunnlag = grunnlag)
        } else {
            vilkårsresultat.leggTilHvisIkkeEksisterer(Vilkårtype.OVERGANGUFØREVILKÅRET)
        }
        vilkårsresultatRepository.lagre(kontekst.behandlingId, vilkårsresultat)

        return Fullført
    }

    private fun vedtakBehøverVurdering(kontekst: FlytKontekstMedPerioder): Boolean {
        return when (kontekst.vurderingType) {
            VurderingType.FØRSTEGANGSBEHANDLING, VurderingType.REVURDERING -> {
                val perioderOvergangUføreErRelevant = perioderMedVurderingsbehov(kontekst)

                if (perioderOvergangUføreErRelevant.segmenter().any { it.verdi } && vurderingsbehovTvingerVurdering(kontekst)) {
                    return true
                }

                val perioderOvergangUføreErVurdert = kontekst.forrigeBehandlingId?.let { forrigeBehandlingId ->
                    val forrigeBehandling = behandlingRepository.hent(forrigeBehandlingId)
                    val forrigeRettighetsperiode =
                        /* Lagrer vi ned rettighetsperioden som ble brukt for en behandling noe sted? */
                        vilkårsresultatRepository.hent(forrigeBehandlingId).finnVilkår(Vilkårtype.ALDERSVILKÅRET)
                            .tidslinje().helePerioden()

                    perioderMedVurderingsbehov(
                        kontekst.copy(
                            /* TODO: hacky. Er faktisk bare behandlingId som brukes av sjekkene. */
                            behandlingId = forrigeBehandlingId,
                            forrigeBehandlingId = forrigeBehandling.forrigeBehandlingId,
                            rettighetsperiode = forrigeRettighetsperiode,
                            behandlingType = forrigeBehandling.typeBehandling(),
                        )
                    )
                }.orEmpty()

                perioderOvergangUføreErRelevant.leftJoin(perioderOvergangUføreErVurdert) { erRelevant, erVurdert ->
                    erRelevant && erVurdert != true
                }.segmenter().any { it.verdi }
            }

            VurderingType.MELDEKORT -> false
            VurderingType.EFFEKTUER_AKTIVITETSPLIKT -> false
            VurderingType.EFFEKTUER_AKTIVITETSPLIKT_11_9 -> false
            VurderingType.IKKE_RELEVANT -> false
        }
    }

    private fun vurderingsbehovTvingerVurdering(kontekst: FlytKontekstMedPerioder): Boolean {
        return kontekst.vurderingsbehovRelevanteForSteg.any {
            it in listOf(Vurderingsbehov.SYKDOM_ARBEVNE_BEHOV_FOR_BISTAND, Vurderingsbehov.MOTTATT_SØKNAD)
        }
    }

    private fun perioderMedVurderingsbehov(kontekst: FlytKontekstMedPerioder): Tidslinje<Boolean> {
        val utfall = tidligereVurderinger.behandlingsutfall(kontekst, type())
        val sykdomsvurderinger = sykdomRepository.hentHvisEksisterer(kontekst.behandlingId)
            ?.somSykdomsvurderingstidslinje().orEmpty()
        val bistandsvurderinger = bistandRepository.hentHvisEksisterer(kontekst.behandlingId)
            ?.somBistandsvurderingstidslinje().orEmpty()

        return Tidslinje.zip3(utfall, sykdomsvurderinger, bistandsvurderinger)
            .mapValue { (utfall, sykdomsvurdering, bistandsvurdering) ->
                when (utfall) {
                    null -> false
                    TidligereVurderinger.Behandlingsutfall.IKKE_BEHANDLINGSGRUNNLAG -> false
                    TidligereVurderinger.Behandlingsutfall.UUNGÅELIG_AVSLAG -> false
                    TidligereVurderinger.Behandlingsutfall.UKJENT -> {
                        sykdomsvurdering?.erOppfyltOrdinær(kontekst.rettighetsperiode.fom) == true && bistandsvurdering != null && !bistandsvurdering.erBehovForBistand()
                    }
                }
            }
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
