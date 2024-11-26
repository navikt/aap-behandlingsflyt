package no.nav.aap.behandlingsflyt.behandling.underveis.regler

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
        val kvote = input.kvote

        val standardKvoteTidslinje = varighetTidslinje(
            resultat = resultat,
            rettighetsperiode = input.rettighetsperiode,
            kvote = kvote.antallHverdagerMedRett,
            tellerMotKvotePredikat = ::skalTelleMotStandardKvote,
            kvoteBruktOppVurdering = Avslag(STANDARDKVOTE_BRUKT_OPP)
        )

        val studentKvoteTidslinje = varighetTidslinje(
            resultat = resultat,
            rettighetsperiode = input.rettighetsperiode,
            kvote = kvote.studentKvote,
            tellerMotKvotePredikat = ::skalTelleMotStudentKvote,
            kvoteBruktOppVurdering = Avslag(STUDENTKVOTE_BRUKT_OPP)
        )

        val varighetTidslinje = standardKvoteTidslinje.kombiner(studentKvoteTidslinje,
            JoinStyle.OUTER_JOIN { periode, standardkvote, studentkvote ->
                val gjeldendeKvote = listOfNotNull(standardkvote, studentkvote).single()
                Segment(periode, gjeldendeKvote.verdi)
            })

        return resultat.leggTilVurderinger(varighetTidslinje, Vurdering::leggTilVarighetVurdering)
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

        if (førsteVurderingEtterKvote == null) {
            return Tidslinje()
        }

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

    // §§ 11-5, 11-14, 11-15 og 11-16 skal telle mot kvoten (§ 11-12 fjerde ledd).
    private fun paragraf11_12_4(vurdering: Vurdering): Boolean {
        return vurdering.fårAapEtter(Vilkårtype.SYKDOMSVILKÅRET, null) || // 11-5
                vurdering.fårAapEtter(Vilkårtype.SYKDOMSVILKÅRET, STUDENT) // 11-14
        // 11-15 (etablerer virksomhet) Ikke implementert
        // 11-16 (uten påbegynt aktivitet) Er vel neppe et eget vilkår?
    }


    private fun helger(rettighetsperiode: Periode): Tidslinje<Unit> {
        val førsteLørdag = rettighetsperiode.fom.with(TemporalAdjusters.previous(DayOfWeek.SATURDAY))
        return generateSequence(førsteLørdag) { it.plusWeeks(1) }
            .takeWhile { it <= rettighetsperiode.tom }
            .map { Segment(Periode(it, it.plusDays(1)), Unit) }
            .let { Tidslinje(it.toList()) }
    }
}

