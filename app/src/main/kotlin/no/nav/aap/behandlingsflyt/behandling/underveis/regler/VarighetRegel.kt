package no.nav.aap.behandlingsflyt.behandling.underveis.regler

import no.nav.aap.behandlingsflyt.behandling.underveis.regler.VarighetRegel.VarighetVurdering.KVOTE_BRUKT_OPP
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.VarighetRegel.VarighetVurdering.KVOTE_IKKE_BRUKT_OPP
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
    enum class VarighetVurdering {
        KVOTE_IKKE_BRUKT_OPP,
        KVOTE_BRUKT_OPP,
    }

    override fun vurder(input: UnderveisInput, resultat: Tidslinje<Vurdering>): Tidslinje<Vurdering> {
        val vurderingerIHverdag = resultat.kombiner(
            helger(input.rettighetsperiode), StandardSammenslåere.minus()
        )
        val kvote = input.kvote
        var dagerBrukt = 0

        val vurderingenSomBrukerOppKvoten = vurderingerIHverdag.firstOrNull { vurdering ->
            val kvoteBrukt = if (vurdering.verdi.harRett()) vurdering.periode.antallDager() else 0

            (kvote.antallHverdagerMedRett <= dagerBrukt + kvoteBrukt).also { vurderingenBrukerOppKvoten ->
                if (!vurderingenBrukerOppKvoten) dagerBrukt += kvoteBrukt
            }
        }

        if (vurderingenSomBrukerOppKvoten == null) {
            return resultat
        }

        val dagerInnIPeriode = kvote.antallHverdagerMedRett - dagerBrukt
        val stansdato = vurderingenSomBrukerOppKvoten.periode.fom.plusDays(dagerInnIPeriode.toLong())

        val varighetTidslinje = listOf(
            Segment(Periode(input.rettighetsperiode.fom, stansdato.minusDays(1)), KVOTE_IKKE_BRUKT_OPP),
            Segment(Periode(stansdato, input.rettighetsperiode.tom), KVOTE_BRUKT_OPP),
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