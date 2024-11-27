package no.nav.aap.behandlingsflyt.behandling.underveis.regler

import no.nav.aap.behandlingsflyt.behandling.underveis.regler.Hverdager.Companion.antallHverdager
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.Hverdager.Companion.plusHverdager
import no.nav.aap.behandlingsflyt.behandling.underveis.Kvoter
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Innvilgelsesårsak.STUDENT
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode

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
        val sykdomVarighetTidslinje = sykdomstidslinje(input.kvoter, resultat)

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
    private fun sykdomstidslinje(kvoter: Kvoter, resultat: Tidslinje<Vurdering>): Tidslinje<VarighetVurdering> {
        val telleverk = Telleverk(kvoter)

        return resultat.flatMap {
            val relevanteKvoter = relevanteKvoter(it.verdi)
            when {
                relevanteKvoter.isNotEmpty() ->
                    vurderPeriode(
                        periode = it.periode,
                        relevanteKvoter = relevanteKvoter,
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

    private fun vurderPeriode(
        periode: Periode,
        relevanteKvoter: Set<Sykdomskvoter>,
        telleverk: Telleverk,
    ): Tidslinje<VarighetVurdering> {
        require(relevanteKvoter.isNotEmpty())
        val dagerTilStans = telleverk.minsteUbrukteKvote(relevanteKvoter)
        val kvoterSomErStanset = telleverk.kvoterSomErStanset(relevanteKvoter)

        if (kvoterSomErStanset.isEmpty() && periode.antallHverdager() <= dagerTilStans) {
            telleverk.øk(relevanteKvoter, periode)
            return Tidslinje(
                periode,
                Oppfylt
            )
        }

        val kvoterSomBlirStanset = telleverk.kvoterNærmestÅBliBruktOpp(relevanteKvoter)
        telleverk.markereKvoterOversteget(kvoterSomBlirStanset)
        if (kvoterSomErStanset.isNotEmpty()) {
            return Tidslinje(
                periode,
                Avslag(kvoterSomBlirStanset.map { it.avslagsårsak }.toSet())
            )
        }
        else if (dagerTilStans < periode.antallHverdager()) {
            telleverk.øk(relevanteKvoter, dagerTilStans)

            val stansDato = periode.fom.plusHverdager(dagerTilStans)

            val stansÅrsaker = kvoterSomBlirStanset.map { it.avslagsårsak }.toSet()
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
            error("kan ikke skje")
        }
    }
}
