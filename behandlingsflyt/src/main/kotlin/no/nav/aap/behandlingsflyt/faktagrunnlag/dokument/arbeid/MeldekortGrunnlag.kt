package no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid

import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingReferanse
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

        val rekkefølgeIder = rekkefølge.map { it.referanse.verdi }
        require(meldekortene.all { it.referanse.verdi in rekkefølgeIder }) {
            "sjekk feilet: ${meldekortene.joinToString { it.referanse.toString() }} subset ${rekkefølge.joinToString { it.referanse.toString() }}"
        }
    }

    /**
     * Returnerer sortert stigende på innsendingstidspunkt
     */
    fun meldekort(): List<Meldekort> {
        return meldekortene.sortedWith(compareBy { rekkefølge.first { at -> at.referanse.verdi == it.referanse.verdi }.mottattTidspunkt })
    }

    fun innsendingsdatoPerMelding(): Map<LocalDate, InnsendingReferanse> {
        val datoer = HashMap<LocalDate, InnsendingReferanse>()

        for (dokumentRekkefølge in rekkefølge) {
            datoer[dokumentRekkefølge.mottattTidspunkt.toLocalDate()] = dokumentRekkefølge.referanse
        }

        return datoer
    }
}
