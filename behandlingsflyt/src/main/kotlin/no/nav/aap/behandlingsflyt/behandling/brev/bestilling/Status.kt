package no.nav.aap.behandlingsflyt.behandling.brev.bestilling

enum class Status {
    SENDT,
    FORHÅNDSVISNING_KLAR,
    FULLFØRT,
    AVBRUTT;

    fun erEndeTilstand(): Boolean {
        return setOf(
            SENDT,
            FULLFØRT,
            AVBRUTT
        ).contains(this)
    }
}