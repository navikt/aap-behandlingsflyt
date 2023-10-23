package no.nav.aap.behandlingsflyt.flyt.steg

enum class StegStatus {
    /**
     * Teknisk status
     */
    START,

    /**
     * Utfører forettningslogikken i steget
     */
    UTFØRER,

    /**
     * Venter på en gitt hendelse
     */
    VENTER_PÅ_CALLBACK,

    /**
     * Punkt for å vente på avklaringsbehov
     */
    AVKLARINGSPUNKT,

    /**
     * Teknisk status, finne neste steg
     */
    AVSLUTTER,

    /**
     * Tilbakeført fra steg A til steg B, mer for logg at hendelsen har inntruffet
     */
    TILBAKEFØRT;

    fun neste(): StegStatus {
        val rekkefølge = rekkefølge()
        rekkefølge.listIterator().
        val indexOf = rekkefølge.indexOf(this)

        if (indexOf > -1 && indexOf < rekkefølge.size - 1) {
            return rekkefølge[indexOf + 1]
        }
        return START
    }

    companion object {
        fun rekkefølge(): List<StegStatus> {
            return listOf(START, UTFØRER, AVKLARINGSPUNKT, AVSLUTTER)
        }
    }
}
