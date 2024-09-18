package no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid

data class AktivitetskortGrunnlag (
    internal val aktivitetskortene: Set<Aktivitetskort> ,
    private val rekkefølge: Set<DokumentRekkefølge>
) {
    init {
        require(rekkefølge.size >= aktivitetskortene.size)
        require(rekkefølge.all { aktivitetskortene.any { pk -> it.journalpostId.identifikator == pk.journalpostId.toString() } })
    }

    fun aktivitestskort(): List<Aktivitetskort> {
        return aktivitetskortene.sortedWith(compareBy {
            rekkefølge.first { at -> at.journalpostId.identifikator == it.journalpostId.toString() }.mottattTidspunkt })
    }
}
