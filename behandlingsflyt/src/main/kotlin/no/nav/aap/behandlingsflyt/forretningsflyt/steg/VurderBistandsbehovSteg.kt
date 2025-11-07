package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovService
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderingerImpl
import no.nav.aap.behandlingsflyt.behandling.vilkår.bistand.BistandFaktagrunnlag
import no.nav.aap.behandlingsflyt.behandling.vilkår.bistand.Bistandsvilkåret
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.BistandRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.StudentRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomRepository
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.orEmpty
import no.nav.aap.lookup.repository.RepositoryProvider
import org.slf4j.LoggerFactory

class VurderBistandsbehovSteg(
    private val bistandRepository: BistandRepository,
    private val studentRepository: StudentRepository,
    private val sykdomsRepository: SykdomRepository,
    private val vilkårsresultatRepository: VilkårsresultatRepository,
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
    private val tidligereVurderinger: TidligereVurderinger,
    private val behandlingRepository: BehandlingRepository,
    private val avklaringsbehovService: AvklaringsbehovService
) : BehandlingSteg {
    constructor(repositoryProvider: RepositoryProvider) : this(
        bistandRepository = repositoryProvider.provide(),
        studentRepository = repositoryProvider.provide(),
        sykdomsRepository = repositoryProvider.provide(),
        vilkårsresultatRepository = repositoryProvider.provide(),
        avklaringsbehovRepository = repositoryProvider.provide(),
        tidligereVurderinger = TidligereVurderingerImpl(repositoryProvider),
        behandlingRepository = repositoryProvider.provide(),
        avklaringsbehovService = AvklaringsbehovService(repositoryProvider)
    )

    private val log = LoggerFactory.getLogger(javaClass)

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)
        avklaringsbehovService.oppdaterAvklaringsbehov(
            avklaringsbehovene = avklaringsbehovene,
            definisjon = Definisjon.AVKLAR_BISTANDSBEHOV,
            vedtakBehøverVurdering = { vedtakBehøverVurdering(kontekst) },
            erTilstrekkeligVurdert = { erTilstrekkeligVurdert(kontekst) },
            tilbakestillGrunnlag = {
                val forrigeVurderinger = kontekst.forrigeBehandlingId
                    ?.let { bistandRepository.hentHvisEksisterer(it) }
                    ?.vurderinger
                    .orEmpty()

                val nåværendeVurderinger = bistandRepository.hentHvisEksisterer(kontekst.behandlingId)
                    ?.vurderinger
                    .orEmpty()

                if (forrigeVurderinger.toSet() != nåværendeVurderinger.toSet()) {
                    bistandRepository.lagre(kontekst.behandlingId, forrigeVurderinger)
                }

                val vilkårsresultat = vilkårsresultatRepository.hent(kontekst.behandlingId)
                val nyttVilkår = vilkårsresultat.optionalVilkår(Vilkårtype.BISTANDSVILKÅRET)

                if (nyttVilkår != null) {
                    val forrigeVilkårTidslinje =
                        kontekst.forrigeBehandlingId?.let { vilkårsresultatRepository.hent(it) }
                            ?.optionalVilkår(Vilkårtype.BISTANDSVILKÅRET)
                            ?.tidslinje()
                            .orEmpty()

                    if (nyttVilkår.tidslinje() != forrigeVilkårTidslinje) {
                        nyttVilkår.nullstillTidslinje()
                            .leggTilVurderinger(forrigeVilkårTidslinje)

                        vilkårsresultatRepository.lagre(kontekst.behandlingId, vilkårsresultat)
                    }
                }
            },
            kontekst
        )

        /* Dette skal på sikt ut av denne metoden, og samles i et eget fastsett-steg. */
        val vilkårsresultat = vilkårsresultatRepository.hent(kontekst.behandlingId)
        vilkårsresultat.leggTilHvisIkkeEksisterer(Vilkårtype.BISTANDSVILKÅRET)
        if (avklaringsbehovene.hentBehovForDefinisjon(Definisjon.AVKLAR_BISTANDSBEHOV)?.status()
                ?.erAvsluttet() == true
        ) {
            val grunnlag = BistandFaktagrunnlag(
                kontekst.rettighetsperiode.fom,
                kontekst.rettighetsperiode.tom,
                bistandRepository.hentHvisEksisterer(kontekst.behandlingId)?.vurderinger.orEmpty(),
                studentRepository.hentHvisEksisterer(kontekst.behandlingId)?.studentvurdering,
            )
            Bistandsvilkåret(vilkårsresultat).vurder(grunnlag = grunnlag)
        }
        vilkårsresultatRepository.lagre(kontekst.behandlingId, vilkårsresultat)

        return Fullført
    }

    private fun vedtakBehøverVurdering(kontekst: FlytKontekstMedPerioder): Boolean {
        return when (kontekst.vurderingType) {
            VurderingType.FØRSTEGANGSBEHANDLING,
            VurderingType.REVURDERING -> {
                val perioderBistandsvilkåretErRelevant = perioderHvorBistandsvilkåretErRelevant(kontekst)
                if (perioderBistandsvilkåretErRelevant.segmenter().any { it.verdi } && vurderingsbehovTvingerVurdering(
                        kontekst
                    )) {
                    return true
                }
                val perioderBistandsvilkåretErVurdert = kontekst.forrigeBehandlingId
                    ?.let { forrigeBehandlingId ->
                        val forrigeBehandling = behandlingRepository.hent(forrigeBehandlingId)
                        val forrigeRettighetsperiode =
                            /* Lagrer vi ned rettighetsperioden som ble brukt for en behandling noe sted? */
                            vilkårsresultatRepository.hent(forrigeBehandlingId)
                                .finnVilkår(Vilkårtype.ALDERSVILKÅRET)
                                .tidslinje()
                                .helePerioden()

                        perioderHvorBistandsvilkåretErRelevant(
                            kontekst.copy(
                                /* TODO: hacky. Er faktisk bare behandlingId som brukes av sjekkene. */
                                behandlingId = forrigeBehandlingId,
                                forrigeBehandlingId = forrigeBehandling.forrigeBehandlingId,
                                rettighetsperiode = forrigeRettighetsperiode,
                                behandlingType = forrigeBehandling.typeBehandling(),
                            )
                        )
                    }
                    .orEmpty()

                val relevantePerioderSomManglerVedtattVurdering =
                    perioderBistandsvilkåretErRelevant.leftJoin(perioderBistandsvilkåretErVurdert) { erRelevant, erVurdert ->
                        erRelevant && erVurdert != true
                    }.segmenter().any { it.verdi }
                relevantePerioderSomManglerVedtattVurdering
            }

            VurderingType.MELDEKORT -> false
            VurderingType.EFFEKTUER_AKTIVITETSPLIKT -> false
            VurderingType.EFFEKTUER_AKTIVITETSPLIKT_11_9 -> false
            VurderingType.IKKE_RELEVANT -> false
        }
    }

    private fun vurderingsbehovTvingerVurdering(kontekst: FlytKontekstMedPerioder): Boolean {
        return kontekst.vurderingsbehovRelevanteForSteg.any {
            it in listOf(
                Vurderingsbehov.SYKDOM_ARBEVNE_BEHOV_FOR_BISTAND,
                Vurderingsbehov.MOTTATT_SØKNAD,
                Vurderingsbehov.DØDSFALL_BRUKER,
                Vurderingsbehov.VURDER_RETTIGHETSPERIODE,
                Vurderingsbehov.HELHETLIG_VURDERING
            )
        }
    }

    private fun perioderHvorBistandsvilkåretErRelevant(kontekst: FlytKontekstMedPerioder): Tidslinje<Boolean> {
        val tidligereVurderingsutfall = tidligereVurderinger.behandlingsutfall(kontekst, type())

        val sykdomsvurderinger = sykdomsRepository.hentHvisEksisterer(kontekst.behandlingId)
            ?.somSykdomsvurderingstidslinje(kontekst.rettighetsperiode.fom)
            ?.begrensetTil(kontekst.rettighetsperiode)
            .orEmpty()

        val studentvurderinger = studentRepository.hentHvisEksisterer(kontekst.behandlingId)
            ?.somTidslinje(kontekst.rettighetsperiode)
            .orEmpty()

        return Tidslinje.zip3(tidligereVurderingsutfall, sykdomsvurderinger, studentvurderinger)
            .mapValue { (behandlingsutfall, sykdomsvurdering, studentvurdering) ->
                when (behandlingsutfall) {
                    null -> false
                    TidligereVurderinger.Behandlingsutfall.IKKE_BEHANDLINGSGRUNNLAG -> false
                    TidligereVurderinger.Behandlingsutfall.UUNGÅELIG_AVSLAG -> false
                    TidligereVurderinger.Behandlingsutfall.UKJENT -> {
                        studentvurdering?.erOppfylt() != true &&
                                sykdomsvurdering?.erOppfylt(kravdato = kontekst.rettighetsperiode.fom) == true
                    }
                }
            }
    }

    private fun erTilstrekkeligVurdert(kontekst: FlytKontekstMedPerioder): Boolean {
        val gjeldendeBistandstidslinje = bistandRepository.hentHvisEksisterer(kontekst.behandlingId)
            ?.somBistandsvurderingstidslinje(kontekst.rettighetsperiode.fom)
            .orEmpty()
        val perioderBistandsvilkåretErRelevant = perioderHvorBistandsvilkåretErRelevant(kontekst)
        return perioderBistandsvilkåretErRelevant.leftJoin(gjeldendeBistandstidslinje) { erRelevant, bistandsvurdering ->
            !erRelevant || bistandsvurdering != null
        }.segmenter().all { it.verdi }
    }

    companion object : FlytSteg {
        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): BehandlingSteg {
            return VurderBistandsbehovSteg(repositoryProvider)
        }

        override fun type(): StegType {
            return StegType.VURDER_BISTANDSBEHOV
        }
    }
}
