package no.nav.aap.behandlingsflyt.behandling.underveis.regler

import no.nav.aap.behandlingsflyt.behandling.underveis.Kvote
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.VarighetVurdering.Avslagsårsak.STANDARDKVOTE_BRUKT_OPP
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.VarighetVurdering.Avslagsårsak.STUDENTKVOTE_BRUKT_OPP
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Innvilgelsesårsak.STUDENT
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.komponenter.tidslinje.JoinStyle
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
        val sykdomVarighetTidslinje = sykdomVarighetTidslinje(resultat, input.rettighetsperiode, input.kvote)

        return resultat.leggTilVurderinger(sykdomVarighetTidslinje, Vurdering::leggTilVarighetVurdering)
    }

    private fun sykdomVarighetTidslinje(
        resultat: Tidslinje<Vurdering>,
        rettighetsperiode: Periode,
        kvote: Kvote
    ): Tidslinje<VarighetVurdering> {
        val studentkvoteTidslinje = varighetTidslinje(
            resultat = resultat,
            rettighetsperiode = rettighetsperiode,
            kvote = kvote.studentKvote,
            tellerMotKvotePredikat = ::skalTelleMotStudentKvote,
            kvoteBruktOppVurdering = Avslag(STUDENTKVOTE_BRUKT_OPP)
        )

        val studentkvoteBrukt = studentkvoteTidslinje.fold(0) { acc, segment ->
            if (segment.verdi is Oppfylt) acc + segment.periode.antallDager() else acc
        }

        val standardkvoteTidslinje = varighetTidslinje(
            resultat = resultat,
            rettighetsperiode = rettighetsperiode,
            kvote = kvote.antallHverdagerMedRett - studentkvoteBrukt,
            tellerMotKvotePredikat = ::skalTelleMotStandardKvote,
            kvoteBruktOppVurdering = Avslag(STANDARDKVOTE_BRUKT_OPP)
        )

        return standardkvoteTidslinje.kombiner(studentkvoteTidslinje,
            JoinStyle.OUTER_JOIN { periode, standardkvote, studentkvote ->
                val gjeldendeKvote = listOfNotNull(standardkvote, studentkvote).single()
                Segment(periode, gjeldendeKvote.verdi)
            })
    }

    private fun varighetTidslinje(
        resultat: Tidslinje<Vurdering>,
        rettighetsperiode: Periode,
        kvote: Int,
        tellerMotKvotePredikat: (Vurdering) -> Boolean,
        kvoteBruktOppVurdering: Avslag
    ): Tidslinje<VarighetVurdering> {
        val vurderingerIHverdag = resultat.kombiner(
            helger(rettighetsperiode), StandardSammenslåere.minus()
        )

        var dagerBrukt = 0

        val førsteVurderingEtterKvote = vurderingerIHverdag.firstOrNull { vurdering ->
            val kvoteBrukt = if (tellerMotKvotePredikat(vurdering.verdi)) vurdering.periode.antallDager() else 0

            (kvote < dagerBrukt + kvoteBrukt).also { vurderingenBrukerOppKvoten ->
                if (!vurderingenBrukerOppKvoten) dagerBrukt += kvoteBrukt
            }
        }

        if (førsteVurderingEtterKvote == null) return Tidslinje()

        val dagerInnIPeriode = kvote - dagerBrukt
        val stansdato = førsteVurderingEtterKvote.periode.fom.plusDays(dagerInnIPeriode.toLong())

        return listOf(
            Segment<VarighetVurdering>(Periode(rettighetsperiode.fom, stansdato.minusDays(1)), Oppfylt),
            Segment<VarighetVurdering>(Periode(stansdato, rettighetsperiode.tom), kvoteBruktOppVurdering),
        ).let { Tidslinje(it) }
            .kombiner(resultat.filter { !tellerMotKvotePredikat(it.verdi) }, StandardSammenslåere.minus())

    }


    private fun skalTelleMotStandardKvote(vurdering: Vurdering): Boolean {
        return vurdering.harRett() && vurdering.fårAapEtter(Vilkårtype.SYKDOMSVILKÅRET, null)
    }

    private fun skalTelleMotStudentKvote(vurdering: Vurdering): Boolean {
        return vurdering.harRett() && vurdering.fårAapEtter(Vilkårtype.SYKDOMSVILKÅRET, STUDENT)
    }

    private fun helger(rettighetsperiode: Periode): Tidslinje<Unit> {
        val førsteLørdag = rettighetsperiode.fom.with(TemporalAdjusters.previous(DayOfWeek.SATURDAY))
        return generateSequence(førsteLørdag) { it.plusWeeks(1) }
            .takeWhile { it <= rettighetsperiode.tom }
            .map { Segment(Periode(it, it.plusDays(1)), Unit) }
            .let { Tidslinje(it.toList()) }
    }
}

