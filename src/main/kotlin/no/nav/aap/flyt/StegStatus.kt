package no.nav.aap.flyt

enum class StegStatus {
    /**
     * Teknisk status
     */
    START,

    /**
     * Punkt for å vente på avklaringsbehov
     */
    INNGANG,

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
    UTGANG,

    /**
     * Teknisk status, finne neste steg
     */
    AVSLUTTER,

    /**
     * Tilbakeført fra steg A til steg B, mer for logg at hendelsen har inntruffet
     */
    TILBAKEFØRT;

    companion object {
        fun rekkefølge(): List<StegStatus> {
            return listOf(START, INNGANG, UTFØRER, UTGANG, AVSLUTTER)
        }

        fun neste(status: StegStatus): StegStatus {
            val rekkefølge = rekkefølge()
            val indexOf = rekkefølge.indexOf(status)

            if (indexOf > -1 && indexOf < rekkefølge.size - 1) {
                return rekkefølge[indexOf + 1]
            }
            return START
        }
    }
}
