package no.nav.aap.behandlingsflyt.behandling.beregning

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.Beregningsgrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.BeregningsgrunnlagRepository
import no.nav.aap.behandlingsflyt.behandling.beregning.Beregning.Companion.kombinerInntektOgManuellInntekt
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.InntektGrunnlagRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.ManuellInntektGrunnlagRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre.UføreRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade.YrkesskadeRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningVurderingRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.ManuellInntektVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.lookup.repository.RepositoryProvider
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Year
import java.time.YearMonth

class BeregningService(
    private val inntektGrunnlagRepository: InntektGrunnlagRepository,
    private val sykdomRepository: SykdomRepository,
    private val uføreRepository: UføreRepository,
    private val beregningsgrunnlagRepository: BeregningsgrunnlagRepository,
    private val beregningVurderingRepository: BeregningVurderingRepository,
    private val yrkesskadeRepository: YrkesskadeRepository,
    private val manuellInntektGrunnlagRepository: ManuellInntektGrunnlagRepository
) {

    constructor(repositoryProvider: RepositoryProvider) : this(
        inntektGrunnlagRepository = repositoryProvider.provide(),
        sykdomRepository = repositoryProvider.provide(),
        uføreRepository = repositoryProvider.provide(),
        beregningsgrunnlagRepository = repositoryProvider.provide(),
        beregningVurderingRepository = repositoryProvider.provide(),
        yrkesskadeRepository = repositoryProvider.provide(),
        manuellInntektGrunnlagRepository = repositoryProvider.provide(),
    )

    fun beregnGrunnlag(behandlingId: BehandlingId): Beregningsgrunnlag {
        val uføregrad = uføreRepository.hentHvisEksisterer(behandlingId)?.vurderinger.orEmpty()
        val yrkesskadevurdering = sykdomRepository.hentHvisEksisterer(behandlingId)?.yrkesskadevurdering
        val beregningGrunnlag = beregningVurderingRepository.hentHvisEksisterer(behandlingId)
        val registrerteYrkesskader = yrkesskadeRepository.hentHvisEksisterer(behandlingId)?.yrkesskader
        val inntektGrunnlag = inntektGrunnlagRepository.hent(behandlingId)
        val manuelleInntekter = manuellInntektGrunnlagRepository.hentHvisEksisterer(behandlingId)?.manuelleInntekter.orEmpty()

        val manuelleInntektsÅr = manuelleInntekter.filter { it.periode != null }.map { it.år }.toSet()
        val inntektsPerioder = byggInntektsPerioder(
            registerMåneder = inntektGrunnlag.inntektPerMåned,
            manuelleInntekter = manuelleInntekter,
            manuelleInntektsÅr = manuelleInntektsÅr,
        )

        val beregningsgrunnlag = Beregning(
            årsInntekter = kombinerInntektOgManuellInntekt(inntektGrunnlag.inntekter, manuelleInntekter),
            nedsettelsesDato = beregningGrunnlag?.tidspunktVurdering?.nedsattArbeidsevneEllerStudieevneDato
                ?: throw IllegalStateException("Nedsettelsesdato må være satt for beregning"),
            ytterligereNedsettelsesDato = beregningGrunnlag.tidspunktVurdering.ytterligereNedsattArbeidsevneDato,
            inntektsPerioder = inntektsPerioder,
            // TODO: Hvor langt tilbake i tid skal man hente uføregrader?
            uføregrad = uføregrad,
            yrkesskadevurdering = yrkesskadevurdering,
            registrerteYrkesskader = registrerteYrkesskader,
            yrkesskadeBeløpVurderinger = beregningGrunnlag.yrkesskadeBeløpVurdering?.vurderinger,
            manuelleInntektsÅr = manuelleInntektsÅr,
        ).beregnBeregningsgrunnlag()

        beregningsgrunnlagRepository.lagre(behandlingId, beregningsgrunnlag)
        return beregningsgrunnlag
    }

    /**
     * Erstatter register-månedene for [manuelleInntektsÅr] med manuelle månedsinntekter der
     * delperiodens (beregnet PGI + eøs) fordeles jevnt på periodens måneder.
     */
    private fun byggInntektsPerioder(
        registerMåneder: Set<Månedsinntekt>,
        manuelleInntekter: Set<ManuellInntektVurdering>,
        manuelleInntektsÅr: Set<Year>,
    ): Set<Månedsinntekt> {
        if (manuelleInntektsÅr.isEmpty()) return registerMåneder

        val beholdteRegisterMåneder = registerMåneder
            .filterNot { Year.of(it.årMåned.year) in manuelleInntektsÅr }

        val manuelleMåneder = manuelleInntekter
            .filter { it.periode != null && it.år in manuelleInntektsÅr }
            .flatMap { distribuerPerMåned(it) }

        return (beholdteRegisterMåneder + manuelleMåneder).toSet()
    }

    private fun distribuerPerMåned(vurdering: ManuellInntektVurdering): List<Månedsinntekt> {
        val periode = requireNotNull(vurdering.periode)
        val totalt = (vurdering.belop?.verdi ?: BigDecimal.ZERO)
            .add(vurdering.eøsBeløp?.verdi ?: BigDecimal.ZERO)

        val måneder = generateSequence(YearMonth.from(periode.fom)) { it.plusMonths(1) }
            .takeWhile { !it.isAfter(YearMonth.from(periode.tom)) }
            .toList()

        val perMåned = totalt.divide(BigDecimal(måneder.size), 2, RoundingMode.DOWN)
        val rest = totalt.subtract(perMåned.multiply(BigDecimal(måneder.size)))

        // Legg avrundingsresten på første måned slik at summen blir eksakt lik delperiodens total.
        return måneder.mapIndexed { index, årMåned ->
            val beløp = if (index == 0) perMåned.add(rest) else perMåned
            Månedsinntekt(årMåned, Beløp(beløp))
        }
    }

    fun deaktiverGrunnlag(behandlingId: BehandlingId) {
        beregningsgrunnlagRepository.deaktiver(behandlingId)
    }

    fun utledRelevanteBeregningsÅr(behandlingId: BehandlingId): Set<Year> {
        val beregningGrunnlag = beregningVurderingRepository.hentHvisEksisterer(behandlingId)
        return Beregning.utledAlleRelevanteÅr(beregningGrunnlag)
    }
}
