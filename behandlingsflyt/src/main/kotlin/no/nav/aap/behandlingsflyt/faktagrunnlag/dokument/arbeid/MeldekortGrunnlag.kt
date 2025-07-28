package no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid

import no.nav.aap.verdityper.dokument.JournalpostId
import java.time.LocalDate

data class MeldekortGrunnlag(
    internal val meldekortene: Set<Meldekort>,
    private val rekkefølge: Set<DokumentRekkefølge>
) {
    init {
        require(rekkefølge.size >= meldekortene.size) {
            "sjekk feilet: rekkefølge.size(${rekkefølge.size}) >= meldekortene.size(${meldekortene.size})"

        }

        val rekkefølgeIder = rekkefølge.map { it.referanse.asJournalpostId }
        require(meldekortene.all { it.journalpostId in rekkefølgeIder }) {
            "sjekk feilet: ${meldekortene.joinToString { it.journalpostId.toString() }} subset ${rekkefølge.joinToString { it.referanse.toString() }}"
        }
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
