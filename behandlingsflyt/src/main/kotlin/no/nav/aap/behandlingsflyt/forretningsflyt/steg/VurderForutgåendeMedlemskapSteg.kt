package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovService
import no.nav.aap.behandlingsflyt.behandling.lovvalg.ForutgåendeMedlemskapArbeidInntektGrunnlag
import no.nav.aap.behandlingsflyt.behandling.lovvalg.ForutgåendeMedlemskapGrunnlag
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderingerImpl
import no.nav.aap.behandlingsflyt.behandling.vilkår.medlemskap.ForutgåendeMedlemskapVurderingService
import no.nav.aap.behandlingsflyt.behandling.vilkår.medlemskap.ForutgåendeMedlemskapvilkåret
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsresultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.MedlemskapArbeidInntektForutgåendeRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.MedlemskapArbeidInntektRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.PersonopplysningForutgåendeRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomRepository
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.orEmpty
import no.nav.aap.lookup.repository.RepositoryProvider

class VurderForutgåendeMedlemskapSteg private constructor(
    private val vilkårsresultatRepository: VilkårsresultatRepository,
    private val forutgåendeMedlemskapArbeidInntektRepository: MedlemskapArbeidInntektForutgåendeRepository,
    private val medlemskapArbeidInntektRepository: MedlemskapArbeidInntektRepository,
    private val personopplysningForutgåendeRepository: PersonopplysningForutgåendeRepository,
    private val sykdomRepository: SykdomRepository,
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
    private val tidligereVurderinger: TidligereVurderinger,
    private val avklaringsbehovService: AvklaringsbehovService,
    private val behandlingRepository: BehandlingRepository,
) : BehandlingSteg {

    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        vilkårsresultatRepository = repositoryProvider.provide(),
        forutgåendeMedlemskapArbeidInntektRepository = repositoryProvider.provide(),
        medlemskapArbeidInntektRepository = repositoryProvider.provide(),
        personopplysningForutgåendeRepository = repositoryProvider.provide(),
        avklaringsbehovRepository = repositoryProvider.provide(),
        sykdomRepository = repositoryProvider.provide(),
        tidligereVurderinger = TidligereVurderingerImpl(repositoryProvider),
        avklaringsbehovService = AvklaringsbehovService(repositoryProvider),
        behandlingRepository = repositoryProvider.provide(),
    )

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        val grunnlag = lazy { hentGrunnlag(kontekst.sakId, kontekst.behandlingId) }

        avklaringsbehovService.oppdaterAvklaringsbehovForPeriodisertYtelsesvilkår(
            avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId),
            behandlingRepository = behandlingRepository,
            definisjon = Definisjon.AVKLAR_FORUTGÅENDE_MEDLEMSKAP,
            vilkårsresultatRepository = vilkårsresultatRepository,
            tvingerAvklaringsbehov = setOf(Vurderingsbehov.REVURDER_MEDLEMSKAP, Vurderingsbehov.FORUTGAENDE_MEDLEMSKAP),
            nårVurderingErRelevant = { nyKontekst -> nårVurderingErRelevant(nyKontekst, grunnlag.value) },
            nårVurderingErGyldig = { nårVurderingErGyldig(kontekst, grunnlag.value) },
            kontekst = kontekst,
            tilbakestillGrunnlag = { tilbakestillGrunnlagNy(kontekst, grunnlag.value.medlemskapArbeidInntektGrunnlag) },
        )

        when (kontekst.vurderingType) {
            VurderingType.FØRSTEGANGSBEHANDLING,
            VurderingType.REVURDERING -> {
                vurderVilkår(kontekst, grunnlag.value)
            }
            VurderingType.EFFEKTUER_AKTIVITETSPLIKT,
            VurderingType.EFFEKTUER_AKTIVITETSPLIKT_11_9,
            VurderingType.MELDEKORT,
            VurderingType.IKKE_RELEVANT -> {}
        }

        return Fullført
    }

    private fun tilbakestillGrunnlagNy(
        kontekst: FlytKontekstMedPerioder,
        grunnlag: ForutgåendeMedlemskapArbeidInntektGrunnlag?
    ) {
        val forrigeVurderinger = kontekst.forrigeBehandlingId?.let { forrigeBehandlingId ->
            forutgåendeMedlemskapArbeidInntektRepository.hentHvisEksisterer(forrigeBehandlingId)
                ?.vurderinger
        } ?: emptyList()

        if (forrigeVurderinger.toSet() != grunnlag?.vurderinger?.toSet()) {
            forutgåendeMedlemskapArbeidInntektRepository.lagreVurderinger(
                kontekst.behandlingId,
                forrigeVurderinger,
            )
        }
    }

    private fun harYrkesskadeSammenheng(kontekst: FlytKontekstMedPerioder): Boolean {
        val sykdomGrunnlag = sykdomRepository.hentHvisEksisterer(kontekst.behandlingId)
        val harYrkesskadeSammenheng = sykdomGrunnlag?.yrkesskadevurdering?.erÅrsakssammenheng
        return harYrkesskadeSammenheng == true
    }

    private fun vurderVilkår(kontekst: FlytKontekstMedPerioder, grunnlag: ForutgåendeMedlemskapGrunnlag) {
        val vilkårsresultat = vilkårsresultatRepository.hent(kontekst.behandlingId)

        if (kontekst.harNoeTilBehandling()) {
            if (harYrkesskadeSammenheng(kontekst)) {
                ForutgåendeMedlemskapvilkåret(
                    vilkårsresultat,
                    kontekst.rettighetsperiode,
                ).leggTilYrkesskadeVurdering()
                vilkårsresultatRepository.lagre(kontekst.behandlingId, vilkårsresultat)

            } else {
                ForutgåendeMedlemskapvilkåret(vilkårsresultat, kontekst.rettighetsperiode).vurder(grunnlag)
                vilkårsresultatRepository.lagre(kontekst.behandlingId, vilkårsresultat)
            }
        }
    }

    private fun nårVurderingErRelevant(
        kontekst: FlytKontekstMedPerioder,
        grunnlag: ForutgåendeMedlemskapGrunnlag
    ): Tidslinje<Boolean> {
        val tidligereVurderingsutfall = tidligereVurderinger.behandlingsutfall(kontekst, type())
        val automatiskVilkårsvurderingForutgåendeMedlemskap = vilkårsvurderingForutgåendeMedlemskapUtenManuelleVurderinger(kontekst, grunnlag)

        return Tidslinje.zip2(tidligereVurderingsutfall, automatiskVilkårsvurderingForutgåendeMedlemskap)
            .mapValue { (behandlingsutfall, automatiskVilkårsvurderingLovvalg) ->
                when (behandlingsutfall) {
                    null -> false
                    TidligereVurderinger.Behandlingsutfall.IKKE_BEHANDLINGSGRUNNLAG -> false
                    TidligereVurderinger.Behandlingsutfall.UUNGÅELIG_AVSLAG -> false
                    TidligereVurderinger.Behandlingsutfall.UKJENT -> {
                        val automatiskVilkårsvurderingForutgåendeMedlemskapIkkeOppfylt =
                            automatiskVilkårsvurderingLovvalg?.erOppfylt() == false

                        // Må gjøres slik for å trigge overstyrt avklaringsbehov hvis allerede automatisk oppfylt
                        val tvingerAvklaringsbehov = kontekst.vurderingsbehovRelevanteForSteg.any {
                            it in vurderingsbehovSomTvingerAvklaringsbehov()
                        }

                        automatiskVilkårsvurderingForutgåendeMedlemskapIkkeOppfylt || tvingerAvklaringsbehov
                    }
                }
            }
    }

    private fun nårVurderingErGyldig(
        kontekst: FlytKontekstMedPerioder,
        grunnlag: ForutgåendeMedlemskapGrunnlag
    ): Tidslinje<Boolean> {
        val automatiskVilkårsvurderingLovvalg = vilkårsvurderingForutgåendeMedlemskapUtenManuelleVurderinger(kontekst, grunnlag).mapValue { it.erOppfylt() }
        val automatiskVurderingOppfylt = automatiskVilkårsvurderingLovvalg.filter { it.verdi }.isNotEmpty()
        if (automatiskVurderingOppfylt) {
            return automatiskVilkårsvurderingLovvalg
        }

        // Automatisk vurdering er ikke oppfylt - trenger manuell vurdering
        return grunnlag.medlemskapArbeidInntektGrunnlag?.gjeldendeVurderinger().orEmpty().mapValue { true }
    }

    private fun vilkårsvurderingForutgåendeMedlemskapUtenManuelleVurderinger(
        kontekst: FlytKontekstMedPerioder,
        grunnlag: ForutgåendeMedlemskapGrunnlag,
    ): Tidslinje<Vilkårsvurdering> {
        val vilkårsresultat = Vilkårsresultat()
        val grunnlagUtenManuellVurdering = grunnlag.copy(
            medlemskapArbeidInntektGrunnlag = grunnlag.medlemskapArbeidInntektGrunnlag?.copy(
                vurderinger = emptyList()
            )
        )

        if (harYrkesskadeSammenheng(kontekst)) {
            ForutgåendeMedlemskapvilkåret(
                vilkårsresultat,
                kontekst.rettighetsperiode,
            ).leggTilYrkesskadeVurdering()
        } else {
            ForutgåendeMedlemskapvilkåret(vilkårsresultat, kontekst.rettighetsperiode)
                .vurder(grunnlagUtenManuellVurdering)
        }

        return vilkårsresultat.finnVilkår(Vilkårtype.MEDLEMSKAP).tidslinje()
    }

    private fun hentGrunnlag(sakId: SakId, behandlingId: BehandlingId): ForutgåendeMedlemskapGrunnlag {
        val personopplysningForutgåendeGrunnlag =
            personopplysningForutgåendeRepository.hentHvisEksisterer(behandlingId)

        val forutgåendeMedlemskapArbeidInntektGrunnlag =
            forutgåendeMedlemskapArbeidInntektRepository.hentHvisEksisterer(behandlingId)
        val oppgittUtenlandsOppholdGrunnlag =
            medlemskapArbeidInntektRepository.hentOppgittUtenlandsOppholdHvisEksisterer(behandlingId)
                ?: medlemskapArbeidInntektRepository.hentSistRelevanteOppgitteUtenlandsOppholdHvisEksisterer(
                    sakId
                )

        val grunnlag = ForutgåendeMedlemskapGrunnlag(
            forutgåendeMedlemskapArbeidInntektGrunnlag,
            personopplysningForutgåendeGrunnlag,
            oppgittUtenlandsOppholdGrunnlag
        )
        return grunnlag
    }

    private fun vurderingsbehovSomTvingerAvklaringsbehov(): Set<Vurderingsbehov> =
        setOf(Vurderingsbehov.REVURDER_MEDLEMSKAP, Vurderingsbehov.FORUTGAENDE_MEDLEMSKAP)

    companion object : FlytSteg {
        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): BehandlingSteg {
            return VurderForutgåendeMedlemskapSteg(repositoryProvider, gatewayProvider)
        }

        override fun type(): StegType {
            return StegType.VURDER_MEDLEMSKAP
        }
    }
}