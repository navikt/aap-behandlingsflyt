package no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid

import no.nav.aap.verdityper.dokument.JournalpostId
import java.time.LocalDate

data class PliktkortGrunnlag(
    internal val pliktkortene: Set<Pliktkort>,
    private val rekkefølge: Set<DokumentRekkefølge>
) {
    init {
        require(rekkefølge.size >= pliktkortene.size)
        require(rekkefølge.all { pliktkortene.any { pk -> it.referanse.asJournalpostId == pk.journalpostId } })
    }

    /**
     * Returnerer sortert stigende på innsendingstidspunkt
     */
    fun pliktkort(): List<Pliktkort> {
        return pliktkortene.sortedWith(compareBy { rekkefølge.first { at -> at.referanse.asJournalpostId == it.journalpostId }.mottattTidspunkt })
    }

    fun innsendingsdatoPerMelding(): Map<LocalDate, JournalpostId> {
        val datoer = HashMap<LocalDate, JournalpostId>()

        for (dokumentRekkefølge in rekkefølge) {
            datoer[dokumentRekkefølge.mottattTidspunkt.toLocalDate()] = dokumentRekkefølge.referanse.asJournalpostId
        }

        return datoer
    }
}
