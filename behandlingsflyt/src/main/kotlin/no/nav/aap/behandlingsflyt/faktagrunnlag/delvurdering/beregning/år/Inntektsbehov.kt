package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.år

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.Grunnbeløp
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.InntektPerÅr
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre.Uføre
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.komponenter.verdityper.GUnit
import no.nav.aap.komponenter.verdityper.Prosent
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.Year
import java.util.*

class Inntektsbehov(private val input: Input) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun utledAlleRelevanteÅr(): Set<Year> {
        return input.datoerForInnhenting().flatMap(::treÅrForutFor).toSortedSet()
    }

    fun hentYtterligereNedsattArbeidsevneDato(): LocalDate? {
        return input.beregningGrunnlag?.tidspunktVurdering?.ytterligereNedsattArbeidsevneDato
    }

    private fun treÅrForutFor(nedsettelsesdato: LocalDate): SortedSet<Year> {
        val nedsettelsesår = Year.from(nedsettelsesdato)
        return 3.downTo(1L).map(nedsettelsesår::minusYears).toSortedSet()
    }

    fun utledForOrdinær(): Set<InntektPerÅr> {
        return filtrerInntekter(input.nedsettelsesDato, input.inntekter)
    }

    fun utledForYtterligereNedsatt(): Set<InntektPerÅr> {
        val ytterligereNedsettelsesDato = input.beregningGrunnlag?.tidspunktVurdering?.ytterligereNedsattArbeidsevneDato
        requireNotNull(ytterligereNedsettelsesDato)
        return filtrerInntekter(ytterligereNedsettelsesDato, input.inntekter)
    }

    /**
     * Skal beregne med uføre om det finnes data på uføregrad.
     */
    fun finnesUføreData(): Boolean {
        return input.beregningGrunnlag?.tidspunktVurdering?.ytterligereNedsattArbeidsevneDato != null
                && input.uføregrad.isNotEmpty()
    }

    /**
     * Om det eksisterer informasjon om yrkesskade (tidspunkt og andel nedsettelse) og det finnes en antatt årlig
     * inntekt, så skal beregningen skje med yrkesskadefordel (§11-22)
     */
    fun yrkesskadeVurderingEksisterer(): Boolean {
        return input.registrerteYrkesskader?.harYrkesskade() == true && input.yrkesskadevurdering?.relevanteSaker != null && input.yrkesskadevurdering.relevanteSaker.isNotEmpty() && input.beregningGrunnlag?.yrkesskadeBeløpVurdering != null && input.yrkesskadevurdering.andelAvNedsettelsen != null
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

    fun uføregrad(): List<Uføre> {
        return requireNotNull(input.uføregrad)
    }

    fun skadetidspunkt(): LocalDate {
        return samleOpplysningerOmYrkesskade().max().skadedato
    }

    fun antattÅrligInntekt(): Beløp {
        return requireNotNull(samleOpplysningerOmYrkesskade().max().antattÅrligInntekt)
    }

    private fun samleOpplysningerOmYrkesskade(): List<YrkesskadeBeregning> {
        // Finn den saken med størst beløp basert på antall G på skadetidspunktet
        val relevanteSaker = input.yrkesskadevurdering?.relevanteSaker.orEmpty()
        val sakerMedDato =
            relevanteSaker.map { sak -> input.registrerteYrkesskader?.yrkesskader?.singleOrNull { it.ref == sak } }

        return sakerMedDato.filterNotNull().map { sak ->
            YrkesskadeBeregning(
                sak.ref,
                requireNotNull(sak.skadedato), // TODO her må alternativt skadedato hentes fra yrkesskadevurdering når denne får funksjonalitet for å manuelt overstyre skadedato
                input.beregningGrunnlag?.yrkesskadeBeløpVurdering?.vurderinger?.first { it.referanse == sak.ref }?.antattÅrligInntekt!!
            )
        }
    }

    fun andelYrkesskade(): Prosent {
        return requireNotNull(input.yrkesskadevurdering?.andelAvNedsettelsen)
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
