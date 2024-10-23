package no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid

import no.nav.aap.tidslinje.JoinStyle
import no.nav.aap.tidslinje.Segment
import no.nav.aap.tidslinje.Tidslinje

data class AktivitetspliktGrunnlag(
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
        private fun merge(dokument1: BruddAktivitetsplikt?, dokument2: BruddAktivitetsplikt?) = when {
            dokument1 == null || dokument2 == null -> dokument1 ?: dokument2 ?: error("outer join hvor begge sider er null")
            dokument1.opprettetTid < dokument2.opprettetTid -> dokument2
            else -> dokument2
        }
    }
}
