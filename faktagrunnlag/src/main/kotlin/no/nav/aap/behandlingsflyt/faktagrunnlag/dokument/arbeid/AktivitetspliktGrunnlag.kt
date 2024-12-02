package no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid

import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.StandardSammenslåere
import no.nav.aap.komponenter.tidslinje.Tidslinje

data class AktivitetspliktGrunnlag(
    val bruddene: Set<AktivitetspliktDokument>,
) {
    fun dokumentTidslinje(paragraf: Brudd.Paragraf): Tidslinje<AktivitetspliktDokument> {
        return bruddene
            .asSequence()
            .filter { it.brudd.paragraf == paragraf }
            .sortedBy { it.metadata.opprettetTid }
            .map { Tidslinje(it.brudd.periode, it) }
            .fold(Tidslinje()) { bruddtidslinje1, bruddtidslinje2 ->
                bruddtidslinje1.kombiner(bruddtidslinje2, StandardSammenslåere.prioriterHøyreSideCrossJoin())
            }
    }

    fun tidslinje(paragraf: Brudd.Paragraf): Tidslinje<AktivitetspliktRegistrering> {
        return dokumentTidslinje(paragraf)
            .filterIsInstance<Segment<AktivitetspliktRegistrering>>()
            .let(::Tidslinje)
    }
}
