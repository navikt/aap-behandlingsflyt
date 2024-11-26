package no.nav.aap.behandlingsflyt.behandling.underveis.regler

import no.nav.aap.behandlingsflyt.behandling.underveis.Kvote
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.VarighetVurdering.Avslagsårsak.ETABLERINGSFASEKVOTE_BRUKT_OPP
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.VarighetVurdering.Avslagsårsak.STANDARDKVOTE_BRUKT_OPP
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.VarighetVurdering.Avslagsårsak.STUDENTKVOTE_BRUKT_OPP
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.VarighetVurdering.Avslagsårsak.UTVIKLINGSFASEKVOTE_BRUKT_OPP
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Innvilgelsesårsak.STUDENT
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import java.time.DayOfWeek
import java.time.LocalDate

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
        val sykdomVarighetTidslinje = sykdomstidslinje(input.kvote, resultat)

        return resultat.leggTilVurderinger(sykdomVarighetTidslinje, Vurdering::leggTilVarighetVurdering)
    }


    private fun skalTelleMotStandardKvote(vurdering: Vurdering): Boolean {
        return vurdering.harRett() &&
                (vurdering.fårAapEtter(Vilkårtype.SYKDOMSVILKÅRET, null) ||
                        vurdering.fårAapEtter(Vilkårtype.SYKDOMSVILKÅRET, STUDENT))
    }

    private fun skalTelleMotStudentKvote(vurdering: Vurdering): Boolean {
        return vurdering.harRett() && vurdering.fårAapEtter(Vilkårtype.SYKDOMSVILKÅRET, STUDENT)
    }

    // ønsker vi kvote-info ved avslag i helgen?
    private fun sykdomstidslinje(kvote: Kvote, resultat: Tidslinje<Vurdering>): Tidslinje<VarighetVurdering> {
        val kvoter = mapOf(
            Sykdomskvoter.STANDARD to kvote.antallHverdagerMedRett,
            Sykdomskvoter.STUDENT to kvote.studentKvote,
            Sykdomskvoter.UTVIKLINGSFASE to 0,
            Sykdomskvoter.ETABLERINGSFASE to 0,
        )

        val telleverk: MutableMap<Sykdomskvoter, Int> = Sykdomskvoter.entries.associateWith { 0 }.toMutableMap()

        return resultat.flatMap {
            val relevanteKvoter = relevanteKvoter(it.verdi)
            when {
                relevanteKvoter.isNotEmpty() ->
                    vurderPeriode(
                        periode = it.periode,
                        kvoter = kvoter.filterKeys { it in relevanteKvoter },
                        telleverk = telleverk,
                    )

                else -> Tidslinje()
            }
        }
    }

    private fun relevanteKvoter(vurdering: Vurdering): Set<Sykdomskvoter> {
        return mapOf(
            Sykdomskvoter.STANDARD to skalTelleMotStandardKvote(vurdering),
            Sykdomskvoter.STUDENT to skalTelleMotStudentKvote(vurdering),
            Sykdomskvoter.ETABLERINGSFASE to false,
            Sykdomskvoter.UTVIKLINGSFASE to false,
        ).filterValues { it }.keys
    }

    private fun hverdagerFra(start: LocalDate): Sequence<LocalDate> {
        var dag = start
        return sequence {
            while (true) {
                if (dag.erHverdag) {
                    yield(dag)
                }
                dag = dag.plusDays(1)
            }
        }
    }

    private fun stansDato(fom: LocalDate, hverdagerSomSkalLeggesTil: Int): LocalDate {
        return hverdagerFra(fom).elementAt(hverdagerSomSkalLeggesTil)
    }

    private fun vurderPeriode(
        periode: Periode,
        kvoter: Map<Sykdomskvoter, Int>,
        telleverk: MutableMap<Sykdomskvoter, Int>,
    ): Tidslinje<VarighetVurdering> {

        val dagerIgjenPerKvote = kvoter.mapValues { it.value - telleverk[it.key]!! }
        val stansIPerioden = dagerIgjenPerKvote.any { (it.value - periode.antallDager()) < 0 }

        if (stansIPerioden) {
            val gjenværendeKvote = dagerIgjenPerKvote.minOf { it.value }
            telleverk.øk(kvoter.keys, gjenværendeKvote)

            val stansDato = stansDato(periode.fom, gjenværendeKvote)

            val stansÅrsaker = dagerIgjenPerKvote
                .filter { it.value == gjenværendeKvote }
                .keys
                .map { it.avslagsårsak }.toSet()
            return listOfNotNull<Segment<VarighetVurdering>>(
                if (stansDato == periode.fom)
                    null
                else Segment(
                    Periode(periode.fom, stansDato.minusDays(1)),
                    Oppfylt
                ),
                Segment(
                    Periode(stansDato, periode.tom),
                    Avslag(stansÅrsaker)
                )
            ).let { Tidslinje(it) }
        } else {
            telleverk.øk(kvoter.keys, periode)
            return Tidslinje(
                periode,
                Oppfylt
            )
        }
    }
}

private val helg = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)

private val LocalDate.erHverdag: Boolean
    get() = dayOfWeek !in helg

fun MutableMap<Sykdomskvoter, Int>.øk(kvoter: Set<Sykdomskvoter>, periode: Periode) {
    val hverdager = periode.dager().count { it.erHverdag }
    this.øk(kvoter, hverdager)
}

fun MutableMap<Sykdomskvoter, Int>.øk(kvoter: Set<Sykdomskvoter>, bruk: Int) {
    for (kvote in kvoter) {
        this[kvote] = this[kvote]!! + bruk
    }
}


enum class Sykdomskvoter(val avslagsårsak: VarighetVurdering.Avslagsårsak) {
    STANDARD(STANDARDKVOTE_BRUKT_OPP),
    STUDENT(STUDENTKVOTE_BRUKT_OPP),
    ETABLERINGSFASE(ETABLERINGSFASEKVOTE_BRUKT_OPP),
    UTVIKLINGSFASE(UTVIKLINGSFASEKVOTE_BRUKT_OPP),
}

