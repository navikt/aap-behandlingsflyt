package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovService
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.ÅrsakTilSettPåVent
import no.nav.aap.behandlingsflyt.behandling.lovvalg.MedlemskapLovvalgGrunnlag
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderingerImpl
import no.nav.aap.behandlingsflyt.behandling.vilkår.medlemskap.Medlemskapvilkåret
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Avslagsårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårService
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.MedlemskapArbeidInntektRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.PersonopplysningRepository
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FantAvklaringsbehov
import no.nav.aap.behandlingsflyt.flyt.steg.FantVentebehov
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.flyt.steg.Ventebehov
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.ÅrsakTilBehandling
import no.nav.aap.lookup.repository.RepositoryProvider

class VurderLovvalgSteg private constructor(
    private val vilkårsresultatRepository: VilkårsresultatRepository,
    private val personopplysningRepository: PersonopplysningRepository,
    private val medlemskapArbeidInntektRepository: MedlemskapArbeidInntektRepository,
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
    private val tidligereVurderinger: TidligereVurderinger,
    private val avklaringsbehovService: AvklaringsbehovService,
    private val vilkårService: VilkårService,
) : BehandlingSteg {
    constructor(repositoryProvider: RepositoryProvider) : this(
        vilkårsresultatRepository = repositoryProvider.provide(),
        personopplysningRepository = repositoryProvider.provide(),
        medlemskapArbeidInntektRepository = repositoryProvider.provide(),
        avklaringsbehovRepository = repositoryProvider.provide(),
        tidligereVurderinger = TidligereVurderingerImpl(repositoryProvider),
        avklaringsbehovService = AvklaringsbehovService(repositoryProvider),
        vilkårService = VilkårService(repositoryProvider),
    )


    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        when (kontekst.vurderingType) {
            VurderingType.FØRSTEGANGSBEHANDLING -> {
                if (tidligereVurderinger.girAvslagEllerIngenBehandlingsgrunnlag(kontekst, type())) {
                    avklaringsbehovService.avbrytForSteg(kontekst.behandlingId, type())
                    vilkårService.ingenNyeVurderinger(
                        kontekst,
                        Vilkårtype.LOVVALG,
                        "mangler behandlingsgrunnlag",
                    )
                    return Fullført
                }
                return vurderVilkår(kontekst)
            }

            VurderingType.REVURDERING -> {
                return vurderVilkår(kontekst)
            }

            VurderingType.MELDEKORT,
            VurderingType.IKKE_RELEVANT -> {
                // Do nothing
                return Fullført
            }
        }
    }

    private fun vurderVilkår(kontekst: FlytKontekstMedPerioder): StegResultat {
        val manuellVurdering =
            medlemskapArbeidInntektRepository.hentHvisEksisterer(kontekst.behandlingId)?.manuellVurdering
        val vilkårsresultat = vilkårsresultatRepository.hent(kontekst.behandlingId)
        val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)

        if (kontekst.harNoeTilBehandling()) {
            val rettighetsperiode = kontekst.rettighetsperiode
            val personopplysningGrunnlag = personopplysningRepository.hentHvisEksisterer(kontekst.behandlingId)
                ?: throw IllegalStateException("Forventet å finne personopplysninger")
            val medlemskapArbeidInntektGrunnlag =
                medlemskapArbeidInntektRepository.hentHvisEksisterer(kontekst.behandlingId)
            val oppgittUtenlandsOppholdGrunnlag =
                medlemskapArbeidInntektRepository.hentOppgittUtenlandsOppholdHvisEksisterer(kontekst.behandlingId)
                    ?: medlemskapArbeidInntektRepository.hentSistRelevanteOppgitteUtenlandsOppholdHvisEksisterer(
                        kontekst.sakId
                    )

            Medlemskapvilkåret(vilkårsresultat, rettighetsperiode).vurder(
                MedlemskapLovvalgGrunnlag(
                    medlemskapArbeidInntektGrunnlag,
                    personopplysningGrunnlag,
                    oppgittUtenlandsOppholdGrunnlag
                )
            )
            vilkårsresultatRepository.lagre(kontekst.behandlingId, vilkårsresultat)
        }

        val måOverføresTilAnnetLand =
            vilkårsresultatRepository.hent(kontekst.behandlingId).finnVilkår(Vilkårtype.LOVVALG).vilkårsperioder()
                .any { it.avslagsårsak == Avslagsårsak.NORGE_IKKE_KOMPETENT_STAT }
        val alleVilkårOppfylt =
            vilkårsresultatRepository.hent(kontekst.behandlingId).finnVilkår(Vilkårtype.LOVVALG).vilkårsperioder()
                .all { it.erOppfylt() }

        if (måOverføresTilAnnetLand) {
            return FantVentebehov(
                Ventebehov(
                    definisjon = Definisjon.VENTE_PÅ_UTENLANDSK_VIDEREFØRING_AVKLARING,
                    grunn = ÅrsakTilSettPåVent.VENTER_PÅ_UTENLANDSK_VIDEREFORING_AVKLARING
                )
            )
        }
        if (!alleVilkårOppfylt && manuellVurdering == null
            || spesifiktTriggetRevurderLovvalgUtenManuellVurdering(kontekst, avklaringsbehovene)
        ) {
            return FantAvklaringsbehov(Definisjon.AVKLAR_LOVVALG_MEDLEMSKAP)
        }
        return Fullført
    }

    private fun spesifiktTriggetRevurderLovvalgUtenManuellVurdering(
        kontekst: FlytKontekstMedPerioder,
        avklaringsbehovene: Avklaringsbehovene
    ): Boolean {
        val erSpesifiktTriggetRevurderLovvalg =
            kontekst.årsakerTilBehandling.any { it == ÅrsakTilBehandling.REVURDER_LOVVALG || it == ÅrsakTilBehandling.LOVVALG_OG_MEDLEMSKAP }
        return erSpesifiktTriggetRevurderLovvalg && erIkkeVurdertTidligereIBehandlingen(avklaringsbehovene)
    }

    private fun erIkkeVurdertTidligereIBehandlingen(
        avklaringsbehovene: Avklaringsbehovene
    ): Boolean {
        return !avklaringsbehovene.erVurdertTidligereIBehandlingen(Definisjon.AVKLAR_LOVVALG_MEDLEMSKAP)
    }

    companion object : FlytSteg {
        override fun konstruer(repositoryProvider: RepositoryProvider): BehandlingSteg {
            return VurderLovvalgSteg(repositoryProvider)
        }

        override fun type(): StegType {
            return StegType.VURDER_LOVVALG
        }
    }
}
