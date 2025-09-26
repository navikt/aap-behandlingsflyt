package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovService
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderingerImpl
import no.nav.aap.behandlingsflyt.behandling.vilkår.bistand.BistandFaktagrunnlag
import no.nav.aap.behandlingsflyt.behandling.vilkår.bistand.Bistandsvilkåret
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Innvilgelsesårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkår
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårService
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsresultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.BistandGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.BistandRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.StudentGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.StudentRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.Sykdomsvurdering
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FantAvklaringsbehov
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Status
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.miljo.Miljø
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.tidslinjeOf
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.lookup.repository.RepositoryProvider
import org.slf4j.LoggerFactory
import java.time.LocalDate

class VurderBistandsbehovSteg private constructor(
    private val bistandRepository: BistandRepository,
    private val studentRepository: StudentRepository,
    private val sykdomsRepository: SykdomRepository,
    private val vilkårsresultatRepository: VilkårsresultatRepository,
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
    private val tidligereVurderinger: TidligereVurderinger,
    private val vilkårService: VilkårService,
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
        vilkårService = VilkårService(repositoryProvider),
        behandlingRepository = repositoryProvider.provide(),
        avklaringsbehovService = AvklaringsbehovService(repositoryProvider)
    )

    private val log = LoggerFactory.getLogger(javaClass)

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        if (Miljø.erProd()) {
            return gammelUtfør(kontekst)
        }

        val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)
        avklaringsbehovService.oppdaterAvklaringsbehov(
            avklaringsbehovene = avklaringsbehovene,
            definisjon = Definisjon.AVKLAR_BISTANDSBEHOV,
            vedtakBehøverVurdering = { vedtakBehøverVurdering(kontekst) },
            erTilstrekkeligVurdert = { true },
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
            },
            kontekst
        )

        /* Dette skal på sikt ut av denne metoden, og samles i et eget fastsett-steg. */
        val vilkårsresultat = vilkårsresultatRepository.hent(kontekst.behandlingId)
        vilkårsresultat.leggTilHvisIkkeEksisterer(Vilkårtype.BISTANDSVILKÅRET)
        if (avklaringsbehovene.hentBehovForDefinisjon(Definisjon.AVKLAR_BISTANDSBEHOV)?.status() in setOf(Status.AVSLUTTET, Status.AVBRUTT)) {
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

                if (perioderBistandsvilkåretErRelevant.any { it.verdi } && vurderingsbehovTvingerVurdering(kontekst)) {
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
                    ?: tidslinjeOf()

                perioderBistandsvilkåretErRelevant.leftJoin(perioderBistandsvilkåretErVurdert) { erRelevant, erVurdert ->
                    erRelevant && erVurdert != true
                }.any { it.verdi }
            }

            VurderingType.MELDEKORT -> false
            VurderingType.EFFEKTUER_AKTIVITETSPLIKT -> false
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
            ?: tidslinjeOf()

        val studentvurderinger = studentRepository.hentHvisEksisterer(kontekst.behandlingId)
            ?.somTidslinje(kontekst.rettighetsperiode)
            ?: tidslinjeOf()

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

    fun gammelUtfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        val bistandsGrunnlag = bistandRepository.hentHvisEksisterer(kontekst.behandlingId)
        val studentGrunnlag = studentRepository.hentHvisEksisterer(kontekst.behandlingId)
        val sykdomsvurderinger =
            sykdomsRepository.hentHvisEksisterer(kontekst.behandlingId)?.sykdomsvurderinger.orEmpty()

        val vilkårsresultat = vilkårsresultatRepository.hent(kontekst.behandlingId)
        val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)

        when (kontekst.vurderingType) {
            VurderingType.FØRSTEGANGSBEHANDLING, VurderingType.REVURDERING -> {
                if (tidligereVurderinger.girIngenBehandlingsgrunnlag(kontekst, type())) {
                    log.info("Ingen behandlingsgrunnlag for vilkårtype ${Vilkårtype.BISTANDSVILKÅRET} for behandlingId ${kontekst.behandlingId}. Avbryter steg.")
                    avklaringsbehovene.avbrytForSteg(type())
                    vilkårService.ingenNyeVurderinger(
                        kontekst.behandlingId,
                        Vilkårtype.BISTANDSVILKÅRET,
                        kontekst.rettighetsperiode,
                        "mangler behandlingsgrunnlag",
                    )
                    return Fullført
                }

                // sjekk behovet for avklaring for periode
                if (erBehovForAvklarForPerioden(
                        kontekst.rettighetsperiode,
                        studentGrunnlag,
                        sykdomsvurderinger,
                        bistandsGrunnlag,
                        vilkårsresultat,
                        avklaringsbehovene,
                        kontekst.behandlingType,
                    )
                ) {
                    return FantAvklaringsbehov(Definisjon.AVKLAR_BISTANDSBEHOV)
                }

                // Vurder vilkår
                vurderVilkårForPeriode(
                    kontekst.rettighetsperiode,
                    bistandsGrunnlag,
                    studentGrunnlag,
                    vilkårsresultat
                )
            }

            VurderingType.MELDEKORT,
            VurderingType.EFFEKTUER_AKTIVITETSPLIKT,
            VurderingType.IKKE_RELEVANT -> {
                // Skal ikke gjøre noe
            }
        }
        if (kontekst.harNoeTilBehandling()) {
            vilkårsresultatRepository.lagre(kontekst.behandlingId, vilkårsresultat)
        }

        postcondition(vilkårsresultat.finnVilkår(Vilkårtype.BISTANDSVILKÅRET))

        return Fullført
    }

    private fun erIkkeVurdertTidligereIBehandlingen(
        avklaringsbehovene: Avklaringsbehovene
    ): Boolean {
        return !avklaringsbehovene.erVurdertTidligereIBehandlingen(Definisjon.AVKLAR_BISTANDSBEHOV)
    }

    private fun postcondition(vilkår: Vilkår) {
        if (vilkår.harPerioderSomIkkeErVurdert(vilkår.vilkårsperioder().map { it.periode }.toSet())) {
            throw IllegalStateException("Det finnes perioder som ikke er vurdert")
        }
    }

    private fun erBehovForAvklarForPerioden(
        periode: Periode,
        studentGrunnlag: StudentGrunnlag?,
        sykdomsvurderinger: List<Sykdomsvurdering>,
        bistandsGrunnlag: BistandGrunnlag?,
        vilkårsresultat: Vilkårsresultat,
        avklaringsbehovene: Avklaringsbehovene,
        typeBehandling: TypeBehandling,
    ): Boolean {
        val vilkår = vilkårsresultat.finnVilkår(Vilkårtype.BISTANDSVILKÅRET)
        val erIkkeAvslagPåVilkårTidligere =
            erIkkeAvslagPåVilkårTidligere(vilkårsresultat, sykdomsvurderinger, typeBehandling, periode.fom)
        if (!erIkkeAvslagPåVilkårTidligere || studentGrunnlag?.studentvurdering?.erOppfylt() == true) {
            return false
        }
        return harBehovForAvklaring(
            bistandsGrunnlag,
            periode,
            vilkår,
            studentGrunnlag?.studentvurdering?.erOppfylt() == true
        ) || erIkkeVurdertTidligereIBehandlingen(avklaringsbehovene)
    }


    private fun vurderVilkårForPeriode(
        periode: Periode,
        bistandsGrunnlag: BistandGrunnlag?,
        studentGrunnlag: StudentGrunnlag?,
        vilkårsresultat: Vilkårsresultat
    ) {
        val grunnlag = BistandFaktagrunnlag(
            periode.fom,
            periode.tom,
            bistandsGrunnlag?.vurderinger.orEmpty(),
            studentGrunnlag?.studentvurdering
        )
        Bistandsvilkåret(vilkårsresultat).vurder(grunnlag = grunnlag)
    }

    private fun harBehovForAvklaring(
        bistandsGrunnlag: BistandGrunnlag?,
        periodeTilVurdering: Periode,
        vilkår: Vilkår,
        erStudentStegOppfylt: Boolean
    ): Boolean {
        return !harVurdertPerioden(periodeTilVurdering, bistandsGrunnlag)
                || harInnvilgetForStudentUtenÅVæreStudent(vilkår, erStudentStegOppfylt)
    }

    private fun harVurdertPerioden(
        periode: Periode,
        grunnlag: BistandGrunnlag?
    ): Boolean {
        return grunnlag?.harVurdertPeriode(periode) == true
    }

    private fun erIkkeAvslagPåVilkårTidligere(
        vilkårsresultat: Vilkårsresultat,
        sykdomsvurderinger: List<Sykdomsvurdering>,
        typeBehandling: TypeBehandling,
        kravDato: LocalDate,
    ): Boolean {
        return vilkårsresultat.finnVilkår(Vilkårtype.ALDERSVILKÅRET).harPerioderSomErOppfylt()
                && vilkårsresultat.finnVilkår(Vilkårtype.LOVVALG).harPerioderSomErOppfylt()
                && sykdomsvurderinger.any { it.erOppfylt(typeBehandling, kravDato) }
    }


    private fun harInnvilgetForStudentUtenÅVæreStudent(vilkår: Vilkår, erStudentStegOppfylt: Boolean): Boolean {

        return !erStudentStegOppfylt && vilkår.vilkårsperioder()
            .any { it.innvilgelsesårsak == Innvilgelsesårsak.STUDENT }
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
