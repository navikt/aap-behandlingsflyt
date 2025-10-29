package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovService
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.ÅrsakTilSettPåVent
import no.nav.aap.behandlingsflyt.behandling.lovvalg.MedlemskapLovvalgGrunnlag
import no.nav.aap.behandlingsflyt.behandling.lovvalg.tilTidslinje
import no.nav.aap.behandlingsflyt.behandling.lovvalg.validerGyldigForRettighetsperiode
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderingerImpl
import no.nav.aap.behandlingsflyt.behandling.vilkår.medlemskap.Medlemskapvilkåret
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Avslagsårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsresultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.MedlemskapArbeidInntektRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.PersonopplysningRepository
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FantVentebehov
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.flyt.steg.Ventebehov
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.lookup.repository.RepositoryProvider

class VurderLovvalgSteg private constructor(
    private val vilkårsresultatRepository: VilkårsresultatRepository,
    private val behandlingRepository: BehandlingRepository,
    private val personopplysningRepository: PersonopplysningRepository,
    private val medlemskapArbeidInntektRepository: MedlemskapArbeidInntektRepository,
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
    private val tidligereVurderinger: TidligereVurderinger,
    private val avklaringsbehovService: AvklaringsbehovService,
    private val unleashGateway: UnleashGateway
) : BehandlingSteg {
    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        vilkårsresultatRepository = repositoryProvider.provide(),
        behandlingRepository = repositoryProvider.provide(),
        personopplysningRepository = repositoryProvider.provide(),
        medlemskapArbeidInntektRepository = repositoryProvider.provide(),
        avklaringsbehovRepository = repositoryProvider.provide(),
        tidligereVurderinger = TidligereVurderingerImpl(repositoryProvider),
        avklaringsbehovService = AvklaringsbehovService(repositoryProvider),
        unleashGateway = gatewayProvider.provide()
    )

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        if (unleashGateway.isDisabled(BehandlingsflytFeature.LovvalgMedlemskapPeriodisert)) {
            return gammelUfør(kontekst)
        }

        var grunnlag = lazy { hentGrunnlag(kontekst.sakId, kontekst.behandlingId) }
        val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)

        if (utenlandsLovvalgslandTattAvVent(kontekst, avklaringsbehovene)) {
            tilbakestillVurderinger(kontekst, grunnlag.value)
            grunnlag = lazy { hentGrunnlag(kontekst.sakId, kontekst.behandlingId) }
        }

        avklaringsbehovService.oppdaterAvklaringsbehovForPeriodisertYtelsesvilkår(
            avklaringsbehovene = avklaringsbehovene,
            behandlingRepository = behandlingRepository,
            vilkårsresultatRepository = vilkårsresultatRepository,
            kontekst = kontekst,
            definisjon = Definisjon.AVKLAR_LOVVALG_MEDLEMSKAP,
            tvingerAvklaringsbehov = vurderingsbehovSomTvingerAvklaringsbehov(),
            nårVurderingErRelevant = { perioderVurderingErRelevant(kontekst, grunnlag.value) },
            erTilstrekkeligVurdert = { erTilstrekkeligVurdert(kontekst, grunnlag.value) },
            tilbakestillGrunnlag = { tilbakestillVurderinger(kontekst, grunnlag.value) },
        )

        when (kontekst.vurderingType) {
            VurderingType.FØRSTEGANGSBEHANDLING,
            VurderingType.REVURDERING -> {
                val vilkårsresultat = vilkårsresultatRepository.hent(kontekst.behandlingId)
                Medlemskapvilkåret(vilkårsresultat, kontekst.rettighetsperiode, brukPeriodisertManuellVurdering = true)
                    .vurder(grunnlag.value)
                vilkårsresultatRepository.lagre(kontekst.behandlingId, vilkårsresultat)

                if (norgeIkkeKompetentStat(kontekst)) {
                    return FantVentebehov(
                        Ventebehov(
                            definisjon = Definisjon.VENTE_PÅ_UTENLANDSK_VIDEREFØRING_AVKLARING,
                            grunn = ÅrsakTilSettPåVent.VENTER_PÅ_UTENLANDSK_VIDEREFORING_AVKLARING
                        )
                    )
                }
            }

            VurderingType.EFFEKTUER_AKTIVITETSPLIKT,
            VurderingType.EFFEKTUER_AKTIVITETSPLIKT_11_9,
            VurderingType.MELDEKORT,
            VurderingType.IKKE_RELEVANT -> {
                /* noop */
            }
        }

        return Fullført
    }

    /**
     * Her har vi ulike scenarioer vi må ta høyde for
     * - Hvis det finnes manuelle vurderinger - så må hele tidslinjen være vurdert manuelt
     * - Hvis det er automatisk vurdering - så må lovvalgvilkåret være oppfylt for hele perioden
     */
    private fun erTilstrekkeligVurdert(
        kontekst: FlytKontekstMedPerioder,
        grunnlag: MedlemskapLovvalgGrunnlag
    ): Boolean {
        val harManuelleVurderinger = grunnlag.medlemskapArbeidInntektGrunnlag?.vurderinger?.isNotEmpty() == true
        if (harManuelleVurderinger) {
            return grunnlag.medlemskapArbeidInntektGrunnlag
                .gjeldendeVurderinger()
                .validerGyldigForRettighetsperiode(kontekst.rettighetsperiode)
                .isValid
        } else {
            val vilkårsresultat = vilkårsresultatRepository.hent(kontekst.behandlingId)
            Medlemskapvilkåret(vilkårsresultat, kontekst.rettighetsperiode, brukPeriodisertManuellVurdering = true)
                .vurder(grunnlag)
            val lovvalgVilkåretOppfylt = !vilkårsresultat.finnVilkår(Vilkårtype.LOVVALG).harPerioderSomIkkeErOppfylt()
            return lovvalgVilkåretOppfylt
        }
    }

    private fun perioderVurderingErRelevant(kontekst: FlytKontekstMedPerioder, grunnlag: MedlemskapLovvalgGrunnlag): Tidslinje<Boolean> {
        val tidligereVurderingsutfall = tidligereVurderinger.behandlingsutfall(kontekst, type())
        val vilkårsvurderingLovvalg = vilkårsvurderingLovvalgUtenManuelleVurderinger(kontekst, grunnlag)

        return Tidslinje.zip2(tidligereVurderingsutfall, vilkårsvurderingLovvalg)
            .mapValue { (behandlingsutfall, vilkårsvurdering) ->
                when (behandlingsutfall) {
                    null -> false
                    TidligereVurderinger.Behandlingsutfall.IKKE_BEHANDLINGSGRUNNLAG -> false
                    TidligereVurderinger.Behandlingsutfall.UUNGÅELIG_AVSLAG -> false
                    TidligereVurderinger.Behandlingsutfall.UKJENT -> {
                        val lovvalgIkkeOppfylt = vilkårsvurdering?.erOppfylt() == false

                        // Må gjøres slik for å trigge overstyrt avklaringsbehov hvis allerede automatisk oppfylt
                        val tvingerAvklaringsbehov = kontekst.vurderingsbehovRelevanteForSteg.any {
                            it in vurderingsbehovSomTvingerAvklaringsbehov()
                        }

                        lovvalgIkkeOppfylt || tvingerAvklaringsbehov
                    }
                }
            }
    }

    private fun vilkårsvurderingLovvalgUtenManuelleVurderinger(
        kontekst: FlytKontekstMedPerioder,
        grunnlag: MedlemskapLovvalgGrunnlag,
    ): Tidslinje<Vilkårsvurdering> {
        val vilkårsresultat = Vilkårsresultat()
        val grunnlagUtenManuellVurdering = grunnlag.copy(
            medlemskapArbeidInntektGrunnlag = grunnlag.medlemskapArbeidInntektGrunnlag?.copy(
                manuellVurdering = null,
                vurderinger = emptyList()
            )
        )

        Medlemskapvilkåret(vilkårsresultat, kontekst.rettighetsperiode, brukPeriodisertManuellVurdering = true)
            .vurder(grunnlagUtenManuellVurdering)

        return vilkårsresultat.finnVilkår(Vilkårtype.LOVVALG).tidslinje()
    }

    private fun tilbakestillVurderinger(
        kontekst: FlytKontekstMedPerioder,
        grunnlag: MedlemskapLovvalgGrunnlag
    ) {
        // Tilbakestill vurderinger i grunnlag
        val forrigeVurderinger = kontekst.forrigeBehandlingId?.let { forrigeBehandlingId ->
            medlemskapArbeidInntektRepository.hentHvisEksisterer(forrigeBehandlingId)
                ?.vurderinger
        } ?: emptyList()

        if (forrigeVurderinger != grunnlag.medlemskapArbeidInntektGrunnlag?.vurderinger) {
            medlemskapArbeidInntektRepository.lagreVurderinger(
                kontekst.behandlingId,
                forrigeVurderinger,
            )
        }

        // Tilbakestill vilkårsvurderinger
        val forrigeVilkårsvurderinger =
            kontekst.forrigeBehandlingId
                ?.let { vilkårsresultatRepository.hent(it).optionalVilkår(Vilkårtype.LOVVALG) }
                ?.tidslinje()
                ?: Tidslinje()

        val vilkårsresultat = vilkårsresultatRepository.hent(kontekst.behandlingId)
        val vilkår = vilkårsresultat.optionalVilkår(Vilkårtype.LOVVALG)
        if (vilkår != null) {
            vilkår.nullstillTidslinje()
            vilkår.leggTilVurderinger(forrigeVilkårsvurderinger)
            vilkårsresultatRepository.lagre(kontekst.behandlingId, vilkårsresultat)
        }
    }

    private fun vurderingsbehovSomTvingerAvklaringsbehov(): Set<Vurderingsbehov> =
        setOf(Vurderingsbehov.REVURDER_LOVVALG, Vurderingsbehov.LOVVALG_OG_MEDLEMSKAP)


    private fun gammelUfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        var grunnlag = lazy { hentGrunnlag(kontekst.sakId, kontekst.behandlingId) }
        val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)

        if (utenlandsLovvalgslandTattAvVent(kontekst, avklaringsbehovene)) {
            tilbakestillGrunnlag(kontekst, grunnlag.value)
            grunnlag = lazy { hentGrunnlag(kontekst.sakId, kontekst.behandlingId) }
        }

        avklaringsbehovService.oppdaterAvklaringsbehov(
            avklaringsbehovene = avklaringsbehovene,
            definisjon = Definisjon.AVKLAR_LOVVALG_MEDLEMSKAP,
            vedtakBehøverVurdering = { vedtakBehøverVurdering(kontekst, grunnlag, avklaringsbehovene) },
            erTilstrekkeligVurdert = { grunnlag.value.medlemskapArbeidInntektGrunnlag?.manuellVurdering != null },
            tilbakestillGrunnlag = { tilbakestillGrunnlag(kontekst, grunnlag.value) },
            kontekst
        )

        when (kontekst.vurderingType) {
            VurderingType.FØRSTEGANGSBEHANDLING,
            VurderingType.REVURDERING -> {
                if (tidligereVurderinger.girIngenBehandlingsgrunnlag(kontekst, type())) {
                    avklaringsbehovene.avbrytForSteg(type())
                    return Fullført
                }

                val vilkårsresultat = vilkårsresultatRepository.hent(kontekst.behandlingId)
                Medlemskapvilkåret(vilkårsresultat, kontekst.rettighetsperiode)
                    .vurder(grunnlag.value)
                vilkårsresultatRepository.lagre(kontekst.behandlingId, vilkårsresultat)

                avbrytTidligereUnødvendigeBehov(kontekst, grunnlag.value)

                if (norgeIkkeKompetentStat(kontekst)) {
                    return FantVentebehov(
                        Ventebehov(
                            definisjon = Definisjon.VENTE_PÅ_UTENLANDSK_VIDEREFØRING_AVKLARING,
                            grunn = ÅrsakTilSettPåVent.VENTER_PÅ_UTENLANDSK_VIDEREFORING_AVKLARING
                        )
                    )
                }
            }

            VurderingType.EFFEKTUER_AKTIVITETSPLIKT,
            VurderingType.EFFEKTUER_AKTIVITETSPLIKT_11_9,
            VurderingType.MELDEKORT,
            VurderingType.IKKE_RELEVANT -> {
                /* noop */
            }
        }

        return Fullført
    }

    private fun tilbakestillGrunnlag(
        kontekst: FlytKontekstMedPerioder,
        grunnlag: MedlemskapLovvalgGrunnlag
    ) {
        val forrigeManuelleVurdering = kontekst.forrigeBehandlingId?.let { forrigeBehandlingId ->
            medlemskapArbeidInntektRepository.hentHvisEksisterer(forrigeBehandlingId)
                ?.manuellVurdering
        }
        if (forrigeManuelleVurdering != grunnlag.medlemskapArbeidInntektGrunnlag?.manuellVurdering) {
            medlemskapArbeidInntektRepository.lagreVurderinger(
                kontekst.behandlingId,
                forrigeManuelleVurdering?.let { listOf(it) } ?: emptyList(),
            )
        }
    }

    private fun norgeIkkeKompetentStat(kontekst: FlytKontekstMedPerioder): Boolean =
        vilkårsresultatRepository.hent(kontekst.behandlingId)
            .optionalVilkår(Vilkårtype.LOVVALG)
            ?.vilkårsperioder()
            ?.any { it.avslagsårsak == Avslagsårsak.NORGE_IKKE_KOMPETENT_STAT } == true

    private fun vedtakBehøverVurdering(
        kontekst: FlytKontekstMedPerioder,
        grunnlag: Lazy<MedlemskapLovvalgGrunnlag>,
        avklaringsbehovene: Avklaringsbehovene,
    ): Boolean {
        return when (kontekst.vurderingType) {
            VurderingType.FØRSTEGANGSBEHANDLING,
            VurderingType.REVURDERING -> {
                if (tidligereVurderinger.girAvslagEllerIngenBehandlingsgrunnlag(kontekst, type())) {
                    return false
                }

                if (kontekst.vurderingsbehovRelevanteForSteg.isEmpty()) {
                    return false
                }

                if (manueltTriggetVurderingsbehov(kontekst)) {
                    return true
                }

                if (manueltTriggetLøsning(avklaringsbehovene)) {
                    return true
                }

                val vilkårsresultat = Vilkårsresultat()
                val grunnlagUtenManuellVurdering = grunnlag.value.copy(
                    medlemskapArbeidInntektGrunnlag = grunnlag.value.medlemskapArbeidInntektGrunnlag?.copy(
                        manuellVurdering = null,
                        vurderinger = emptyList()
                    )
                )
                Medlemskapvilkåret(vilkårsresultat, kontekst.rettighetsperiode)
                    .vurder(grunnlagUtenManuellVurdering)
                vilkårsresultat.finnVilkår(Vilkårtype.LOVVALG).harPerioderSomIkkeErOppfylt()
            }

            VurderingType.MELDEKORT -> false
            VurderingType.IKKE_RELEVANT -> false
            VurderingType.EFFEKTUER_AKTIVITETSPLIKT -> false
            VurderingType.EFFEKTUER_AKTIVITETSPLIKT_11_9 -> false
        }
    }

    private fun utenlandsLovvalgslandTattAvVent(
        kontekst: FlytKontekstMedPerioder,
        avklaringsbehovene: Avklaringsbehovene
    ): Boolean {
        val norgeIkkeKompetentStat = norgeIkkeKompetentStat(kontekst)
        val finnesÅpneAvklaringsbehov =
            avklaringsbehovene.hentBehovForDefinisjon(Definisjon.AVKLAR_LOVVALG_MEDLEMSKAP)?.status()?.erÅpent() == true
        val overstyrtBehov =
            avklaringsbehovene.hentBehovForDefinisjon(Definisjon.MANUELL_OVERSTYRING_LOVVALG)?.status()
                ?.erÅpent() == true
        val finnesÅpneVentebehov =
            avklaringsbehovene.hentBehovForDefinisjon(Definisjon.VENTE_PÅ_UTENLANDSK_VIDEREFØRING_AVKLARING)?.status()
                ?.erÅpent() == true

        return norgeIkkeKompetentStat && !finnesÅpneVentebehov && !finnesÅpneAvklaringsbehov && !overstyrtBehov
    }

    private fun manueltTriggetLøsning(avklaringsbehovene: Avklaringsbehovene): Boolean {
        val avklaringsbehov = avklaringsbehovene.hentBehovForDefinisjon(Definisjon.AVKLAR_LOVVALG_MEDLEMSKAP)
        return avklaringsbehov != null
    }

    private fun manueltTriggetVurderingsbehov(kontekst: FlytKontekstMedPerioder): Boolean {
        return kontekst.vurderingsbehovRelevanteForSteg
            .any { it == Vurderingsbehov.REVURDER_LOVVALG || it == Vurderingsbehov.LOVVALG_OG_MEDLEMSKAP }
    }

    private fun avbrytTidligereUnødvendigeBehov(
        kontekst: FlytKontekstMedPerioder,
        grunnlag: MedlemskapLovvalgGrunnlag
    ) {
        val alleVilkårOppfylt =
            vilkårsresultatRepository.hent(kontekst.behandlingId).finnVilkår(Vilkårtype.LOVVALG).vilkårsperioder()
                .all { it.erOppfylt() }
        if (alleVilkårOppfylt
            && grunnlag.medlemskapArbeidInntektGrunnlag?.manuellVurdering == null
            && !manueltTriggetVurderingsbehov(kontekst)
        ) {
            avklaringsbehovService.avbrytForSteg(kontekst.behandlingId, type())
        }
    }

    private fun hentGrunnlag(sakId: SakId, behandlingId: BehandlingId): MedlemskapLovvalgGrunnlag {
        val medlemskapArbeidInntektGrunnlag =
            medlemskapArbeidInntektRepository.hentHvisEksisterer(behandlingId)
        val oppgittUtenlandsOppholdGrunnlag =
            medlemskapArbeidInntektRepository.hentOppgittUtenlandsOppholdHvisEksisterer(behandlingId)
                ?: medlemskapArbeidInntektRepository.hentSistRelevanteOppgitteUtenlandsOppholdHvisEksisterer(sakId)

        val brukerPersonopplysning = personopplysningRepository.hentBrukerPersonOpplysningHvisEksisterer(behandlingId)

        val grunnlag = MedlemskapLovvalgGrunnlag(
            medlemskapArbeidInntektGrunnlag,
            brukerPersonopplysning,
            oppgittUtenlandsOppholdGrunnlag
        )
        return grunnlag
    }

    companion object : FlytSteg {
        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): BehandlingSteg {
            return VurderLovvalgSteg(repositoryProvider, gatewayProvider)
        }

        override fun type(): StegType {
            return StegType.VURDER_LOVVALG
        }
    }
}
