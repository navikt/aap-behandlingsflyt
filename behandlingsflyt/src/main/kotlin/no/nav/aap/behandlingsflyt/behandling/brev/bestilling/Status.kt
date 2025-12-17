package no.nav.aap.behandlingsflyt.behandling.brev.bestilling

enum class Status {
    @Deprecated("SENDT var tidligere start-status for brevbestilling, men benyttes ikke lenger")
    SENDT,
    FORHÅNDSVISNING_KLAR,
    FULLFØRT,
    AVBRUTT,
    TILBAKESTILT,
    ANNULLERT;

    fun erEndeTilstand(): Boolean {
        return setOf(
            FULLFØRT,
            AVBRUTT,
            TILBAKESTILT,
            ANNULLERT
        ).contains(this)
    }

    fun kanGjenopptas(): Boolean {
        return AVBRUTT == this
    }

    fun erTilbakestilt(): Boolean {
        return TILBAKESTILT == this
    }

}