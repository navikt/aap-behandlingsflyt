package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
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
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.flyt.steg.oppdaterAvklaringsbehov
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import java.time.Year

class ManglendeLigningGrunnlagSteg internal constructor(
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
    private val inntektGrunnlagRepository: InntektGrunnlagRepository,
    private val manuellInntektGrunnlagRepository: ManuellInntektGrunnlagRepository,
    private val tidligereVurderinger: TidligereVurderinger,
    private val beregningService: BeregningService
) : BehandlingSteg {
    constructor(repositoryProvider: RepositoryProvider) : this(
        avklaringsbehovRepository = repositoryProvider.provide(),
        inntektGrunnlagRepository = repositoryProvider.provide(),
        manuellInntektGrunnlagRepository = repositoryProvider.provide(),
        tidligereVurderinger = TidligereVurderingerImpl(repositoryProvider),
        beregningService = BeregningService(repositoryProvider)
    )

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)
        val manuellInntektGrunnlag = manuellInntektGrunnlagRepository.hentHvisEksisterer(kontekst.behandlingId)
        val inntektGrunnlag = inntektGrunnlagRepository.hentHvisEksisterer(kontekst.behandlingId)

        oppdaterAvklaringsbehov(
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

                if (forrigeManuelleInntekter != manuellInntektGrunnlag?.manuelleInntekter) {
                   manuellInntektGrunnlagRepository.lagre(kontekst.behandlingId, forrigeManuelleInntekter)
                }
            }
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