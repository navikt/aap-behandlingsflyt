package no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov

public enum class Status {
     /**  Hvis Definisjon.type er
      * - MANUELT_PÅKREVD:  behandlingsflyt venter på en løsning
      * - MANUELT_FRIVILLIG:  behandlingsflyt kan godta en løsning
      * - OVERSTYR: ???
      * */
    OPPRETTET,

    /** Behandlingsflyt har mottatt en løsning, og avklaringsbehovet
     * er en kandidat for kvalitetssikring og totrinnskontroll. */
    AVSLUTTET,

    /** Beslutter har godkjent løsningen. */
    TOTRINNS_VURDERT,

    /** Beslutter har avvist løsningen. */
    SENDT_TILBAKE_FRA_BESLUTTER,

    /** Kvalitetssikrer har godkjent løsningen. */
    KVALITETSSIKRET,

    /** Kvalitetssikrer har avvist løsningen. */
    SENDT_TILBAKE_FRA_KVALITETSSIKRER,

    /** Behovet er avbrutt. Eventuelle løsninger er ikke
     * kandidater for kvalitetssikring eller totrinnskontroll. Kan ses på som en
     * form for soft-delete. */
    AVBRUTT,
    ;

    public fun erÅpent(): Boolean {
        return this in setOf(
            OPPRETTET,
            SENDT_TILBAKE_FRA_BESLUTTER,
            SENDT_TILBAKE_FRA_KVALITETSSIKRER
        )
    }

    public fun erAvsluttet(): Boolean {
        return this in setOf(
            AVSLUTTET,
            TOTRINNS_VURDERT,
            KVALITETSSIKRET,
            AVBRUTT
        )
    }
}
