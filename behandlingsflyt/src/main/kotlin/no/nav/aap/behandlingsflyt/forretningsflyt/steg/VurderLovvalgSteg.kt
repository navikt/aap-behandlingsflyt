package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovMetadataUtleder
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovService
import no.nav.aap.behandlingsflyt.behandling.lovvalg.MedlemskapLovvalgRegisterGrunnlag
import no.nav.aap.behandlingsflyt.behandling.lovvalg.MedlemskapLovvalgVurderingsgrunnlag
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderingerImpl
import no.nav.aap.behandlingsflyt.behandling.vilkår.medlemskap.LovvalgDelvurderingRepository
import no.nav.aap.behandlingsflyt.behandling.vilkår.medlemskap.MedlemskapLovvalgVurderingService
import no.nav.aap.behandlingsflyt.behandling.vilkår.medlemskap.Medlemskapvilkåret
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.ManuellVurderingForLovvalgMedlemskap
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.MedlemskapArbeidInntektRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.PersonopplysningRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.gjeldendeVurderinger
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.lookup.repository.RepositoryProvider
import kotlin.lazy

class VurderLovvalgSteg private constructor(
    private val vilkårsresultatRepository: VilkårsresultatRepository,
    private val personopplysningRepository: PersonopplysningRepository,
    private val medlemskapArbeidInntektRepository: MedlemskapArbeidInntektRepository,
    private val tidligereVurderinger: TidligereVurderinger,
    private val avklaringsbehovService: AvklaringsbehovService,
    private val lovvalgDelvurderingRepository: LovvalgDelvurderingRepository,
    private val unleashGateway: UnleashGateway
) : BehandlingSteg, AvklaringsbehovMetadataUtleder {
    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        vilkårsresultatRepository = repositoryProvider.provide(),
        personopplysningRepository = repositoryProvider.provide(),
        medlemskapArbeidInntektRepository = repositoryProvider.provide(),
        tidligereVurderinger = TidligereVurderingerImpl(repositoryProvider, gatewayProvider),
        avklaringsbehovService = AvklaringsbehovService(repositoryProvider, gatewayProvider),
        lovvalgDelvurderingRepository = repositoryProvider.provide(),
        unleashGateway = gatewayProvider.provide()
    )

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        val registerGrunnlag = lazy { hentRegisterGrunnlag(kontekst.sakId, kontekst.behandlingId) }
        opprettDelvurderinger(kontekst, registerGrunnlag)

        val tvingerAvklaringsbehov = vurderingsbehovSomTvingerAvklaringsbehov()  // med MOTTATT_SØKNAD
        avklaringsbehovService.oppdaterAvklaringsbehovForPeriodisertYtelsesvilkårTilstrekkeligVurdert(
            kontekst = kontekst,
            definisjon = Definisjon.AVKLAR_LOVVALG_MEDLEMSKAP,
            tvingerAvklaringsbehov = tvingerAvklaringsbehov,
            nårVurderingErRelevant = ::nårVurderingErRelevant,
            perioderSomIkkeErTilstrekkeligVurdert = ::perioderSomIkkeErTilstrekkeligVurdert,
            tilbakestillGrunnlag = { tilbakestillVurderinger(kontekst) },
        )

        when (kontekst.vurderingType) {
            VurderingType.FØRSTEGANGSBEHANDLING,
            VurderingType.MIGRER_RETTIGHETSPERIODE,
            VurderingType.MIGERING_FRA_ARENA,
            VurderingType.REVURDERING -> {
                val vilkårsresultat = vilkårsresultatRepository.hent(kontekst.behandlingId)
                // Manuelle vurderinger hentes på nytt da de kan ha blitt tilbakestilt
                val vurderingsgrunnlag = MedlemskapLovvalgVurderingsgrunnlag(
                    register = registerGrunnlag.value,
                    delvurdering = lovvalgDelvurderingRepository.hentHvisEksisterer(kontekst.behandlingId),
                    manuelleVurderinger = hentManuelleVurderinger(kontekst.behandlingId),
                )
                Medlemskapvilkåret(vilkårsresultat, kontekst.rettighetsperiode)
                    .vurder(vurderingsgrunnlag)
                vilkårsresultatRepository.lagre(kontekst.behandlingId, vilkårsresultat)
            }

            VurderingType.EFFEKTUER_AKTIVITETSPLIKT,
            VurderingType.EFFEKTUER_AKTIVITETSPLIKT_11_9,
            VurderingType.UTVID_VEDTAKSLENGDE,
            VurderingType.MELDEKORT,
            VurderingType.AUTOMATISK_BREV,
            VurderingType.G_REGULERING,
            VurderingType.OVERGANG_UFORE_STANS,
            VurderingType.IKKE_RELEVANT -> {
                /* noop */
            }
        }


        return Fullført
    }

    private fun opprettDelvurderinger(
        kontekst: FlytKontekstMedPerioder,
        registerGrunnlag: Lazy<MedlemskapLovvalgRegisterGrunnlag>
    ) {
        when (kontekst.vurderingType) {
            VurderingType.FØRSTEGANGSBEHANDLING,
            VurderingType.MIGRER_RETTIGHETSPERIODE,
            VurderingType.MIGERING_FRA_ARENA,
            VurderingType.REVURDERING -> {
                val tilhørighetVurdering = MedlemskapLovvalgVurderingService().vurderTilhørighet(
                    registerGrunnlag.value,
                    kontekst.rettighetsperiode,
                    kontekst.vurderingType
                )
                val relevantDato = kontekst.rettighetsperiode.fom
                lovvalgDelvurderingRepository.lagre(kontekst.behandlingId, tilhørighetVurdering, relevantDato)
            }

            else -> {
                /* noop */
            }
        }
    }


    private fun perioderSomIkkeErTilstrekkeligVurdert (
        kontekst: FlytKontekstMedPerioder,
    ): Set<Periode> {
        val relevantTidslinje = nårVurderingErRelevant(kontekst)
        val kanVurderesAutomatiskTidslinje = lovvalgDelvurderingRepository.hentHvisEksisterer(kontekst.behandlingId)?.kanVurderesAutomatiskTidslinje() ?: Tidslinje.empty()
        val manuelleVurderinger = hentManuelleVurderinger(kontekst.behandlingId).gjeldendeVurderinger()

        return Tidslinje.map3(relevantTidslinje, manuelleVurderinger, kanVurderesAutomatiskTidslinje) { relevant, manuellVurdering, automatiskVurdering ->
            relevant == true && manuellVurdering == null && automatiskVurdering != true
        }.filter { it.verdi }.perioder().toSet()
    }

    override fun nårVurderingErRelevant(kontekst: FlytKontekstMedPerioder): Tidslinje<Boolean> {
        val tidligereVurderingsutfall = tidligereVurderinger.behandlingsutfall(kontekst, type())
        val kanVurderesAutomatiskTidslinje = lovvalgDelvurderingRepository.hentHvisEksisterer(kontekst.behandlingId)?.kanVurderesAutomatiskTidslinje() ?: Tidslinje.empty()

        val forrigeHaddeAvslagPåLovvalg = kontekst.forrigeBehandlingId?.let { forrigeId ->
            vilkårsresultatRepository.hent(forrigeId)
                .finnVilkår(Vilkårtype.LOVVALG)
                .vilkårsperioder()
                .lastOrNull()?.erOppfylt() == false
        } ?: false

        val tvingerRelevans = kontekst.vurderingsbehovRelevanteForSteg.any {
            it in vurderingsbehovSomGjørAtRevurderingSkalTvinges()
        } || (forrigeHaddeAvslagPåLovvalg && Vurderingsbehov.MOTTATT_SØKNAD in kontekst.vurderingsbehovRelevanteForSteg)

        return Tidslinje.zip2(tidligereVurderingsutfall, kanVurderesAutomatiskTidslinje)
            .mapValue { (behandlingsutfall, automatiskVilkårsvurderingLovvalg) ->
                val automatiskIkkeOppfylt = automatiskVilkårsvurderingLovvalg != true
                when (behandlingsutfall) {
                    null -> false
                    TidligereVurderinger.IkkeBehandlingsgrunnlag -> false
                    is TidligereVurderinger.UunngåeligAvslag -> false
                    is TidligereVurderinger.PotensieltOppfylt -> automatiskIkkeOppfylt || tvingerRelevans
                }
            }

    }


    private fun vurderingsbehovSomGjørAtRevurderingSkalTvinges(): Set<Vurderingsbehov> =
        setOf(
            Vurderingsbehov.REVURDER_LOVVALG,
            Vurderingsbehov.LOVVALG_OG_MEDLEMSKAP,
        )

    private fun tilbakestillVurderinger(
        kontekst: FlytKontekstMedPerioder,
    ) {
        val forrigeVurderinger = kontekst.forrigeBehandlingId?.let { forrigeBehandlingId ->
            medlemskapArbeidInntektRepository.hentHvisEksisterer(forrigeBehandlingId)
                ?.vurderinger
        } ?: emptyList()

        if (forrigeVurderinger.toSet() != hentManuelleVurderinger(kontekst.behandlingId).toSet()) {
            medlemskapArbeidInntektRepository.lagreVurderinger(
                kontekst.behandlingId,
                forrigeVurderinger,
            )
        }
    }

    private fun vurderingsbehovSomTvingerAvklaringsbehov(): Set<Vurderingsbehov> =
        setOf(Vurderingsbehov.REVURDER_LOVVALG, Vurderingsbehov.LOVVALG_OG_MEDLEMSKAP, Vurderingsbehov.MOTTATT_SØKNAD)

    private fun hentManuelleVurderinger(behandlingId: BehandlingId): List<ManuellVurderingForLovvalgMedlemskap> {
        return medlemskapArbeidInntektRepository.hentHvisEksisterer(behandlingId)?.vurderinger.orEmpty()
    }

    private fun hentRegisterGrunnlag(sakId: SakId, behandlingId: BehandlingId): MedlemskapLovvalgRegisterGrunnlag {
        val medlemskapArbeidInntektGrunnlag =
            medlemskapArbeidInntektRepository.hentHvisEksisterer(behandlingId)
        val oppgittUtenlandsOppholdGrunnlag =
            medlemskapArbeidInntektRepository.hentOppgittUtenlandsOppholdHvisEksisterer(behandlingId)
                ?: medlemskapArbeidInntektRepository.hentSistRelevanteOppgitteUtenlandsOppholdHvisEksisterer(sakId)

        val brukerPersonopplysning = personopplysningRepository.hentBrukerPersonOpplysningHvisEksisterer(behandlingId)

        return MedlemskapLovvalgRegisterGrunnlag.fra(
            medlemskapArbeidInntektGrunnlag,
            brukerPersonopplysning,
            oppgittUtenlandsOppholdGrunnlag,
            vurderBosattStatusOgNorskStatsborgerskap =
                unleashGateway.isEnabled(BehandlingsflytFeature.BosattStatsborgerskapGjennomslipp),
        )
    }

    override val stegType = type()

    companion object : FlytSteg {
        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): VurderLovvalgSteg {
            return VurderLovvalgSteg(repositoryProvider, gatewayProvider)
        }

        override fun type(): StegType {
            return StegType.VURDER_LOVVALG
        }
    }
}
