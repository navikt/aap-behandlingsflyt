package no.nav.aap.behandlingsflyt.faktagrunnlag.arbeid

import no.nav.aap.verdityper.dokument.JournalpostId
import java.time.LocalDateTime

class DokumentRekkefølge(val journalpostId: JournalpostId, val mottattTidspunkt: LocalDateTime) :
    Comparable<DokumentRekkefølge> {


    override fun compareTo(other: DokumentRekkefølge): Int {
        return mottattTidspunkt.compareTo(other.mottattTidspunkt)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DokumentRekkefølge

        if (journalpostId != other.journalpostId) return false
        if (mottattTidspunkt != other.mottattTidspunkt) return false

        return true
    }

    override fun hashCode(): Int {
        var result = journalpostId.hashCode()
        result = 31 * result + mottattTidspunkt.hashCode()
        return result
    }
}