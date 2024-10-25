package no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid

import no.nav.aap.tidslinje.JoinStyle
import no.nav.aap.tidslinje.Segment
import no.nav.aap.tidslinje.Tidslinje

data class AktivitetspliktGrunnlag(
    val bruddene: Set<AktivitetspliktDokument>,
) {
    val tidslinje: Tidslinje<AktivitetspliktRegistrering> by lazy {
        bruddene
            .asSequence()
            .map { Tidslinje(it.brudd.periode, it) }
            .fold(Tidslinje()) { bruddtidslinje1, bruddtidslinje2 ->
                bruddtidslinje1.kombiner(bruddtidslinje2, JoinStyle.OUTER_JOIN { periode, brudd1, brudd2 ->
                    merge(brudd1?.verdi, brudd2?.verdi)?.let {
                        Segment(periode, it)
                    }
                })
            }
    }

    companion object {
        private fun merge(
            dokument1: AktivitetspliktDokument?,
            dokument2: AktivitetspliktDokument?
        ): AktivitetspliktRegistrering? {
            val nyesteDokument = when {
                dokument1 == null || dokument2 == null -> dokument1 ?: dokument2
                ?: error("outer join hvor begge sider er null")

                dokument1.metadata.opprettetTid < dokument2.metadata.opprettetTid -> dokument2
                else -> dokument2
            }
            return when (nyesteDokument) {
                is AktivitetspliktFeilregistrering -> null
                is AktivitetspliktRegistrering -> nyesteDokument
            }
        }
    }
}
