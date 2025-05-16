package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.beregning.BeregningService
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderingerImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.InntektGrunnlagRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.ManuellInntektGrunnlagRepository
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FantAvklaringsbehov
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.lookup.repository.RepositoryProvider

class ManglendeLigningGrunnlagSteg private constructor(
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
    private val inntektGrunnlagRepository: InntektGrunnlagRepository,
    private val manuellInnektGrunnlagRepository: ManuellInntektGrunnlagRepository,
    private val tidligereVurderinger: TidligereVurderinger,
    private val beregningService: BeregningService
) : BehandlingSteg {
    constructor(repositoryProvider: RepositoryProvider) : this(
        avklaringsbehovRepository = repositoryProvider.provide(),
        inntektGrunnlagRepository = repositoryProvider.provide(),
        manuellInnektGrunnlagRepository = repositoryProvider.provide(),
        tidligereVurderinger = TidligereVurderingerImpl(repositoryProvider),
        beregningService = BeregningService(repositoryProvider)
    )

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        val behandlingId = kontekst.behandlingId
        val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(behandlingId)
        val avklaringsbehov = avklaringsbehovene.hentBehovForDefinisjon(Definisjon.FASTSETT_MANUELL_INNTEKT)

        if (tidligereVurderinger.girAvslagEllerIngenBehandlingsgrunnlag(kontekst, type())) {
            if (avklaringsbehov != null && avklaringsbehov.erÅpent()) avklaringsbehovene.avbryt(Definisjon.FASTSETT_MANUELL_INNTEKT)
            return Fullført
        }

        when (kontekst.vurdering.vurderingType) {
            VurderingType.FØRSTEGANGSBEHANDLING -> {
                if (tidligereVurderinger.girIngenBehandlingsgrunnlag(kontekst, BeregningAvklarFaktaSteg.type())) {
                    avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)
                        .avbrytForSteg(type())
                    return Fullført
                }
                return vurderInntekter(kontekst)
            }

            VurderingType.REVURDERING -> {
                // What do?
                // vurderInntekter(kontekst)?
            }

            VurderingType.MELDEKORT,
            VurderingType.IKKE_RELEVANT -> {
                // Always do nothing
            }
        }
        return Fullført
    }

    private fun vurderInntekter(kontekst: FlytKontekstMedPerioder): StegResultat {
        val inntektGrunnlag =
            requireNotNull(inntektGrunnlagRepository.hentHvisEksisterer(kontekst.behandlingId))
            { "Forventet å finne inntektsgrunnlag siden dette lagres i informasjonskravet." }

        val relevantBeregningsPeriode = beregningService.utledRelevanteBeregningsÅr(kontekst.behandlingId)
        val sisteRelevanteÅr = relevantBeregningsPeriode.max()

        val sisteÅrInntektGrunnlag = inntektGrunnlag.inntekter.firstOrNull { it.år == sisteRelevanteÅr }
        val manuellInntektGrunnlag = manuellInnektGrunnlagRepository.hentHvisEksisterer(kontekst.behandlingId)

        val harManuellInntektPåManglendeÅr =
            manuellInntektGrunnlag?.manuelleInntekter?.firstOrNull { sisteRelevanteÅr == it.år }

        if (sisteÅrInntektGrunnlag == null && harManuellInntektPåManglendeÅr == null) {
            return FantAvklaringsbehov(Definisjon.FASTSETT_MANUELL_INNTEKT)
        }

        return Fullført
    }

    companion object : FlytSteg {
        override fun konstruer(repositoryProvider: RepositoryProvider): BehandlingSteg {
            return ManglendeLigningGrunnlagSteg(repositoryProvider)
        }

        override fun type(): StegType {
            return StegType.MANGLENDE_LIGNING
        }
    }
}