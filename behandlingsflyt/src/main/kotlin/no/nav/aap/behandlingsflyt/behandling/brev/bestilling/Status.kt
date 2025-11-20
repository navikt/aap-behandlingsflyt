package no.nav.aap.behandlingsflyt.behandling.brev.bestilling

enum class Status {
    @Deprecated("SENDT var tidligere start-status for brevbestilling, men benyttes ikke lenger")
    SENDT,
    FORHÅNDSVISNING_KLAR,
    FULLFØRT,
    AVBRUTT;

    fun erEndeTilstand(): Boolean {
        return setOf(
            FULLFØRT,
            AVBRUTT
        ).contains(this)
    }
}