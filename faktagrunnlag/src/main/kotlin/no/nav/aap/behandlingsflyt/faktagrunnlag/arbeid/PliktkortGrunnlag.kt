package no.nav.aap.behandlingsflyt.faktagrunnlag.arbeid

data class PliktkortGrunnlag(
    internal val pliktkortene: Set<Pliktkort>,
    private val rekkefølge: Set<DokumentRekkefølge>
) {
    init {
        require(rekkefølge.size >= pliktkortene.size)
        require(rekkefølge.all { pliktkortene.any { pk -> it.journalpostId == pk.journalpostId } })
    }

    /**
     * Returnerer sortert stigende på innsendingstidspunkt
     */
    fun pliktkort(): List<Pliktkort> {
        return pliktkortene.sortedWith(compareBy { rekkefølge.first { at -> at.journalpostId == it.journalpostId }.mottattTidspunkt })
    }
}
