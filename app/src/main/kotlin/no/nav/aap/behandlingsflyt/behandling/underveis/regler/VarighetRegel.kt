package no.nav.aap.behandlingsflyt.behandling.underveis.regler

import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.StandardSammenslåere
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import java.time.DayOfWeek
import java.time.temporal.TemporalAdjusters

/**
 * Håndterer varighetsbestemmelsene (11-12 + unntak fra denne). Sjekker uttak mot kvoten etablert i saken.
 *
 * - Varigheten på ordinær (3 år)
 * - Unntak
 *   - Utvidelse (2 år)
 *   - Sykepengererstatning (6 måneder)
 *   - Venter på uføre (4 + 4 måneder)
 *   - Avklart arbeid (??)
 * - Dødsfall på bruker
 *
 */
//WIP
class VarighetRegel : UnderveisRegel {
    override fun vurder(input: UnderveisInput, resultat: Tidslinje<Vurdering>): Tidslinje<Vurdering> {
        val vurderingerIHverdag = resultat.kombiner(
            helger(input.rettighetsperiode), StandardSammenslåere.minus()
        )
        val kvote = input.kvote
        var dagerBrukt = 0
        
        val førsteVurderingUtenforKvote = vurderingerIHverdag.firstOrNull { vurdering ->
            val kvoteBrukt = if (vurdering.verdi.harRett()) {
                vurdering.periode.antallDager()
            } else { 0 }
            
            (dagerBrukt+kvoteBrukt < kvote.antallHverdagerMedRett).also { innenforKvote ->
                if (innenforKvote) dagerBrukt += kvoteBrukt
            }
        }
        
        if (førsteVurderingUtenforKvote == null) {
            return resultat
        }
        
        val stansdato = førsteVurderingUtenforKvote.periode.fom.plusDays(
            (kvote.antallHverdagerMedRett-dagerBrukt).toLong()
        )

        val varighetTidslinje = listOf(
            Segment(Periode(input.rettighetsperiode.fom, stansdato.minusDays(1)), false),
            Segment(Periode(stansdato, input.rettighetsperiode.tom), true),
        ).let { Tidslinje(it) }

        return resultat.leggTilVurderinger(varighetTidslinje, Vurdering::leggTilVarighetVurdering)
    }

    private fun helger(rettighetsperiode: Periode): Tidslinje<Unit> {
        val førsteLørdag = rettighetsperiode.fom.with(TemporalAdjusters.previous(DayOfWeek.SATURDAY))
        return generateSequence(førsteLørdag) { it.plusWeeks(1) }
            .takeWhile { it <= rettighetsperiode.tom }
            .map { Segment(Periode(it, it.plusDays(1)), Unit) }
            .let { Tidslinje(it.toList()) }
    }
}