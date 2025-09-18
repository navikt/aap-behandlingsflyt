package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.behandlingsflyt.behandling.beregning.BeregningService
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderingerImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.InntektGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.InntektGrunnlagRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.InntektPerÅr
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.ManuellInntektGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.ManuellInntektGrunnlagRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.ManuellInntektVurdering
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.EnkeltAvklaringsbehovstegService
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
import no.nav.aap.komponenter.miljo.Miljø.erProd
import no.nav.aap.lookup.repository.RepositoryProvider
import java.time.Year

class ManglendeLigningGrunnlagSteg internal constructor(
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
    private val inntektGrunnlagRepository: InntektGrunnlagRepository,
    private val manuellInntektGrunnlagRepository: ManuellInntektGrunnlagRepository,
    private val tidligereVurderinger: TidligereVurderinger,
    private val beregningService: BeregningService,
    private val erProd: Boolean,
    private val enkeltAvklaringsbehovstegService: EnkeltAvklaringsbehovstegService
) : BehandlingSteg {
    constructor(repositoryProvider: RepositoryProvider) : this(
        avklaringsbehovRepository = repositoryProvider.provide(),
        inntektGrunnlagRepository = repositoryProvider.provide(),
        manuellInntektGrunnlagRepository = repositoryProvider.provide(),
        tidligereVurderinger = TidligereVurderingerImpl(repositoryProvider),
        beregningService = BeregningService(repositoryProvider),
        erProd = erProd(),
        enkeltAvklaringsbehovstegService = EnkeltAvklaringsbehovstegService(repositoryProvider)
    )

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        if (erProd) {
            return gammelVariant(kontekst)
        }

        val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)
        val manuellInntektGrunnlag = manuellInntektGrunnlagRepository.hentHvisEksisterer(kontekst.behandlingId)
        val inntektGrunnlag = inntektGrunnlagRepository.hentHvisEksisterer(kontekst.behandlingId)

        enkeltAvklaringsbehovstegService.oppdaterAvklaringsbehov(
            avklaringsbehovene = avklaringsbehovene,
            definisjon = Definisjon.FASTSETT_MANUELL_INNTEKT,
            vedtakBehøverVurdering = {
                when (kontekst.vurderingType) {
                    VurderingType.FØRSTEGANGSBEHANDLING,
                    VurderingType.REVURDERING -> {
                        when {
                            tidligereVurderinger.girAvslagEllerIngenBehandlingsgrunnlag(kontekst, type()) -> false
                            kontekst.vurderingsbehovRelevanteForSteg.isEmpty() -> false
                            manueltTriggetVurderingsbehov(kontekst) -> true
                            else -> {
                                val sisteRelevanteÅr = hentSisteRelevanteÅr(kontekst)
                                val sisteÅrInntektGrunnlag = hentInntektGrunnlag(inntektGrunnlag, sisteRelevanteÅr)

                                // Behøver vurdering dersom inntekt for siste år mangler fra register
                                sisteÅrInntektGrunnlag == null
                            }
                        }
                    }

                    VurderingType.MELDEKORT,
                    VurderingType.EFFEKTUER_AKTIVITETSPLIKT,
                    VurderingType.IKKE_RELEVANT ->
                        false
                }
            },
            erTilstrekkeligVurdert = {
                val sisteRelevanteÅr = hentSisteRelevanteÅr(kontekst)
                val inntektGrunnlagSisteRelevanteÅr = hentInntektGrunnlag(inntektGrunnlag, sisteRelevanteÅr)
                val manuellInntektVurderingSisteRelevanteÅr = hentManuellInntektVurdering(manuellInntektGrunnlag, sisteRelevanteÅr)

                // Har enten inntekt fra register eller manuelt satt inntekt for siste relevante år
                inntektGrunnlagSisteRelevanteÅr != null || manuellInntektVurderingSisteRelevanteÅr != null
            },
            tilbakestillGrunnlag = {
                val forrigeManuelleInntekter = kontekst.forrigeBehandlingId?.let { forrigeBehandlingId ->
                    manuellInntektGrunnlagRepository.hentHvisEksisterer(forrigeBehandlingId)?.manuelleInntekter
                }.orEmpty()

                val gjeldendeManuelleInntekter = manuellInntektGrunnlag?.manuelleInntekter.orEmpty()

                if (forrigeManuelleInntekter != gjeldendeManuelleInntekter) {
                   manuellInntektGrunnlagRepository.lagre(kontekst.behandlingId, forrigeManuelleInntekter)
                }
            },
            kontekst
        )
        return Fullført
    }

    private fun manueltTriggetVurderingsbehov(kontekst: FlytKontekstMedPerioder): Boolean {
        return kontekst.vurderingsbehovRelevanteForSteg.any { it == Vurderingsbehov.REVURDER_MANUELL_INNTEKT }
    }

    private fun hentManuellInntektVurdering(manuellInntektGrunnlag: ManuellInntektGrunnlag?, sisteRelevanteÅr: Year): ManuellInntektVurdering? {
        return manuellInntektGrunnlag?.manuelleInntekter?.firstOrNull { it.år == sisteRelevanteÅr}
    }

    private fun hentInntektGrunnlag(inntektGrunnlag: InntektGrunnlag?, sisteRelevanteÅr: Year): InntektPerÅr? {
        checkNotNull(inntektGrunnlag) { "Forventet å finne inntektsgrunnlag siden dette lagres i informasjonskravet." }

        return inntektGrunnlag.inntekter.firstOrNull { it.år == sisteRelevanteÅr }
    }

    private fun hentSisteRelevanteÅr(kontekst: FlytKontekstMedPerioder): Year {
        val relevantBeregningsPeriode = beregningService.utledRelevanteBeregningsÅr(kontekst.behandlingId)
        return relevantBeregningsPeriode.max()
    }

    fun gammelVariant(kontekst: FlytKontekstMedPerioder): StegResultat {
        val behandlingId = kontekst.behandlingId
        val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(behandlingId)
        val avklaringsbehov = avklaringsbehovene.hentBehovForDefinisjon(Definisjon.FASTSETT_MANUELL_INNTEKT)

        if (tidligereVurderinger.girAvslagEllerIngenBehandlingsgrunnlag(kontekst, type())) {
            if (avklaringsbehov != null && avklaringsbehov.erÅpent()) avklaringsbehovene.avbryt(Definisjon.FASTSETT_MANUELL_INNTEKT)
            return Fullført
        }

        when (kontekst.vurderingType) {
            VurderingType.FØRSTEGANGSBEHANDLING -> {
                if (tidligereVurderinger.girIngenBehandlingsgrunnlag(kontekst, BeregningAvklarFaktaSteg.type())) {
                    avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)
                        .avbrytForSteg(type())
                    return Fullført
                }
                return vurderInntekter(kontekst)
            }

            VurderingType.REVURDERING -> {
                return revurderInntekter(avklaringsbehovene, kontekst)
            }

            VurderingType.MELDEKORT,
            VurderingType.EFFEKTUER_AKTIVITETSPLIKT,
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
        val manuellInntektGrunnlag = manuellInntektGrunnlagRepository.hentHvisEksisterer(kontekst.behandlingId)

        val harManuellInntektPåManglendeÅr =
            manuellInntektGrunnlag?.manuelleInntekter?.firstOrNull { sisteRelevanteÅr == it.år }
        if (sisteÅrInntektGrunnlag == null && harManuellInntektPåManglendeÅr == null) {
            return FantAvklaringsbehov(Definisjon.FASTSETT_MANUELL_INNTEKT)
        }

        return Fullført
    }

    private fun revurderInntekter(
        avklaringsbehovene: Avklaringsbehovene,
        kontekst: FlytKontekstMedPerioder
    ): StegResultat {
        val erIkkeVurdertTidligereIBehandlingen =
            !avklaringsbehovene.erVurdertTidligereIBehandlingen(Definisjon.FASTSETT_MANUELL_INNTEKT)
        val manuellInntektGrunnlag = manuellInntektGrunnlagRepository.hentHvisEksisterer(kontekst.behandlingId)

        if (erIkkeVurdertTidligereIBehandlingen || manuellInntektGrunnlag == null) {
            return FantAvklaringsbehov(Definisjon.FASTSETT_MANUELL_INNTEKT)
        }
        return Fullført
    }


    companion object : FlytSteg {
        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): BehandlingSteg {
            return ManglendeLigningGrunnlagSteg(repositoryProvider)
        }

        override fun type(): StegType {
            return StegType.MANGLENDE_LIGNING
        }
    }
}