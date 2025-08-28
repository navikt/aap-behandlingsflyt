package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.behandlingsflyt.behandling.lovvalg.ForutgåendeMedlemskapGrunnlag
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderingerImpl
import no.nav.aap.behandlingsflyt.behandling.vilkår.medlemskap.ForutgåendeMedlemskapvilkåret
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårService
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.MedlemskapArbeidInntektForutgåendeRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.MedlemskapArbeidInntektRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.PersonopplysningForutgåendeRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomRepository
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FantAvklaringsbehov
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import org.slf4j.LoggerFactory

class VurderForutgåendeMedlemskapSteg private constructor(
    private val vilkårsresultatRepository: VilkårsresultatRepository,
    private val forutgåendeMedlemskapArbeidInntektRepository: MedlemskapArbeidInntektForutgåendeRepository,
    private val medlemskapArbeidInntektRepository: MedlemskapArbeidInntektRepository,
    private val personopplysningForutgåendeRepository: PersonopplysningForutgåendeRepository,
    private val sykdomRepository: SykdomRepository,
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
    private val tidligereVurderinger: TidligereVurderinger,
    private val vilkårService: VilkårService,
) : BehandlingSteg {

    private val log = LoggerFactory.getLogger(javaClass)

    constructor(repositoryProvider: RepositoryProvider) : this(
        vilkårsresultatRepository = repositoryProvider.provide(),
        forutgåendeMedlemskapArbeidInntektRepository = repositoryProvider.provide(),
        medlemskapArbeidInntektRepository = repositoryProvider.provide(),
        personopplysningForutgåendeRepository = repositoryProvider.provide(),
        avklaringsbehovRepository = repositoryProvider.provide(),
        sykdomRepository = repositoryProvider.provide(),
        tidligereVurderinger = TidligereVurderingerImpl(repositoryProvider),
        vilkårService = VilkårService(repositoryProvider),
    )

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        val girAvslag = tidligereVurderinger.girAvslag(kontekst, type())

        if (girAvslag) {
            log.info("Gir avslag pga. tidligere vurderinger. Avbryter avklaringsbehov. BehandlingId: ${kontekst.behandlingId}")
            val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)
            val medlemskapBehov = avklaringsbehovene.hentBehovForDefinisjon(Definisjon.AVKLAR_FORUTGÅENDE_MEDLEMSKAP)
            if (medlemskapBehov != null && medlemskapBehov.erÅpent()) {
                avklaringsbehovene.avbryt(Definisjon.AVKLAR_FORUTGÅENDE_MEDLEMSKAP)
            }
            return Fullført
        }

        when (kontekst.vurderingType) {
            VurderingType.FØRSTEGANGSBEHANDLING -> {
                if (tidligereVurderinger.girIngenBehandlingsgrunnlag(kontekst, type())) {
                    avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)
                        .avbrytForSteg(type())
                    vilkårService.ingenNyeVurderinger(kontekst, Vilkårtype.MEDLEMSKAP, "mangler behandlingsgrunnlag")
                    return Fullført
                }

                return vurderVilkår(kontekst)
            }

            VurderingType.REVURDERING -> {
                return vurderVilkår(kontekst)
            }

            VurderingType.MELDEKORT,
            VurderingType.AKTIVITETSPLIKT,
            VurderingType.IKKE_RELEVANT -> {
                // Do nothing
            }
        }

        return Fullført
    }

    private fun vurderVilkår(kontekst: FlytKontekstMedPerioder): StegResultat {
        val vilkårsresultat = vilkårsresultatRepository.hent(kontekst.behandlingId)
        val manuellVurdering =
            forutgåendeMedlemskapArbeidInntektRepository.hentHvisEksisterer(kontekst.behandlingId)?.manuellVurdering
        val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)

        val sykdomGrunnlag = sykdomRepository.hent(kontekst.behandlingId)
        val harYrkesskadeSammenheng = sykdomGrunnlag.yrkesskadevurdering?.erÅrsakssammenheng
        if (harYrkesskadeSammenheng == true) {
            ForutgåendeMedlemskapvilkåret(
                vilkårsresultat,
                kontekst.rettighetsperiode
            ).leggTilYrkesskadeVurdering()
            vilkårsresultatRepository.lagre(kontekst.behandlingId, vilkårsresultat)
            return Fullført
        }

        if (kontekst.harNoeTilBehandling()) {
            val personopplysningForutgåendeGrunnlag =
                personopplysningForutgåendeRepository.hentHvisEksisterer(kontekst.behandlingId)
                    ?: throw IllegalStateException("Forventet å finne personopplysninger")

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
        val alleVilkårOppfylt =
            vilkårsresultatRepository.hent(kontekst.behandlingId).finnVilkår(Vilkårtype.MEDLEMSKAP).vilkårsperioder()
                .all { it.erOppfylt() }

        if ((!alleVilkårOppfylt && manuellVurdering == null)
            || spesifiktTriggetRevurderMedlemskapUtenManuellVurdering(kontekst, avklaringsbehovene)
        ) {
            return FantAvklaringsbehov(Definisjon.AVKLAR_FORUTGÅENDE_MEDLEMSKAP)
        }
        return Fullført
    }

    private fun spesifiktTriggetRevurderMedlemskapUtenManuellVurdering(
        kontekst: FlytKontekstMedPerioder,
        avklaringsbehovene: Avklaringsbehovene
    ): Boolean {
        val erSpesifiktTriggetRevurderMedlemskap =
            kontekst.vurderingsbehovRelevanteForSteg.any { it == Vurderingsbehov.REVURDER_MEDLEMSKAP || it == Vurderingsbehov.FORUTGAENDE_MEDLEMSKAP }
        return erSpesifiktTriggetRevurderMedlemskap && erIkkeVurdertTidligereIBehandlingen(avklaringsbehovene)
    }

    private fun erIkkeVurdertTidligereIBehandlingen(
        avklaringsbehovene: Avklaringsbehovene
    ): Boolean {
        return !avklaringsbehovene.erVurdertTidligereIBehandlingen(Definisjon.AVKLAR_FORUTGÅENDE_MEDLEMSKAP)
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
