package no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid

data class AktivitetskortGrunnlag (
    private val aktivitetskortene: Set<Aktivitetskort> ,
    private val rekkefølge: Set<DokumentRekkefølge>
) init {
    require(rekkefølge.size >= pliktkortene.size)
    require(rekkefølge.all { pliktkortene.any { pk -> it.journalpostId == pk.journalpostId } })
}