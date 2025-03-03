package no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid

import no.nav.aap.verdityper.dokument.JournalpostId
import java.time.LocalDate

data class MeldekortGrunnlag(
    internal val meldekortene: Set<Meldekort>,
    private val rekkefølge: Set<DokumentRekkefølge>
) {
    init {
        require(rekkefølge.size >= meldekortene.size)
        require(rekkefølge.all { meldekortene.any { pk -> it.referanse.asJournalpostId == pk.journalpostId } })
    }

    /**
     * Returnerer sortert stigende på innsendingstidspunkt
     */
    fun meldekort(): List<Meldekort> {
        return meldekortene.sortedWith(compareBy { rekkefølge.first { at -> at.referanse.asJournalpostId == it.journalpostId }.mottattTidspunkt })
    }

    fun innsendingsdatoPerMelding(): Map<LocalDate, JournalpostId> {
        val datoer = HashMap<LocalDate, JournalpostId>()

        for (dokumentRekkefølge in rekkefølge) {
            datoer[dokumentRekkefølge.mottattTidspunkt.toLocalDate()] = dokumentRekkefølge.referanse.asJournalpostId
        }

        return datoer
    }
}
