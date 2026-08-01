package no.nav.aap.behandlingsflyt.steg.beregning

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Year
import java.time.YearMonth
import no.nav.aap.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.InntektGrunnlagRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.ManuellInntektGrunnlagRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre.UføreRepository
import no.nav.aap.behandlingsflyt.steg.yrkesskade.YrkesskadeRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningVurderingRepository
import no.nav.aap.behandlingsflyt.steg.sykdom.SykdomRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.beregning.Beregning
import no.nav.aap.beregning.Beregningsgrunnlag
import no.nav.aap.beregning.InntektPerÅr
import no.nav.aap.beregning.InntektValidering
import no.nav.aap.beregning.ManuellInntektVurdering
import no.nav.aap.beregning.Månedsinntekt
import no.nav.aap.beregning.Uføre
import no.nav.aap.beregning.UføreInntektUtleder
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.misc.inntekt.ManuellInntektGrunnlag

class BeregningService(
    private val inntektGrunnlagRepository: InntektGrunnlagRepository,
    private val sykdomRepository: SykdomRepository,
    private val uføreRepository: UføreRepository,
    private val beregningsgrunnlagRepository: BeregningsgrunnlagRepository,
    private val beregningVurderingRepository: BeregningVurderingRepository,
    private val yrkesskadeRepository: YrkesskadeRepository,
    private val manuellInntektGrunnlagRepository: ManuellInntektGrunnlagRepository,
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
        val manuelleInntekter =
            manuellInntektGrunnlagRepository.hentHvisEksisterer(behandlingId)?.manuelleInntekter.orEmpty()
        val årsInntekter = Beregning.kombinerInntektOgManuellInntekt(inntektGrunnlag.inntekter, manuelleInntekter)

        val årMedManuellInntektIPeriode = manuelleInntekter.filter { it.månedsPeriode != null }.map { it.år }.toSet()
        InntektValidering.validerAtDelperioderDekkerHeleÅret(manuelleInntekter)
        val inntektsPerioder = byggInntektsPerioder(
            registerMåneder = inntektGrunnlag.inntektPerMåned,
            manuelleInntekter = manuelleInntekter,
            årMedManuellInntektIPeriode = årMedManuellInntektIPeriode,
        )
        validerMånedsinntekterForUføre(
            inntektsPerioder = inntektsPerioder,
            årsInntekter = årsInntekter,
            uføregrader = uføregrad,
            årMedManuellInntektIPeriode = årMedManuellInntektIPeriode,
        )

        val tidspunktVurdering = beregningGrunnlag?.tidspunktVurdering
            ?: throw IllegalStateException("Nedsettelsesdato må være satt for beregning")
        val beregningsgrunnlag = Beregning(
            årsInntekter = årsInntekter,
            nedsettelsesDato = tidspunktVurdering.nedsattArbeidsevneEllerStudieevneDato,
            ytterligereNedsettelsesDato = tidspunktVurdering.ytterligereNedsattArbeidsevneDato,
            inntektsPerioder = inntektsPerioder,
            // TODO: Hvor langt tilbake i tid skal man hente uføregrader?
            uføregrad = uføregrad,
            yrkesskadevurdering = yrkesskadevurdering,
            registrerteYrkesskader = registrerteYrkesskader,
            yrkesskadeBeløpVurderinger = beregningGrunnlag.yrkesskadeBeløpVurdering?.vurderinger,
        ).beregnBeregningsgrunnlag()

        beregningsgrunnlagRepository.lagre(behandlingId, beregningsgrunnlag)
        return beregningsgrunnlag
    }

    fun manglerInntekterFor(behandlingId: BehandlingId, inkluderManuelle: Boolean = true): Set<Year> {
        val relevanteÅr = utledRelevanteBeregningsÅr(behandlingId)

        val inntektGrunnlag = inntektGrunnlagRepository.hentHvisEksisterer(behandlingId)
            ?.inntekter.orEmpty()
            .filter { it.år in relevanteÅr }

        val manuelleInntekter =
            manuellInntektGrunnlagRepository.hentHvisEksisterer(behandlingId)?.manuelleInntekter.orEmpty()

        val manuelleInntekterRelevanteÅr =
            if (inkluderManuelle) manuelleInntekter.filter { it.år in relevanteÅr } else emptyList()

        val kombinerteÅr =
            (inntektGrunnlag.map { it.år } + manuelleInntekterRelevanteÅr
                .map { it.år }).toSet()

        return relevanteÅr.filter { it !in kombinerteÅr }.toSet()
    }

    /**
     * Erstatter register-månedene for [årMedManuellInntektIPeriode] med manuelle månedsinntekter der
     * delperiodens (beregnet PGI + eøs) fordeles jevnt på periodens måneder.
     */
    private fun byggInntektsPerioder(
        registerMåneder: Set<Månedsinntekt>,
        manuelleInntekter: Set<ManuellInntektVurdering>,
        årMedManuellInntektIPeriode: Set<Year>,
    ): Set<Månedsinntekt> {
        if (årMedManuellInntektIPeriode.isEmpty()) return registerMåneder

        val beholdteRegisterMåneder = registerMåneder
            .filterNot { Year.of(it.årMåned.year) in årMedManuellInntektIPeriode }

        val manuelleMåneder = manuelleInntekter
            .filter { it.månedsPeriode != null && it.år in årMedManuellInntektIPeriode }
            .flatMap { distribuerPerMåned(it) }

        return (beholdteRegisterMåneder + manuelleMåneder).toSet()
    }

    /**
     * Fordeler total manuell inntekt jevnt over alle måneder i perioden og lager én månedsinntekt per måned.
     */
    private fun distribuerPerMåned(vurdering: ManuellInntektVurdering): List<Månedsinntekt> {
        val periode = requireNotNull(vurdering.månedsPeriode)
        val totalt = (vurdering.belop?.verdi ?: BigDecimal.ZERO)
            .add(vurdering.eøsBeløp?.verdi ?: BigDecimal.ZERO)

        val måneder = generateSequence(YearMonth.from(periode.fom)) { it.plusMonths(1) }
            .takeWhile { !it.isAfter(YearMonth.from(periode.tom)) }
            .toList()

        val perMåned = totalt.divide(BigDecimal(måneder.size), 2, RoundingMode.HALF_UP)

        return måneder.map { årMåned ->
            Månedsinntekt(årMåned, Beløp(perMåned))
        }
    }

    private fun validerMånedsinntekterForUføre(
        inntektsPerioder: Set<Månedsinntekt>,
        årsInntekter: Set<InntektPerÅr>,
        uføregrader: Set<Uføre>,
        årMedManuellInntektIPeriode: Set<Year>,
    ) {
        inntektsPerioder
            .groupBy { Year.of(it.årMåned.year) }
            .forEach { (år, perioder) ->
                if (år in årMedManuellInntektIPeriode) return@forEach
                if (!UføreInntektUtleder.harVariabelUføregrad(uføregrader, år)) return@forEach

                InntektValidering.validerSummertInntekt(
                    år = år,
                    månedsinntekter = perioder.associate { it.årMåned to it.beløp },
                    årsInntekter = årsInntekter,
                )
            }
    }

    fun harPeriodeinntektForKrevdeÅr(kontekst: FlytKontekstMedPerioder, manuellInntektGrunnlag: ManuellInntektGrunnlag?): Boolean {
        val årSomKreverPeriodeinntekt = årSomKreverManuellPeriodeinntekt(kontekst)
        return årSomKreverPeriodeinntekt.all { år ->
            manuellInntektGrunnlag?.manuelleInntekter?.any { it.år == år && it.månedsPeriode != null } == true
        }
    }

    fun årSomKreverManuellPeriodeinntekt(kontekst: FlytKontekstMedPerioder): Set<Year> {
        val ytterligereNedsattDato = beregningVurderingRepository.hentHvisEksisterer(kontekst.behandlingId)
            ?.tidspunktVurdering?.ytterligereNedsattArbeidsevneDato
        val uføregrader = uføreRepository.hentHvisEksisterer(kontekst.behandlingId)?.vurderinger.orEmpty()
        val inntektGrunnlag = inntektGrunnlagRepository.hentHvisEksisterer(kontekst.behandlingId)

        if (ytterligereNedsattDato == null || uføregrader.isEmpty() || inntektGrunnlag == null) return emptySet()

        return UføreInntektUtleder.finnÅrSomKreverManuellPeriodeinntekt(
            uføregrader = uføregrader,
            inntektPerMåned = inntektGrunnlag.inntektPerMåned,
            årsInntekter = inntektGrunnlag.inntekter,
            ytterligereNedsattDato = ytterligereNedsattDato,
        )
    }

    fun deaktiverGrunnlag(behandlingId: BehandlingId) {
        beregningsgrunnlagRepository.deaktiver(behandlingId)
    }

    fun utledRelevanteBeregningsÅr(behandlingId: BehandlingId): Set<Year> {
        val beregningGrunnlag = beregningVurderingRepository.hentHvisEksisterer(behandlingId)
        return Beregning.utledAlleRelevanteÅr(beregningGrunnlag)
    }
}