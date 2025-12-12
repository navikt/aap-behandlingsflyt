package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovService
import no.nav.aap.behandlingsflyt.behandling.beregning.BeregningService
import no.nav.aap.behandlingsflyt.behandling.inntektsbortfall.InntektsbortfallGrunnlag
import no.nav.aap.behandlingsflyt.behandling.inntektsbortfall.InntektsbortfallRepository
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderingerImpl
import no.nav.aap.behandlingsflyt.behandling.vilkår.inntektsbortfall.InntektsbortfallKanBehandlesAutomatisk
import no.nav.aap.behandlingsflyt.behandling.vilkår.inntektsbortfall.InntektsbortfallVilkår
import no.nav.aap.behandlingsflyt.behandling.vilkår.inntektsbortfall.InntektsbortfallVurderingService
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkår
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.InntektGrunnlagRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.ManuellInntektGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.ManuellInntektGrunnlagRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.PersonopplysningRepository
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider

class InntektsbortfallSteg private constructor(
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
    private val avklaringsbehovService: AvklaringsbehovService,
    private val unleashGateway: UnleashGateway,
    private val tidligereVurderinger: TidligereVurderinger,
    private val personopplysningRepository: PersonopplysningRepository,
    private val manuellInntektGrunnlagRepository: ManuellInntektGrunnlagRepository,
    private val inntektGrunnlagRepository: InntektGrunnlagRepository,
    private val vilkårsresultatRepository: VilkårsresultatRepository,
    private val inntektsbortfallRepository: InntektsbortfallRepository,
    private val beregningService: BeregningService
) : BehandlingSteg {
    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {

        val vilkårsPerioder = vurderVilkår(kontekst).vilkårsperioder()
        avklaringsbehovService.oppdaterAvklaringsbehov(
            avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId),
            definisjon = Definisjon.VURDER_INNTEKTSBORTFALL,
            vedtakBehøverVurdering = {
                when (kontekst.vurderingType) {
                    VurderingType.FØRSTEGANGSBEHANDLING,
                    VurderingType.REVURDERING -> {
                        when {
                            tidligereVurderinger.girAvslagEllerIngenBehandlingsgrunnlag(kontekst, type()) -> false
                            kontekst.vurderingsbehovRelevanteForSteg.isEmpty() -> false
                            kanBehandlesAutomatiskRespons(kontekst).kanBehandlesAutomatisk -> false
                            else -> true
                        }
                    }

                    VurderingType.MELDEKORT,
                    VurderingType.AUTOMATISK_BREV,
                    VurderingType.AUTOMATISK_OPPDATER_VILKÅR,
                    VurderingType.EFFEKTUER_AKTIVITETSPLIKT,
                    VurderingType.EFFEKTUER_AKTIVITETSPLIKT_11_9,
                    VurderingType.IKKE_RELEVANT ->
                        false
                }
            },
            erTilstrekkeligVurdert = {
                vilkårsPerioder.none { it.utfall == Utfall.IKKE_VURDERT }
            },
            tilbakestillGrunnlag = {
                // TODO: tilbakestill me
            },
            kontekst = kontekst
        )
        return Fullført
    }

    fun kanBehandlesAutomatiskRespons(kontekst: FlytKontekstMedPerioder): InntektsbortfallKanBehandlesAutomatisk {
        val brukerPersonopplysning =
            personopplysningRepository.hentBrukerPersonOpplysningHvisEksisterer(kontekst.behandlingId)
                ?: throw IllegalStateException("Forventet å finne personopplysninger")

        val manuellInntektGrunnlag = manuellInntektGrunnlagRepository.hentHvisEksisterer(kontekst.behandlingId)
        val inntektGrunnlag = inntektGrunnlagRepository.hentHvisEksisterer(kontekst.behandlingId)

        return InntektsbortfallVurderingService().vurderInntektsbortfall(
            kontekst,
            brukerPersonopplysning,
            manuellInntektGrunnlag ?: ManuellInntektGrunnlag(emptySet()),
            inntektGrunnlag,
            beregningService
        )
    }

    fun vurderVilkår(kontekst: FlytKontekstMedPerioder): Vilkår {
        val tidligereManuellVurdering = inntektsbortfallRepository.hentHvisEksisterer(kontekst.behandlingId)
        val kanBehandlesAutomatisk = kanBehandlesAutomatiskRespons(kontekst)

        val vilkårsresultat = vilkårsresultatRepository.hent(kontekst.behandlingId)
        InntektsbortfallVilkår(vilkårsresultat, kontekst.rettighetsperiode).vurder(
            InntektsbortfallGrunnlag(
                kanBehandlesAutomatisk,
                tidligereManuellVurdering
            )
        )
        vilkårsresultatRepository.lagre(kontekst.behandlingId, vilkårsresultat)
        return vilkårsresultat.finnVilkår(Vilkårtype.INNTEKTSBORTFALL)
    }

    companion object : FlytSteg {
        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): BehandlingSteg {
            return InntektsbortfallSteg(
                avklaringsbehovRepository = repositoryProvider.provide(),
                avklaringsbehovService = AvklaringsbehovService(repositoryProvider),
                unleashGateway = gatewayProvider.provide(),
                tidligereVurderinger = TidligereVurderingerImpl(repositoryProvider),
                personopplysningRepository = repositoryProvider.provide(),
                manuellInntektGrunnlagRepository = repositoryProvider.provide(),
                inntektGrunnlagRepository = repositoryProvider.provide(),
                vilkårsresultatRepository = repositoryProvider.provide(),
                inntektsbortfallRepository = repositoryProvider.provide(),
                beregningService = BeregningService(repositoryProvider),
            )
        }

        override fun type(): StegType {
            return StegType.VURDER_INNTEKTSBORTFALL
        }
    }
}