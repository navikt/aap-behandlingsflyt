package no.nav.aap.behandlingsflyt.behandling.underveis.regler

import no.nav.aap.behandlingsflyt.behandling.underveis.regler.Hverdager.Companion.antallHverdager
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.Hverdager.Companion.plusHverdager
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import org.slf4j.LoggerFactory

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
    private val log = LoggerFactory.getLogger(javaClass)

    override fun vurder(input: UnderveisInput, resultat: Tidslinje<Vurdering>): Tidslinje<Vurdering> {
        val telleverk = Telleverk(input.kvoter)

        val varighetTidslinje = resultat.flatMap {
            val relevanteKvoter = relevanteKvoter(it.verdi)
            when {
                relevanteKvoter.isNotEmpty() ->
                    vurderPeriode(
                        periode = it.periode,
                        relevanteKvoter = relevanteKvoter,
                        telleverk = telleverk,
                    )

                else -> Tidslinje(it.periode, Oppfylt(brukerAvKvoter = emptySet()))
            }
        }

        return resultat.leggTilVurderinger(varighetTidslinje, Vurdering::leggTilVarighetVurdering)
    }

    private fun relevanteKvoter(vurdering: Vurdering): Set<Kvote> {
        return Kvote.entries.associateWith { it.tellerMotKvote.invoke(vurdering) }.filterValues { it }.keys
    }

    private fun vurderPeriode(
        periode: Periode,
        relevanteKvoter: Set<Kvote>,
        telleverk: Telleverk,
    ): Tidslinje<VarighetVurdering> {
        require(relevanteKvoter.isNotEmpty())
        if (Kvote.SYKEPENGEERSTATNING in relevanteKvoter && Kvote.STUDENT in relevanteKvoter) {
            log.warn("sykepengeerstatning -og student-vilkår er oppfylt på samme vurdering")
        }

        val dagerTilStans = telleverk.minsteUbrukteKvote(relevanteKvoter)
        val kvoterStansesIPeriode = dagerTilStans < periode.antallHverdager()

        if (!telleverk.erKvoterStanset(relevanteKvoter) && !kvoterStansesIPeriode) {
            telleverk.øk(relevanteKvoter, periode.antallHverdager())
            return Tidslinje(
                periode,
                Oppfylt(relevanteKvoter)
            )
        }

        val kvoterSomBlirStanset = telleverk.kvoterNærmestÅBliBruktOpp(relevanteKvoter)

        if (telleverk.erKvoterStanset(relevanteKvoter)) {
            return Tidslinje(
                periode,
                Avslag(relevanteKvoter, kvoterSomBlirStanset.map { it.avslagsårsak }.toSet())
            )
        } else if (kvoterStansesIPeriode) {
            telleverk.øk(relevanteKvoter, dagerTilStans)
            telleverk.markereKvoterOversteget(kvoterSomBlirStanset)

            val stansDato = periode.fom.plusHverdager(dagerTilStans)

            val stansÅrsaker = kvoterSomBlirStanset.map { it.avslagsårsak }.toSet()
            return listOfNotNull<Segment<VarighetVurdering>>(
                if (stansDato == periode.fom)
                    null
                else Segment(
                    Periode(periode.fom, stansDato.minusDays(1)),
                    Oppfylt(relevanteKvoter)
                ),
                Segment(
                    Periode(stansDato, periode.tom),
                    Avslag(relevanteKvoter, stansÅrsaker)
                )
            ).let { Tidslinje(it) }
        } else {
            error("kan ikke skje")
        }
    }
}
