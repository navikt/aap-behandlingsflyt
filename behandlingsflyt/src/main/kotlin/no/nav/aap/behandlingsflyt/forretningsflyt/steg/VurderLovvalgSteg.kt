package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.ÅrsakTilSettPåVent
import no.nav.aap.behandlingsflyt.behandling.lovvalg.MedlemskapLovvalgGrunnlag
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderingerImpl
import no.nav.aap.behandlingsflyt.behandling.vilkår.medlemskap.Medlemskapvilkåret
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Avslagsårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsresultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.MedlemskapArbeidInntektRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.PersonopplysningRepository
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FantVentebehov
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.flyt.steg.Ventebehov
import no.nav.aap.behandlingsflyt.flyt.steg.oppdaterAvklaringsbehov
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider

class VurderLovvalgSteg private constructor(
    private val vilkårsresultatRepository: VilkårsresultatRepository,
    private val personopplysningRepository: PersonopplysningRepository,
    private val medlemskapArbeidInntektRepository: MedlemskapArbeidInntektRepository,
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
    private val tidligereVurderinger: TidligereVurderinger,
) : BehandlingSteg {
    constructor(repositoryProvider: RepositoryProvider) : this(
        vilkårsresultatRepository = repositoryProvider.provide(),
        personopplysningRepository = repositoryProvider.provide(),
        medlemskapArbeidInntektRepository = repositoryProvider.provide(),
        avklaringsbehovRepository = repositoryProvider.provide(),
        tidligereVurderinger = TidligereVurderingerImpl(repositoryProvider),
    )

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        val grunnlag = lazy { hentGrunnlag(kontekst.sakId, kontekst.behandlingId) }
        val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)

        oppdaterAvklaringsbehov(
            avklaringsbehovene = avklaringsbehovene,
            definisjon = Definisjon.AVKLAR_LOVVALG_MEDLEMSKAP,
            vedtakBehøverVurdering = { vedtakBehøverVurdering(kontekst, grunnlag, avklaringsbehovene) },
            erTilstrekkeligVurdert = { grunnlag.value.medlemskapArbeidInntektGrunnlag?.manuellVurdering != null },
            tilbakestillGrunnlag = { tilbakestillGrunnlag(kontekst, grunnlag.value) },
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
            medlemskapArbeidInntektRepository.lagreManuellVurdering(
                kontekst.behandlingId,
                forrigeManuelleVurdering,
            )
        }
    }

    private fun norgeIkkeKompetentStat(kontekst: FlytKontekstMedPerioder): Boolean =
        vilkårsresultatRepository.hent(kontekst.behandlingId)
            .finnVilkår(Vilkårtype.LOVVALG)
            .vilkårsperioder()
            .any { it.avslagsårsak == Avslagsårsak.NORGE_IKKE_KOMPETENT_STAT }

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
                        manuellVurdering = null
                    )
                )
                Medlemskapvilkåret(vilkårsresultat, kontekst.rettighetsperiode)
                    .vurder(grunnlagUtenManuellVurdering)
                vilkårsresultat.finnVilkår(Vilkårtype.LOVVALG).harPerioderSomIkkeErOppfylt()
            }

            VurderingType.MELDEKORT -> false
            VurderingType.IKKE_RELEVANT -> false
            VurderingType.EFFEKTUER_AKTIVITETSPLIKT -> false
        }
    }

    private fun manueltTriggetLøsning(avklaringsbehovene: Avklaringsbehovene): Boolean {
        val avklaringsbehov = avklaringsbehovene.hentBehovForDefinisjon(Definisjon.AVKLAR_LOVVALG_MEDLEMSKAP)
        return avklaringsbehov != null
    }

    private fun manueltTriggetVurderingsbehov(kontekst: FlytKontekstMedPerioder): Boolean {
        return kontekst.vurderingsbehovRelevanteForSteg
            .any { it == Vurderingsbehov.REVURDER_LOVVALG || it == Vurderingsbehov.LOVVALG_OG_MEDLEMSKAP }
    }

    private fun hentGrunnlag(sakId: SakId, behandlingId: BehandlingId): MedlemskapLovvalgGrunnlag {
        val medlemskapArbeidInntektGrunnlag =
            medlemskapArbeidInntektRepository.hentHvisEksisterer(behandlingId)
        val oppgittUtenlandsOppholdGrunnlag =
            medlemskapArbeidInntektRepository.hentOppgittUtenlandsOppholdHvisEksisterer(behandlingId)
                ?: medlemskapArbeidInntektRepository.hentSistRelevanteOppgitteUtenlandsOppholdHvisEksisterer(sakId)

        val brukerPersonopplysning = personopplysningRepository.hentBrukerPersonOpplysningHvisEksisterer(behandlingId)
            ?: throw IllegalStateException("Forventet å finne personopplysninger")

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
            return VurderLovvalgSteg(repositoryProvider)
        }

        override fun type(): StegType {
            return StegType.VURDER_LOVVALG
        }
    }
}
