package no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid

import no.nav.aap.tidslinje.JoinStyle
import no.nav.aap.tidslinje.Segment
import no.nav.aap.tidslinje.Tidslinje

data class BruddAktivitetspliktGrunnlag(
    val bruddene: Set<BruddAktivitetsplikt>,
) {
    val tidslinje: Tidslinje<BruddAktivitetsplikt> by lazy {
        bruddene
            .asSequence()
            .map { Tidslinje(it.periode, it) }
            .fold(Tidslinje()) { bruddtidslinje1, bruddtidslinje2 ->
                bruddtidslinje1.kombiner(bruddtidslinje2, JoinStyle.OUTER_JOIN { periode, brudd1, brudd2 ->
                    Segment(periode, merge(brudd1?.verdi, brudd2?.verdi))
                })
            }
    }

    companion object {
        private fun merge(brudd1: BruddAktivitetsplikt?, brudd2: BruddAktivitetsplikt?) = when {
            brudd1 == null || brudd2 == null -> brudd1 ?: brudd2 ?: error("outer join hvor begge sider er null")
            brudd1.opprettetTid < brudd2.opprettetTid -> brudd2
            else -> brudd2
        }
    }
}
