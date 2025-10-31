package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovService
import no.nav.aap.behandlingsflyt.behandling.lovvalg.ForutgåendeMedlemskapArbeidInntektGrunnlag
import no.nav.aap.behandlingsflyt.behandling.lovvalg.ForutgåendeMedlemskapGrunnlag
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderingerImpl
import no.nav.aap.behandlingsflyt.behandling.vilkår.medlemskap.ForutgåendeMedlemskapVurderingService
import no.nav.aap.behandlingsflyt.behandling.vilkår.medlemskap.ForutgåendeMedlemskapvilkåret
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
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
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
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
) : BehandlingSteg {

    constructor(repositoryProvider: RepositoryProvider) : this(
        vilkårsresultatRepository = repositoryProvider.provide(),
        forutgåendeMedlemskapArbeidInntektRepository = repositoryProvider.provide(),
        medlemskapArbeidInntektRepository = repositoryProvider.provide(),
        personopplysningForutgåendeRepository = repositoryProvider.provide(),
        avklaringsbehovRepository = repositoryProvider.provide(),
        sykdomRepository = repositoryProvider.provide(),
        tidligereVurderinger = TidligereVurderingerImpl(repositoryProvider),
        avklaringsbehovService = AvklaringsbehovService(repositoryProvider),
    )

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        val grunnlag = lazy { forutgåendeMedlemskapArbeidInntektRepository.hentHvisEksisterer(kontekst.behandlingId) }

        avklaringsbehovService.oppdaterAvklaringsbehov(
            avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId),
            definisjon = Definisjon.AVKLAR_FORUTGÅENDE_MEDLEMSKAP,
            vedtakBehøverVurdering = { vedtakBehøverVurdering(kontekst) },
            erTilstrekkeligVurdert = {
                val manuellVurdering =
                    forutgåendeMedlemskapArbeidInntektRepository.hentHvisEksisterer(kontekst.behandlingId)?.manuellVurdering
                manuellVurdering != null
            },
            tilbakestillGrunnlag = { tilbakestillGrunnlag(kontekst, grunnlag.value) },
            kontekst = kontekst,
        )

        when (kontekst.vurderingType) {
            VurderingType.FØRSTEGANGSBEHANDLING,
            VurderingType.REVURDERING -> {
                vurderVilkår(kontekst)
            }

            VurderingType.EFFEKTUER_AKTIVITETSPLIKT,
            VurderingType.EFFEKTUER_AKTIVITETSPLIKT_11_9,
            VurderingType.MELDEKORT,
            VurderingType.IKKE_RELEVANT -> {
            }
        }

        return Fullført
    }

    private fun tilbakestillGrunnlag(
        kontekst: FlytKontekstMedPerioder,
        grunnlag: ForutgåendeMedlemskapArbeidInntektGrunnlag?
    ) {
        val forrigeManuelleVurdering = kontekst.forrigeBehandlingId?.let { forrigeBehandlingId ->
            forutgåendeMedlemskapArbeidInntektRepository.hentHvisEksisterer(forrigeBehandlingId)
                ?.manuellVurdering
        }
        if (forrigeManuelleVurdering != grunnlag?.manuellVurdering) {
            forutgåendeMedlemskapArbeidInntektRepository.lagreManuellVurdering(
                kontekst.behandlingId,
                forrigeManuelleVurdering
            )
        }

        // Tilbakestill vilkårsvurderinger
        val forrigeVilkårsvurderinger =
            kontekst.forrigeBehandlingId
                ?.let { vilkårsresultatRepository.hent(it).optionalVilkår(Vilkårtype.MEDLEMSKAP) }
                ?.tidslinje()
                .orEmpty()

        val vilkårsresultat = vilkårsresultatRepository.hent(kontekst.behandlingId)
        val vilkår = vilkårsresultat.optionalVilkår(Vilkårtype.MEDLEMSKAP)
        if (vilkår != null) {
            vilkår.nullstillTidslinje()
            vilkår.leggTilVurderinger(forrigeVilkårsvurderinger)
            vilkårsresultatRepository.lagre(kontekst.behandlingId, vilkårsresultat)
        }
    }

    private fun vedtakBehøverVurdering(
        kontekst: FlytKontekstMedPerioder
    ): Boolean {
        val vurderingFraForrigeBehandling = kontekst.forrigeBehandlingId?.let { forrigeBehandlingId ->
            forutgåendeMedlemskapArbeidInntektRepository.hentHvisEksisterer(forrigeBehandlingId)
                ?.manuellVurdering
        }

        return when (kontekst.vurderingType) {
            VurderingType.FØRSTEGANGSBEHANDLING, VurderingType.REVURDERING -> {
                when {
                    tidligereVurderinger.girAvslagEllerIngenBehandlingsgrunnlag(kontekst, type()) -> false
                    harYrkesskadeSammenheng(kontekst) -> false
                    spesifiktTriggetRevurderMedlemskap(kontekst) -> true
                    !kanBehandlesAutomatisk(kontekst) && vurderingFraForrigeBehandling == null -> true
                    else -> false
                }
            }

            VurderingType.MELDEKORT,
            VurderingType.EFFEKTUER_AKTIVITETSPLIKT,
            VurderingType.EFFEKTUER_AKTIVITETSPLIKT_11_9,
            VurderingType.IKKE_RELEVANT -> {
                false
            }
        }
    }

    private fun harYrkesskadeSammenheng(kontekst: FlytKontekstMedPerioder): Boolean {
        val vilkårsresultat = vilkårsresultatRepository.hent(kontekst.behandlingId)
        val sykdomGrunnlag = sykdomRepository.hent(kontekst.behandlingId)
        val harYrkesskadeSammenheng = sykdomGrunnlag.yrkesskadevurdering?.erÅrsakssammenheng
        if (harYrkesskadeSammenheng == true) {
            ForutgåendeMedlemskapvilkåret(
                vilkårsresultat,
                kontekst.rettighetsperiode
            ).leggTilYrkesskadeVurdering()
            vilkårsresultatRepository.lagre(kontekst.behandlingId, vilkårsresultat)
            return true
        }
        return false
    }

    private fun vurderVilkår(kontekst: FlytKontekstMedPerioder) {
        val vilkårsresultat = vilkårsresultatRepository.hent(kontekst.behandlingId)
        if (kontekst.harNoeTilBehandling()) {
            val personopplysningForutgåendeGrunnlag =
                personopplysningForutgåendeRepository.hentHvisEksisterer(kontekst.behandlingId)

            val forutgåendeMedlemskapArbeidInntektGrunnlag =
                forutgåendeMedlemskapArbeidInntektRepository.hentHvisEksisterer(kontekst.behandlingId)
            val oppgittUtenlandsOppholdGrunnlag =
                medlemskapArbeidInntektRepository.hentOppgittUtenlandsOppholdHvisEksisterer(kontekst.behandlingId)
                    ?: medlemskapArbeidInntektRepository.hentSistRelevanteOppgitteUtenlandsOppholdHvisEksisterer(
                        kontekst.sakId
                    )

            ForutgåendeMedlemskapvilkåret(vilkårsresultat, kontekst.rettighetsperiode).vurder(
                ForutgåendeMedlemskapGrunnlag(
                    forutgåendeMedlemskapArbeidInntektGrunnlag,
                    personopplysningForutgåendeGrunnlag,
                    oppgittUtenlandsOppholdGrunnlag
                )
            )
            vilkårsresultatRepository.lagre(kontekst.behandlingId, vilkårsresultat)
        }
    }

    private fun kanBehandlesAutomatisk(kontekst: FlytKontekstMedPerioder): Boolean {
        val personopplysningForutgåendeGrunnlag =
            personopplysningForutgåendeRepository.hentHvisEksisterer(kontekst.behandlingId)

        val forutgåendeMedlemskapArbeidInntektGrunnlag =
            forutgåendeMedlemskapArbeidInntektRepository.hentHvisEksisterer(kontekst.behandlingId)
        val oppgittUtenlandsOppholdGrunnlag =
            medlemskapArbeidInntektRepository.hentOppgittUtenlandsOppholdHvisEksisterer(kontekst.behandlingId)
                ?: medlemskapArbeidInntektRepository.hentSistRelevanteOppgitteUtenlandsOppholdHvisEksisterer(
                    kontekst.sakId
                )

        val grunnlag = ForutgåendeMedlemskapGrunnlag(
            forutgåendeMedlemskapArbeidInntektGrunnlag,
            personopplysningForutgåendeGrunnlag,
            oppgittUtenlandsOppholdGrunnlag
        )

        return ForutgåendeMedlemskapVurderingService().vurderTilhørighet(
            grunnlag,
            kontekst.rettighetsperiode
        ).kanBehandlesAutomatisk
    }

    private fun spesifiktTriggetRevurderMedlemskap(
        kontekst: FlytKontekstMedPerioder
    ): Boolean {
        return kontekst.vurderingsbehovRelevanteForSteg.any { it == Vurderingsbehov.REVURDER_MEDLEMSKAP || it == Vurderingsbehov.FORUTGAENDE_MEDLEMSKAP }
    }

    companion object : FlytSteg {
        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): BehandlingSteg {
            return VurderForutgåendeMedlemskapSteg(repositoryProvider)
        }

        override fun type(): StegType {
            return StegType.VURDER_MEDLEMSKAP
        }
    }
}