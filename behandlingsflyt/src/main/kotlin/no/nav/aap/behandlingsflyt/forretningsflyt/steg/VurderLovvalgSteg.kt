package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.SYSTEMBRUKER
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovMetadataUtleder
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovService
import no.nav.aap.behandlingsflyt.behandling.lovvalg.MedlemskapLovvalgFaktaGrunnlag
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderingerImpl
import no.nav.aap.behandlingsflyt.behandling.vilkår.medlemskap.EØSLandEllerLandMedAvtale
import no.nav.aap.behandlingsflyt.behandling.vilkår.medlemskap.MedlemskapLovvalgVurderingService
import no.nav.aap.behandlingsflyt.behandling.vilkår.medlemskap.Medlemskapvilkåret
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsresultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.LovvalgDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.LovvalgMedlemskapVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.MedlemskapDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.MedlemskapArbeidInntektRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.PersonopplysningRepository
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
import no.nav.aap.komponenter.tidslinje.orEmpty
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Tid
import no.nav.aap.lookup.repository.RepositoryProvider
import java.time.LocalDateTime
import kotlin.lazy

class VurderLovvalgSteg internal constructor(
    private val vilkårsresultatRepository: VilkårsresultatRepository,
    private val personopplysningRepository: PersonopplysningRepository,
    private val medlemskapArbeidInntektRepository: MedlemskapArbeidInntektRepository,
    private val tidligereVurderinger: TidligereVurderinger,
    private val avklaringsbehovService: AvklaringsbehovService,
    private val unleashGateway: UnleashGateway
) : BehandlingSteg, AvklaringsbehovMetadataUtleder {
    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        vilkårsresultatRepository = repositoryProvider.provide(),
        personopplysningRepository = repositoryProvider.provide(),
        medlemskapArbeidInntektRepository = repositoryProvider.provide(),
        tidligereVurderinger = TidligereVurderingerImpl(repositoryProvider, gatewayProvider),
        avklaringsbehovService = AvklaringsbehovService(repositoryProvider, gatewayProvider),
        unleashGateway = gatewayProvider.provide()
    )

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        val grunnlag = lazy { hentFaktaGrunnlag(kontekst.sakId, kontekst.behandlingId) }

        val tvingerAvklaringsbehov = vurderingsbehovSomTvingerAvklaringsbehov()  // med MOTTATT_SØKNAD
        avklaringsbehovService.oppdaterAvklaringsbehovForPeriodisertYtelsesvilkårTilstrekkeligVurdert(
            kontekst = kontekst,
            definisjon = Definisjon.AVKLAR_LOVVALG_MEDLEMSKAP,
            tvingerAvklaringsbehov = tvingerAvklaringsbehov,
            nårVurderingErRelevant = ::nårVurderingErRelevant,
            perioderSomIkkeErTilstrekkeligVurdert = ::perioderSomIkkeErTilstrekkeligVurdert,
            tilbakestillGrunnlag = { tilbakestillVurderinger(kontekst, grunnlag.value) },
        )

        when (kontekst.vurderingType) {
            VurderingType.FØRSTEGANGSBEHANDLING,
            VurderingType.MIGRER_RETTIGHETSPERIODE,
            VurderingType.MIGERING_FRA_ARENA,
            VurderingType.REVURDERING -> {
                // TODO: Automatisk vurdering (og delvurderinger) bør egentlig være input til faktagrunnlaget, ikke motsatt
                lagreAutomatiskVurdering(kontekst, hentFaktaGrunnlag(kontekst.sakId, kontekst.behandlingId))

                // Hent grunnlag på nytt da det kan ha blitt tilbakestilt eller fått ny automatisk vurdering
                val grunnlag = hentFaktaGrunnlag(kontekst.sakId, kontekst.behandlingId)
                val vilkårsresultat = vilkårsresultatRepository.hent(kontekst.behandlingId)
                Medlemskapvilkåret(vilkårsresultat, kontekst.rettighetsperiode, kontekst.vurderingType)
                    .vurder(grunnlag)
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


    /**
     * Lagrer en automatisk vurdering (NOR, medlem i folketrygden) for hele rettighetsperioden når
     * lovvalg/medlemskap kan vurderes automatisk. Lagrer ingenting når det ikke kan det.
     *
     * Kjører ikke automatisk hvis vi har en manuell vurdering.  
     */
    private fun lagreAutomatiskVurdering(kontekst: FlytKontekstMedPerioder, grunnlag: MedlemskapLovvalgFaktaGrunnlag) {
        val eksisterende = grunnlag.medlemskapArbeidInntektGrunnlag

        // Kjører ikke automatisk vurdering hvis det finnes manuell vurdering
        // TODO: På sikt kan vi kjøre automatisk vurdering fra ny stønadsperiode? Eller bør vi aldri kjøre på nytt? 
        if (eksisterende?.manuelleVurderinger().orEmpty().isNotEmpty()) return

        val rettighetsperiode = kontekst.rettighetsperiode

        // TODO: Bør her lagre ned delvurderingene. Disse bør også sendes med i grunnlagApi slik at de ikke utledes på nytt
        if (!kanVurderesAutomatisk(grunnlag, rettighetsperiode)) return

        val begrunnelse = "Automatisk vurdert av Kelvin"
        val automatiskVurdering = LovvalgMedlemskapVurdering(
            lovvalg = LovvalgDto(begrunnelse, EØSLandEllerLandMedAvtale.NOR), // Litt rart at dto er del av vurdering
            medlemskap = MedlemskapDto(
                begrunnelse,
                varMedlemIFolketrygd = true
            ), // Kan behandles automatisk indikerer disse verdiene. Bør modelleres mer eksplisitt 
            vurdertAv = SYSTEMBRUKER,
            vurdertDato = LocalDateTime.now(),
            fom = rettighetsperiode.fom,
            tom = rettighetsperiode.tom.takeIf { it != Tid.MAKS },
            vurdertIBehandling = kontekst.behandlingId,
        )

        val vurderingerUtenomIkkeVedtatteAutomatiskeVurderinger =
            eksisterende?.vurderinger?.filterNot { it.vurdertIBehandling == kontekst.behandlingId && it.erAutomatiskVurdert() }
                .orEmpty()

        // TODO: Lagrer automatiske vurderinger på nytt, selv om vi finner samme resultat som forrige behandling. 
        //  Ønsker vi å gjøre det?
        medlemskapArbeidInntektRepository.lagreVurderinger(
            kontekst.behandlingId,
            vurderingerUtenomIkkeVedtatteAutomatiskeVurderinger + automatiskVurdering
        )
    }

    private fun kanVurderesAutomatisk(grunnlag: MedlemskapLovvalgFaktaGrunnlag, rettighetsperiode: Periode): Boolean {
        return grunnlag.nyeSoknadGrunnlag != null // Denne gir "ikke relevant" i vilkåret. Ikke så lett å se hvorfor
             && MedlemskapLovvalgVurderingService()
            .vurderTilhørighet(grunnlag, rettighetsperiode)
            .kanBehandlesAutomatisk
    }

    private fun perioderSomIkkeErTilstrekkeligVurdert(
        kontekst: FlytKontekstMedPerioder,
    ): Set<Periode> {
        val grunnlag = hentFaktaGrunnlag(kontekst.sakId, kontekst.behandlingId)
        val relevantTidslinje = nårVurderingErRelevant(kontekst)
        val automatiskVilkårsvurderingLovvalg =
            vilkårsvurderingLovvalgUtenManuelleVurderinger(kontekst, grunnlag).mapValue { it.erOppfylt() }

        val manuelleVurderinger =
            grunnlag.medlemskapArbeidInntektGrunnlag?.gjeldendeManuelleVurderinger().orEmpty()

        return Tidslinje.map3(
            relevantTidslinje,
            manuelleVurderinger,
            automatiskVilkårsvurderingLovvalg
        ) { relevant, manuellVurdering, automatiskVurdering ->
            relevant == true && manuellVurdering == null && automatiskVurdering != true
        }.filter { it.verdi }.perioder().toSet()
    }

    override fun nårVurderingErRelevant(kontekst: FlytKontekstMedPerioder): Tidslinje<Boolean> {

        val grunnlag = hentFaktaGrunnlag(kontekst.sakId, kontekst.behandlingId)
        val tidligereVurderingsutfall = tidligereVurderinger.behandlingsutfall(kontekst, type())

        // TODO: Denne må erstattes av automatisk vurdering
        val automatiskVilkårsvurderingLovvalg = vilkårsvurderingLovvalgUtenManuelleVurderinger(kontekst, grunnlag)

        val manuelleVurderinger =
            grunnlag.medlemskapArbeidInntektGrunnlag?.gjeldendeManuelleVurderinger().orEmpty()
        val harManuelleVurderinger = manuelleVurderinger.isNotEmpty()

        val forrigeHaddeAvslagPåLovvalg = kontekst.forrigeBehandlingId?.let { forrigeId ->
            vilkårsresultatRepository.hent(forrigeId)
                .finnVilkår(Vilkårtype.LOVVALG)
                .vilkårsperioder()
                .lastOrNull()?.erOppfylt() == false
        } ?: false

        val tvingerRelevans = kontekst.vurderingsbehovRelevanteForSteg.any {
            it in vurderingsbehovSomGjørAtRevurderingSkalTvinges()
        } || (forrigeHaddeAvslagPåLovvalg && Vurderingsbehov.MOTTATT_SØKNAD in kontekst.vurderingsbehovRelevanteForSteg)

        return Tidslinje.map3(
            tidligereVurderingsutfall,
            manuelleVurderinger,
            automatiskVilkårsvurderingLovvalg
        ) { behandlingsutfall, manuellVurderingIPerioden, automatiskVilkårsvurdering ->
            val automatiskIkkeOppfylt = automatiskVilkårsvurdering?.erOppfylt() == false

            // harManuelleVurderinger skal fjernes når vi støtter kombinasjon av manuell og automatisk vurdering
            val manglerManuellDekning = harManuelleVurderinger && manuellVurderingIPerioden == null
            when (behandlingsutfall) {
                null -> false
                TidligereVurderinger.IkkeBehandlingsgrunnlag -> false
                is TidligereVurderinger.UunngåeligAvslag -> false
                is TidligereVurderinger.PotensieltOppfylt ->
                    automatiskIkkeOppfylt || tvingerRelevans || manglerManuellDekning
            }
        }

    }


    private fun vurderingsbehovSomGjørAtRevurderingSkalTvinges(): Set<Vurderingsbehov> =
        setOf(
            Vurderingsbehov.REVURDER_LOVVALG,
            Vurderingsbehov.LOVVALG_OG_MEDLEMSKAP,
        )

    private fun vilkårsvurderingLovvalgUtenManuelleVurderinger(
        kontekst: FlytKontekstMedPerioder,
        grunnlag: MedlemskapLovvalgFaktaGrunnlag,
    ): Tidslinje<Vilkårsvurdering> {
        val vilkårsresultat = Vilkårsresultat()
        val grunnlagUtenManuellVurdering = grunnlag.copy(
            medlemskapArbeidInntektGrunnlag = grunnlag.medlemskapArbeidInntektGrunnlag?.copy(
                vurderinger = emptyList()
            )
        )

        Medlemskapvilkåret(vilkårsresultat, kontekst.rettighetsperiode)
            .vurder(grunnlagUtenManuellVurdering)

        return vilkårsresultat.finnVilkår(Vilkårtype.LOVVALG).tidslinje()
    }

    private fun tilbakestillVurderinger(
        kontekst: FlytKontekstMedPerioder,
        grunnlag: MedlemskapLovvalgFaktaGrunnlag
    ) {
        val forrigeVurderinger = kontekst.forrigeBehandlingId?.let { forrigeBehandlingId ->
            medlemskapArbeidInntektRepository.hentHvisEksisterer(forrigeBehandlingId)
                ?.vurderinger
        } ?: emptyList()

        if (forrigeVurderinger.toSet() != grunnlag.medlemskapArbeidInntektGrunnlag?.vurderinger?.toSet()) {
            medlemskapArbeidInntektRepository.lagreVurderinger(
                kontekst.behandlingId,
                forrigeVurderinger,
            )
        }
    }

    private fun vurderingsbehovSomTvingerAvklaringsbehov(): Set<Vurderingsbehov> =
        setOf(Vurderingsbehov.REVURDER_LOVVALG, Vurderingsbehov.LOVVALG_OG_MEDLEMSKAP, Vurderingsbehov.MOTTATT_SØKNAD)

    private fun hentFaktaGrunnlag(sakId: SakId, behandlingId: BehandlingId): MedlemskapLovvalgFaktaGrunnlag {
        val medlemskapArbeidInntektGrunnlag =
            medlemskapArbeidInntektRepository.hentHvisEksisterer(behandlingId)
        val oppgittUtenlandsOppholdGrunnlag =
            medlemskapArbeidInntektRepository.hentOppgittUtenlandsOppholdHvisEksisterer(behandlingId)
                ?: medlemskapArbeidInntektRepository.hentSistRelevanteOppgitteUtenlandsOppholdHvisEksisterer(sakId)

        val brukerPersonopplysning = personopplysningRepository.hentBrukerPersonOpplysningHvisEksisterer(behandlingId)

        val grunnlag = MedlemskapLovvalgFaktaGrunnlag(
            medlemskapArbeidInntektGrunnlag,
            brukerPersonopplysning,
            oppgittUtenlandsOppholdGrunnlag,
            vurderBosattStatusOgNorskStatsborgerskap =
                unleashGateway.isEnabled(BehandlingsflytFeature.BosattStatsborgerskapGjennomslipp),
        )
        return grunnlag
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
