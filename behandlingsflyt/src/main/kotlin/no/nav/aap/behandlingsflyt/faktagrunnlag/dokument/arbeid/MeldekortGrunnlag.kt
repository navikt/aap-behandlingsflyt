package no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid

import no.nav.aap.komponenter.type.Periode
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
     * Returnerer sortert stigende på innsendingstidspunkt, med opprettetTidspunkt som tiebreaker
     */
    fun meldekort(): List<Meldekort> {
        return meldekortene.sortedWith(
            compareBy(
                { rekkefølge.first { at -> at.referanse.asJournalpostId == it.journalpostId }.mottattTidspunkt },
                { it.opprettetTidspunkt }
            )
        )
    }

    fun innsendingsdatoPerMelding(): Map<LocalDate, JournalpostId> {
        val datoer = HashMap<LocalDate, JournalpostId>()

        for (dokumentRekkefølge in rekkefølge) {
            datoer[dokumentRekkefølge.mottattTidspunkt.toLocalDate()] = dokumentRekkefølge.referanse.asJournalpostId
        }

        return datoer
    }

    /**
     * Nyeste meldekort som overlapper meldeperioden (basert på innmeldte timer).
     * Returnerer ikke meldekort uten innmeldte timer.
     */
    fun nyesteForMeldeperiode(meldeperiode: Periode): Meldekort? =
        meldekort().lastOrNull { it.tilhørerMeldeperiode(meldeperiode) }

    /**
     * Nyeste meldekort som overlapper meldeperioden på en gitt dato (basert på innmeldte timer).
     * Returnerer ikke meldekort uten innmeldte timer.
     */
    fun nyesteForMeldeperiodePåDato(meldeperiode: Periode, dato: LocalDate): Meldekort? =
        meldekort().lastOrNull {
            it.tilhørerMeldeperiode(meldeperiode) && it.mottattTidspunkt.toLocalDate() == dato
        }

    /**
     * Alle tidligere meldekort for en meldeperiode.
     * Det nyeste meldekortet (jf. [nyesteForMeldeperiode]) er ekskludert.
     */
    fun tidligereForMeldeperiode(meldeperiode: Periode): List<Meldekort> {
        val nyeste = nyesteForMeldeperiode(meldeperiode)
        return meldekort()
            .filter { it.tilhørerMeldeperiode(meldeperiode) && it != nyeste }
    }
}

private fun Meldekort.tilhørerMeldeperiode(meldeperiode: Periode): Boolean {
    val arbeidsperiode = arbeidsperiode()
    return arbeidsperiode != null && meldeperiode.overlapper(arbeidsperiode)
}
