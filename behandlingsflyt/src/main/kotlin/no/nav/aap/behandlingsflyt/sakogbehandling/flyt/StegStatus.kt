package no.nav.aap.behandlingsflyt.sakogbehandling.flyt

enum class StegStatus {
    /**
     * Teknisk status
     */
    START,
    /**
     * Teknisk status
     */
    OPPDATER_FAKTAGRUNNLAG,

    /**
     * Utfører forettningslogikken i steget
     */
    UTFØRER,


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

    /**
     * Gitt den definerte rekkefølgen i [rekkefølge], regn ut neste stegstatus.
     */
    fun neste(): StegStatus {
        val rekkefølge = rekkefølge()
        val indexOf = rekkefølge.indexOf(this)

        if (indexOf > -1 && indexOf < rekkefølge.size - 1) {
            return rekkefølge[indexOf + 1]
        }
        return START
    }

    companion object {
        fun rekkefølge(): List<StegStatus> {
            return listOf(START, OPPDATER_FAKTAGRUNNLAG, UTFØRER, AVKLARINGSPUNKT, AVSLUTTER)
        }
    }
}
