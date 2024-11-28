package no.nav.aap.verdityper.flyt

enum class ÅrsakTilBehandling {
    MOTTATT_SØKNAD,
    MOTTATT_AKTIVITETSMELDING,
    MOTTATT_MELDEKORT,
    MOTTATT_LEGEERKLÆRING,
    MOTTATT_AVVIST_LEGEERKLÆRING,
    MOTTATT_DIALOGMELDING,
    G_REGULERING;

    companion object {
        /**
         * Alle med funksjonell verdi, G-regulering holdes utenfor
         */
        fun alle(): List<ÅrsakTilBehandling> {
            val alle = ÅrsakTilBehandling.entries.toMutableSet()

            alle.remove(G_REGULERING)

            return alle.toList()
        }
    }
}
