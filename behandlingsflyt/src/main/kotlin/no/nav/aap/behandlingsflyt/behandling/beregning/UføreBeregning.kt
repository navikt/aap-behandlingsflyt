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
import no.nav.aap.komponenter.tidslinje.somTidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.komponenter.verdityper.Tid
import org.slf4j.LoggerFactory
import java.time.Year
import java.time.YearMonth

class UføreBeregning(
    private val grunnlag: Grunnlag11_19,
    private val uføregrader: Set<Uføre>,
    private val relevanteÅr: Set<Year>,
    private val inntektsPerioder: Set<Månedsinntekt>,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    init {
        if (inntektsPerioder.map { Year.of(it.årMåned.year) }.toSet() != relevanteÅr) {
            log.warn("Ikke overenstemmelse med relevanteÅr ($relevanteÅr) og inntektsPerioder (${inntektsPerioder.map { it.årMåned.year }}).")
        }
    }

    fun beregnUføre(ytterligereNedsattÅr: Year): GrunnlagUføre {
        // tidslinjelogikken er basert på antagelsen om at uføre alltid har virkningstidspunkt på den første i måneden
        // og at vi får inntekt fra inntektskomponenten per måned
        val uføreTidslinje = uføregrader.tilTidslinje()

        require(inntektsPerioder
            .filter { Year.of(it.årMåned.year) in relevanteÅr }
            .groupingBy { it.årMåned.year }
            .eachCount().values.all { it == 12 }) { "Krever inntekter for alle måneder i relevante år." }

        val inntektPerMåned = inntektsPerioder
            .filter { Year.of(it.årMåned.year) in relevanteÅr }
            .groupingBy { it.årMåned }
            .fold(Beløp(0)) { acc, curr -> acc.pluss(curr.beløp) }

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

        return inntektPerMåned.mapValues { (årMåned, beløp) ->
            val uføregradDenneMåneden = uføreTidslinje.segment(årMåned.atDay(1))?.verdi ?: Prosent.`0_PROSENT`

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
        }.entries.groupBy { it.key.year }
            .mapValues { (år, månedsinntekter) ->
                val summertInntektJustertForUføre =
                    Beløp(månedsinntekter.sumOf { it.value.inntektJustertForUføregrad.verdi })

                val summertInntekt = Beløp(månedsinntekter.sumOf { it.value.inntektIKroner.verdi })
                val år = Year.of(år)

                val summertInntektIGJustertForUføregrad = gUnit(år, summertInntektJustertForUføre)

                UføreInntekt(
                    år = år,
                    inntektsPerioder = månedsinntekter.map { it.value },
                    inntektIKroner = summertInntekt,
                    inntektJustertForUføregrad = summertInntektJustertForUføre,
                    inntektIGJustertForUføregrad = summertInntektIGJustertForUføregrad.gUnit,
                    inntektIG = gUnit(år, summertInntekt).gUnit,
                    grunnbeløp = summertInntektIGJustertForUføregrad.beløp,
                    uføregrad = månedsinntekter.map { it.value.uføregrad }.lastOrNull()
                )
            }.values.toList()
    }

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
