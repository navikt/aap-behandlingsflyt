package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovService
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
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import org.slf4j.LoggerFactory
import java.time.Year

/**
 * Dette steget sjekker om vi mangler inntekt for året før nedsettelsesdatoen.
 */
class ManglendeLigningGrunnlagSteg internal constructor(
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
    private val inntektGrunnlagRepository: InntektGrunnlagRepository,
    private val manuellInntektGrunnlagRepository: ManuellInntektGrunnlagRepository,
    private val tidligereVurderinger: TidligereVurderinger,
    private val beregningService: BeregningService,
    private val avklaringsbehovService: AvklaringsbehovService
) : BehandlingSteg {
    constructor(repositoryProvider: RepositoryProvider) : this(
        avklaringsbehovRepository = repositoryProvider.provide(),
        inntektGrunnlagRepository = repositoryProvider.provide(),
        manuellInntektGrunnlagRepository = repositoryProvider.provide(),
        tidligereVurderinger = TidligereVurderingerImpl(repositoryProvider),
        beregningService = BeregningService(repositoryProvider),
        avklaringsbehovService = AvklaringsbehovService(repositoryProvider)
    )

    private val log = LoggerFactory.getLogger(javaClass)

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        val manuellInntektGrunnlag = manuellInntektGrunnlagRepository.hentHvisEksisterer(kontekst.behandlingId)
        val inntektGrunnlag = inntektGrunnlagRepository.hentHvisEksisterer(kontekst.behandlingId)

        avklaringsbehovService.oppdaterAvklaringsbehov(
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

                                val erVurdertManueltTidligere = hentManuellInntekterVurdering(
                                    manuellInntektGrunnlag,
                                    sisteRelevanteÅr
                                )?.isNotEmpty() == true

                                val harInntektIAlleRelevantÅrFraRegister =
                                    sisteRelevanteÅr.all { relevantÅr ->
                                        inntektGrunnlag?.inntekter?.map { it.år }
                                            ?.contains(relevantÅr) == true
                                    }

                                // Behøver vurdering dersom en inntekt for siste tre år mangler fra register
                                !harInntektIAlleRelevantÅrFraRegister || erVurdertManueltTidligere
                            }
                        }
                    }

                    VurderingType.MELDEKORT,
                    VurderingType.AUTOMATISK_BREV,
                    VurderingType.EFFEKTUER_AKTIVITETSPLIKT,
                    VurderingType.EFFEKTUER_AKTIVITETSPLIKT_11_9,
                    VurderingType.UTVID_VEDTAKSLENGDE,
                    VurderingType.MIGRER_RETTIGHETSPERIODE,
                    VurderingType.IKKE_RELEVANT ->
                        false
                }
            },
            erTilstrekkeligVurdert = {
                val sisteRelevanteÅr = hentSisteRelevanteÅr(kontekst)
                val inntektGrunnlagSisteRelevanteÅr = hentInntekterGrunnlag(inntektGrunnlag, sisteRelevanteÅr)
                val manuelleInntekterRelevanteÅr =
                    hentManuellInntekterVurdering(manuellInntektGrunnlag, sisteRelevanteÅr)
                val kombinerteÅr =
                    (inntektGrunnlagSisteRelevanteÅr.map { it.år } + manuelleInntekterRelevanteÅr.orEmpty()
                        .map { it.år }).toSet()

                // Har enten inntekt fra register eller manuelt satt inntekt for tre siste relevante år
                log.info("Siste relevante år: $sisteRelevanteÅr, kombinerte år: $kombinerteÅr")
                sisteRelevanteÅr.all { it in kombinerteÅr }
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

    private fun hentManuellInntekterVurdering(
        manuellInntektGrunnlag: ManuellInntektGrunnlag?,
        sisteRelevanteÅr: Set<Year>
    ): List<ManuellInntektVurdering>? {
        return manuellInntektGrunnlag?.manuelleInntekter?.filter { it.år in sisteRelevanteÅr }
    }

    private fun hentInntekterGrunnlag(
        inntektGrunnlag: InntektGrunnlag?,
        sisteRelevanteÅr: Set<Year>
    ): List<InntektPerÅr> {
        checkNotNull(inntektGrunnlag) { "Forventet å finne inntektsgrunnlag siden dette lagres i informasjonskravet." }

        return inntektGrunnlag.inntekter.filter { it.år in sisteRelevanteÅr }
    }

    private fun hentSisteRelevanteÅr(kontekst: FlytKontekstMedPerioder): Set<Year> {
        val relevantBeregningsPeriode = beregningService.utledRelevanteBeregningsÅr(kontekst.behandlingId)
        val sisteÅr = relevantBeregningsPeriode.max()

        val relevanteÅr = (0L..2L).map { sisteÅr.minusYears(it) }.toSet()

        return relevanteÅr
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
