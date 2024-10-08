package no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid

import no.nav.aap.tidslinje.JoinStyle
import no.nav.aap.tidslinje.Segment
import no.nav.aap.tidslinje.Tidslinje

data class BruddAktivitetspliktGrunnlag(
    val bruddene: Set<BruddAktivitetsplikt>,
) {
    fun lagTidslinje(): Tidslinje<BruddAktivitetsplikt> {
        return bruddene
            .asSequence()
            .map { Tidslinje(it.periode, it) }
            .fold(Tidslinje()) { bruddtidslinje1, bruddtidslinje2 ->
                bruddtidslinje1.kombiner(bruddtidslinje2, JoinStyle.OUTER_JOIN { periode, brudd1, brudd2 ->
                    Segment(periode, merge(brudd1?.verdi, brudd2?.verdi))
                })
            }
    }

    companion object {
        fun merge(brudd1: BruddAktivitetsplikt?, brudd2: BruddAktivitetsplikt?): BruddAktivitetsplikt {
            if (brudd1 == null || brudd2 == null) {
                return brudd1 ?: brudd2 ?: error("outer join hvor begge sider er null")
            }

            return when {
                brudd1.opprettetTid < brudd2.opprettetTid -> brudd1
                brudd1.opprettetTid > brudd2.opprettetTid -> brudd2
                else -> {
                    /* TODO: Avklar om det er greit å returnere en av dem. */
                    error("Begge bruddene ble meldt nøyaktig samtidig, på nanosekundet. What to do? Velge en tilfeldig?")
                }
            }
        }
    }
}
