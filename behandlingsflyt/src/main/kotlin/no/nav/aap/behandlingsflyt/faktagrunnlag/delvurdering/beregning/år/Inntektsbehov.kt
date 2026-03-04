package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.år

import no.nav.aap.behandlingsflyt.behandling.beregning.GrunnlagetForBeregningen
import no.nav.aap.behandlingsflyt.behandling.beregning.Månedsinntekt
import no.nav.aap.behandlingsflyt.behandling.beregning.UføreBeregning
import no.nav.aap.behandlingsflyt.behandling.beregning.beregnGrunnlagYrkesskade
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.Beregningsgrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.Grunnbeløp
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.InntektPerÅr
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre.Uføre
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre.tilTidslinje
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade.Yrkesskader
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.ManuellInntektVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.YrkesskadeBeløpVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.Yrkesskadevurdering
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.komponenter.verdityper.GUnit
import no.nav.aap.komponenter.verdityper.Prosent
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.time.LocalDate
import java.time.Year
import java.util.*

/*
* @param nedsettelsesDato Dato da arbeidsevnen ble nedsatt.
* @param årsInntekter Inntekter per år.
* @param uføregrad Uføregrader bruker har hatt de siste tre årene.
* @param yrkesskadevurdering Hvis ikke-null, en yrkesskadevurdering.
* @param registrerteYrkesskader
* @param beregningGrunnlag Se [no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningGrunnlag]
* @param inntektsPerioder Inntekt per måned
*/
class Inntektsbehov(
    val nedsettelsesDato: LocalDate,
    val ytterligereNedsettelsesDato: LocalDate?,
    val årsInntekter: Set<InntektPerÅr>,
    val inntektsPerioder: Set<Månedsinntekt>,
    val uføregrad: Set<Uføre>,
    val yrkesskadevurdering: Yrkesskadevurdering?,
    val registrerteYrkesskader: Yrkesskader?,
    val yrkesskadeBeløpVurderinger: List<YrkesskadeBeløpVurdering>?,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    init {
        if (finnesUføreData()) {
            validerSummertInntekt()
        }
    }

    fun beregnBeregningsgrunnlag(): Beregningsgrunnlag {
        // 6G-begrensning ligger her samt gjennomsnitt
        val grunnlag11_19 = GrunnlagetForBeregningen(utledForOrdinær()).beregnGrunnlaget()

        val beregningMedEllerUtenUføre = if (finnesUføreData()) {
            UføreBeregning(grunnlag11_19, this).beregnUføre()
        } else {
            grunnlag11_19
        }

        // §11-22 Arbeidsavklaringspenger ved yrkesskade
        val beregningMedEllerUtenUføreMedEllerUtenYrkesskade =
            if (yrkesskadeVurderingEksisterer()) {
                beregnGrunnlagYrkesskade(
                    grunnlag11_19 = beregningMedEllerUtenUføre,
                    antattÅrligInntekt = årligAntattInntektVedYrkesskade(),
                    andelAvNedsettelsenSomSkyldesYrkesskaden = andelYrkesskade()
                )
            } else {
                beregningMedEllerUtenUføre
            }
        return beregningMedEllerUtenUføreMedEllerUtenYrkesskade
    }

    /**
     * Mengde med inntekt per år i de tre foregående årene fra nedsettelsesdatoen.
     */
    fun utledForOrdinær(): Set<InntektPerÅr> {
        return filtrerInntekter(nedsettelsesDato, årsInntekter)
    }

    fun validerSummertInntekt() {
        val inntektPerÅrFraPerioder: Map<Year, Beløp> = inntektsPerioder
            .groupBy { Year.of(it.årMåned.year) }
            .mapValues { (_, value) -> value.sumOf { it.beløp.verdi }.let(::Beløp) }

        inntektPerÅrFraPerioder.forEach { (år, sum) ->
            // Forskjell på inntektene kan ikke være større enn 100 kr
            // Mest for sanity - denne sjekken kan nok fjernes, men helst etter at vi har blitt litt smartere
            // i når vi bruker A-Inntekt som kilde. Om uføregraden er konstant et år, bør POPP brukes, for da trengs ikke
            // månedsinntekter.
            val differanse =
                (årsInntekter.first { it.år == år }.beløp.verdi.stripTrailingZeros() - sum.verdi.stripTrailingZeros()).abs()
            require(
                differanse < BigDecimal(100)
            )
            { "Håndterer ikke å støtte forskjellig inntekt fra A-Inntekt og PESYS. Fikk $sum for år $år, men fant ${årsInntekter.filter { it.år == år }}" }
        }
    }

    fun utledForYtterligereNedsatt(): Set<Year> {
        val ytterligereNedsettelsesDato =
            requireNotNull(ytterligereNedsettelsesDato)

        return treÅrForutFor(ytterligereNedsettelsesDato)
    }

    /**
     * Skal beregne med uføre om det finnes data på uføregrad.
     */
    fun finnesUføreData(): Boolean {
        return ytterligereNedsettelsesDato != null
                && uføregrad.isNotEmpty()
                && uføregrad.tilTidslinje().minDato() <= ytterligereNedsettelsesDato
                && uføregrad.tilTidslinje().maxDato() >= ytterligereNedsettelsesDato
    }

    /**
     * Om det eksisterer informasjon om yrkesskade (tidspunkt og andel nedsettelse) og det finnes en antatt årlig
     * inntekt, så skal beregningen skje med yrkesskadefordel (§11-22)
     */
    fun yrkesskadeVurderingEksisterer(): Boolean {
        if (yrkesskadevurdering == null) return false
        val betingelser = listOf(
            registrerteYrkesskader?.harYrkesskade() == true,
            yrkesskadevurdering.relevanteSaker.isNotEmpty(),
            yrkesskadeBeløpVurderinger != null,
            yrkesskadevurdering.andelAvNedsettelsen != null
        )

        return betingelser.all { it }
    }

    /**
     * Gitt en mengde med inntekter [inntekter] og en [nedsettelsesdato], returner en mengde med
     * relevante inntekter (3 år før nedsettelsesdato). Om inntekten ikke finnes, antas den å være
     * lik 0.
     */
    private fun filtrerInntekter(
        nedsettelsesdato: LocalDate, inntekter: Set<InntektPerÅr>
    ): Set<InntektPerÅr> {
        val relevanteÅr = treÅrForutFor(nedsettelsesdato)
        return relevanteÅr.map { relevantÅr ->
            val år = inntekter.firstOrNull { entry -> entry.år == relevantÅr }
            if (år == null) {
                // TODO IKKe default til null kr inntekt! Bør krasje, og fikses i tidligere steg
                log.warn("Fant ikke inntekt for $relevantÅr, bruker 0 i stedet.")
                return@map InntektPerÅr(relevantÅr, Beløp(0))
            }
            return@map år
        }.toSet()
    }

    /**
     * Velg det yrkesskadetidspunktet med høyest antatt inntekt.
     */
    fun skadetidspunkt(): LocalDate {
        return samleOpplysningerOmYrkesskade().max().skadedato
    }

    /**
     * Returner høyeste inntekt blant yrkesskadeopplysningene.
     */
    fun antattÅrligInntekt(): Beløp {
        return requireNotNull(samleOpplysningerOmYrkesskade().max().antattÅrligInntekt)
    }

    private fun samleOpplysningerOmYrkesskade(): List<YrkesskadeBeregning> {
        // Finn den saken med størst beløp basert på antall G på skadetidspunktet
        val relevanteSaker = yrkesskadevurdering?.relevanteSaker.orEmpty()
        val sakerMedDato =
            relevanteSaker.mapNotNull { sak -> registrerteYrkesskader?.yrkesskader?.singleOrNull { it.ref == sak.referanse } }

        return sakerMedDato.map { sak ->
            val skadedato = sak.skadedato
                ?: yrkesskadevurdering?.relevanteSaker?.firstOrNull { it.referanse == sak.ref }?.manuellYrkesskadeDato
            YrkesskadeBeregning(
                sak.ref,
                requireNotNull(skadedato) { "Ulovlig tilstand. skadedato er null, og mangler manuell yrkesskade dato." },
                yrkesskadeBeløpVurderinger?.firstOrNull { it.referanse == sak.ref }?.antattÅrligInntekt!!
            )
        }
    }

    fun andelYrkesskade(): Prosent {
        return requireNotNull(yrkesskadevurdering?.andelAvNedsettelsen)
    }

    fun årligAntattInntektVedYrkesskade(): InntektPerÅr {
        return InntektPerÅr(
            Year.from(skadetidspunkt()),
            antattÅrligInntekt()
        )
    }

    companion object {
        fun utledAlleRelevanteÅr(
            nedsettelsesDato: LocalDate,
            ytterligereNedsattArbeidsevneDato: LocalDate?
        ): Set<Year> {
            val datoerForInnhenting = setOfNotNull(nedsettelsesDato, ytterligereNedsattArbeidsevneDato)
            return datoerForInnhenting.flatMap(::treÅrForutFor).toSortedSet()
        }

        /**
         * Returnerer en mengde av de tre foregående årene fra nedsettelsesdatoen og
         * dato for ytterligere nedsatt arbeidsevne.
         * 
         * Dersom nedsettelsesdato ikke er satt, returneres en tom mengde.
         */
        fun utledAlleRelevanteÅr(beregningGrunnlag: BeregningGrunnlag?): Set<Year> {
            val nedsettelsesDato =
                beregningGrunnlag?.tidspunktVurdering?.nedsattArbeidsevneEllerStudieevneDato ?: return emptySet()
            val ytterligereNedsattArbeidsevneDato =
                beregningGrunnlag.tidspunktVurdering.ytterligereNedsattArbeidsevneDato

            return utledAlleRelevanteÅr(nedsettelsesDato, ytterligereNedsattArbeidsevneDato)
        }

        fun utledRelevanteYtterligereNedsattÅr(beregningGrunnlag: BeregningGrunnlag?): Set<Year> {
            return beregningGrunnlag?.tidspunktVurdering?.ytterligereNedsattArbeidsevneDato?.let(::treÅrForutFor)
                .orEmpty()
        }

        private fun treÅrForutFor(nedsettelsesdato: LocalDate): SortedSet<Year> {
            val nedsettelsesår = Year.from(nedsettelsesdato)
            return 3.downTo(1L).map(nedsettelsesår::minusYears).toSortedSet()
        }

        fun kombinerInntektOgManuellInntekt(
            inntekter: Set<InntektPerÅr>,
            manuelleInntekter: Set<ManuellInntektVurdering>
        ): Set<InntektPerÅr> {
            val manuellePGIByÅr = manuelleInntekter
                .tilÅrInntekt { it.belop }

            val manuellEOSByÅr = manuelleInntekter
                .tilÅrInntekt { it.eøsBeløp }

            val inntekterByÅr = inntekter
                .groupBy { it.år }
                .mapValues {
                    require(it.value.size == 1)
                    it.value.first()
                }

            val kombinerteInntekter =
                (manuellePGIByÅr + inntekterByÅr).mapValues { (år, inntektPerÅr) ->
                    val eos = manuellEOSByÅr[år]?.beløp ?: Beløp(BigDecimal.ZERO)
                    inntektPerÅr.copy(beløp = inntektPerÅr.beløp.pluss(eos))
                }.values.toSet()

            return kombinerteInntekter
        }

        private fun Collection<ManuellInntektVurdering>.tilÅrInntekt(selector: (ManuellInntektVurdering) -> Beløp?): Map<Year, InntektPerÅr> {
            return this.filter { selector(it) != null }
                .map { InntektPerÅr(it.år, selector(it)!!, it) }
                .groupBy { it.år }
                .mapValues {
                    it.value.single()
                }
        }

    }
}

private data class YrkesskadeBeregning(val ref: String, val skadedato: LocalDate, val antattÅrligInntekt: Beløp) :
    Comparable<YrkesskadeBeregning> {

    fun gUnit(): GUnit {
        return Grunnbeløp.finnGUnit(skadedato, antattÅrligInntekt).gUnit
    }

    override fun compareTo(other: YrkesskadeBeregning): Int {
        return gUnit().compareTo(other.gUnit())
    }

}
