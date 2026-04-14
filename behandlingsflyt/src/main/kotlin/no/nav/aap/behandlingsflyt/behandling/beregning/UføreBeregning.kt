package no.nav.aap.behandlingsflyt.behandling.beregning

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.Grunnlag11_19
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.GrunnlagUføre
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.UføreInntekt
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.UføreInntektPeriodisert
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.Grunnbeløp
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.InntektPerÅr
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre.Uføre
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre.tilTidslinje
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.komponenter.verdityper.Prosent
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.Year
import java.time.YearMonth

// TODO: ta inn både månedsinntekt og årsinntekt, og velg basert på uføre-grad
class UføreBeregning(
    private val grunnlag: Grunnlag11_19,
    private val uføregrader: Set<Uføre>,
    private val inntektsPerioder: Set<Månedsinntekt>,
    ytterligereNedsattDato: LocalDate,
    årsInntekter: Set<InntektPerÅr>,
) {
    private val relevanteÅr = Beregning.treÅrForutFor(ytterligereNedsattDato)
    private val ytterligereNedsattÅr = Year.from(ytterligereNedsattDato)

    private val inntektIÅr = { år: Year -> årsInntekter.firstOrNull { it.år == år }?.beløp ?: Beløp(0) }

    private val log = LoggerFactory.getLogger(javaClass)

    init {
        if (inntektsPerioder.map { Year.of(it.årMåned.year) }.toSet() != relevanteÅr) {
            log.warn("Ikke overenstemmelse med relevanteÅr ($relevanteÅr) og inntektsPerioder (${inntektsPerioder.map { it.årMåned.year }}).")
        }
    }

    fun beregnUføre(): GrunnlagUføre {
        // tidslinjelogikken er basert på antagelsen om at uføre alltid har virkningstidspunkt på den første i måneden
        // og at vi får inntekt fra inntektskomponenten per måned
        val uføreTidslinje = uføregrader.tilTidslinje()

        val inntektPerMånedUtenDefaults = inntektsPerioder
            .filter { Year.of(it.årMåned.year) in relevanteÅr }
            .groupingBy { it.årMåned }
            .fold(Beløp(0)) { acc, curr -> acc.pluss(curr.beløp) }

        val inntektPerMåned = generateSequence(relevanteÅr.min().atMonth(1)) { it.plusMonths(1) }
            .takeWhile { Year.of(it.year) in relevanteÅr }
            .associateWith { Beløp(0) }
            .mapValues { (årMåned, beløp) -> inntektPerMånedUtenDefaults[årMåned] ?: beløp }

        val oppjusterteInntekter =
            oppjusterMhpUføregradPeriodisertInntekt(
                inntektPerMåned,
                uføreTidslinje
            )

        // 6G-begrensning ligger her samt gjennomsnitt
        val ytterligereNedsattGrunnlag = beregn11_19Grunnlag(oppjusterteInntekter)

        val type =
            if (grunnlag.grunnlaget() >= ytterligereNedsattGrunnlag.grunnlaget())
                GrunnlagUføre.Type.STANDARD
            else GrunnlagUføre.Type.YTTERLIGERE_NEDSATT

        val grunnlaget = when (type) {
            GrunnlagUføre.Type.STANDARD -> grunnlag.grunnlaget()
            GrunnlagUføre.Type.YTTERLIGERE_NEDSATT -> ytterligereNedsattGrunnlag.grunnlaget()
        }


        return GrunnlagUføre(
            grunnlaget = grunnlaget,
            type = type,
            grunnlag = grunnlag,
            grunnlagYtterligereNedsatt = ytterligereNedsattGrunnlag,
            uføregrader = uføregrader,
            uføreInntekterFraForegåendeÅr = oppjusterteInntekter,
            uføreYtterligereNedsattArbeidsevneÅr = ytterligereNedsattÅr
        )
    }

    private fun oppjusterMhpUføregradPeriodisertInntekt(
        inntektPerMåned: Map<YearMonth, Beløp>,
        uføreTidslinje: Tidslinje<Prosent>
    ): List<UføreInntekt> {

        return inntektPerMåned.entries.groupBy { Year.of(it.key.year) }
            .mapValues {
                val år = it.key
                val detteÅret = Periode(år.atDay(1), år.atMonth(12).atEndOfMonth())
                val uføretidslinjeBegrensetTilGjeldendeÅr =
                    uføreTidslinje.begrensetTil(detteÅret)

                return@mapValues if (uføretidslinjeBegrensetTilGjeldendeÅr.isEmpty()) {
                    // Antar null uføregrad

                    val uføreGradDetteÅret = Prosent.`0_PROSENT`
                    val årsInntekt = inntektIÅr(år)

                    val periodisert = UføreInntektPeriodisert(
                        periode = detteÅret,
                        inntektIKroner = årsInntekt,
                        uføregrad = uføreGradDetteÅret,
                        inntektJustertForUføregrad = årsInntekt
                    )

                    val gUnit = gUnit(år, årsInntekt)

                    UføreInntekt(
                        år = år,
                        inntektsPerioder = listOf(periodisert),
                        inntektIKroner = årsInntekt,
                        inntektJustertForUføregrad = årsInntekt,
                        inntektIGJustertForUføregrad = gUnit.gUnit,
                        inntektIG = gUnit.gUnit,
                        grunnbeløp = gUnit.beløp,
                    )
                } else if (likUføreGradHeleÅret(uføretidslinjeBegrensetTilGjeldendeÅr, detteÅret)) {
                    // Uføregraden er konstant hele året, kan bruke årsinntekt

                    val uføregradDetteÅret = uføretidslinjeBegrensetTilGjeldendeÅr.verdier().first()
                    val arbeidsgrad = uføregradDetteÅret.komplement()
                    val årsInntekt = inntektIÅr(år)

                    val periodisert = UføreInntektPeriodisert(
                        periode = detteÅret,
                        inntektIKroner = årsInntekt,
                        uføregrad = uføregradDetteÅret,
                        inntektJustertForUføregrad = if (arbeidsgrad == Prosent.`0_PROSENT`) {
                            Beløp(0)
                        } else {
                            årsInntekt.dividert(arbeidsgrad)
                        }
                    )

                    val gUnit = gUnit(år, periodisert.inntektJustertForUføregrad)


                    UføreInntekt(
                        år = år,
                        inntektsPerioder = listOf(periodisert),
                        inntektIKroner = årsInntekt,
                        inntektJustertForUføregrad = periodisert.inntektJustertForUføregrad,
                        inntektIGJustertForUføregrad = gUnit.gUnit,
                        inntektIG = gUnit(år, årsInntekt).gUnit,
                        grunnbeløp = gUnit.beløp,
                    )
                } else {
                    val månedsinntekter = it.value.map { (årMåned, beløp) ->
                        val uføregradDenneMåneden =
                            uføreTidslinje.segment(årMåned.atDay(1))?.verdi ?: Prosent.`0_PROSENT`

                        val arbeidsgrad = uføregradDenneMåneden.komplement()

                        UføreInntektPeriodisert(
                            periode = Periode(årMåned.atDay(1), årMåned.atEndOfMonth()),
                            inntektIKroner = beløp,
                            uføregrad = uføregradDenneMåneden,
                            inntektJustertForUføregrad = if (arbeidsgrad == Prosent.`0_PROSENT`) {
                                Beløp(0)
                            } else {
                                beløp.dividert(arbeidsgrad)
                            }
                        )
                    }
                    val summertInntektJustertForUføre =
                        Beløp(månedsinntekter.sumOf { it.inntektJustertForUføregrad.verdi })

                    val summertInntekt = Beløp(månedsinntekter.sumOf { it.inntektIKroner.verdi })

                    val summertInntektIGJustertForUføregrad = gUnit(år, summertInntektJustertForUføre)

                    UføreInntekt(
                        år = år,
                        inntektsPerioder = månedsinntekter,
                        inntektIKroner = summertInntekt,
                        inntektJustertForUføregrad = summertInntektJustertForUføre,
                        inntektIGJustertForUføregrad = summertInntektIGJustertForUføregrad.gUnit,
                        inntektIG = gUnit(år, summertInntekt).gUnit,
                        grunnbeløp = summertInntektIGJustertForUføregrad.beløp,
                    )
                }
            }.values.toList()
    }

    private fun likUføreGradHeleÅret(
        uføretidslinjeBegrensetTilGjeldendeÅr: Tidslinje<Prosent>,
        detteÅret: Periode
    ): Boolean {
        return uføretidslinjeBegrensetTilGjeldendeÅr.helePerioden() == detteÅret && uføretidslinjeBegrensetTilGjeldendeÅr.verdier()
            .toSet().size == 1
    }

    @Suppress("FunctionName")
    private fun beregn11_19Grunnlag(
        oppjusterteInntekter: List<UføreInntekt>
    ): Grunnlag11_19 {
        val oppjusterteInntekterPerÅr = oppjusterteInntekter.map { inntekt ->
            InntektPerÅr(
                år = inntekt.år,
                beløp = inntekt.inntektJustertForUføregrad
            )
        }.toSet()
        return GrunnlagetForBeregningen(oppjusterteInntekterPerÅr).beregnGrunnlaget()
    }

    private fun gUnit(år: Year, beløp: Beløp): Grunnbeløp.BenyttetGjennomsnittsbeløp =
        Grunnbeløp.finnGUnit(år, beløp)
}
