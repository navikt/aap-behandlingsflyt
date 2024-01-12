package no.nav.aap.verdityper.dokument

data class JournalpostId(val identifikator: String) {

    override fun toString(): String {
        return "Journalpost[id=$identifikator]"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as JournalpostId

        return identifikator == other.identifikator
    }

    override fun hashCode(): Int {
        return identifikator.hashCode()
    }
}
