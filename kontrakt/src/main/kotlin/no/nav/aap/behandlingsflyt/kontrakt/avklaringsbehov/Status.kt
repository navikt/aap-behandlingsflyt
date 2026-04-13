package no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov

/** Statusen til et avklaringsbehov.
 *
 * Diagrammet under viser overgangene mellom forskjellige [statuser][Status].
 * Hver status er en sirkel.  Den lille, tomme sirkelen er starten. Statuser med
 * dobbel sirkel er gyldige slutt-statuser.
 *
 * Triggerere for overganger er firkanter.
 * Diagrammet under viser de viktigste overgangene, noen overganger er ikke tegnet inn,
 * fordi det blir uoversiktlig. Følgende mangler:
 *
 * * Fra alle statuser til [AVBRUTT]. Denne overgangen brukes hvis avklaringsbehovet ikke lenger trengs.
 *
 * * Fra alle tilstander, bortsett fra [AVBRUTT] og [OPPRETTET], til [OPPRETTET]. Dette skjer når avklaringsehovet
 *   er løst, men det viser seg at løsningen ikke er god nok.
 *
 *  * Fra [AVSLUTTET], [KVALITETSSIKRET] og [TOTRINNS_VURDERT] til [AVSLUTTET]. Dette skjer når hvis vi mottar
 *      en løsning.
```mermaid
---
config:
layout: elk
---
flowchart TB
START(( ))
AVBRUTT(((AVBRUTT)))

subgraph Åpent avklaringsbehov
OPPRETTET_INN[trenger løsning]
OPPRETTET((OPPRETTET))
SENDT_TILBAKE_FRA_KVALITETSSIKRER_INN[kvalitetssikrer returnerer]
SENDT_TILBAKE_FRA_KVALITETSSIKRER((SENDT_TILBAKE_FRA_KVALITETSSIKRER))
SENDT_TILBAKE_FRA_BESLUTTER_INN[beslutter returnerer]
SENDT_TILBAKE_FRA_BESLUTTER((SENDT_TILBAKE_FRA_BESLUTTER))
end

subgraph Løst avklaringsbehov
AVSLUTTET_INN[løsning mottatt]
AVSLUTTET(((AVSLUTTET)))
KVALITETSSIKRET_INN[kvalitetssikrer godkjenner]
KVALITETSSIKRET(((KVALITETSSIKRET)))
TOTRINNS_VURDERT_INN[beslutter godkjenner]
TOTRINNS_VURDERT(((TOTRINNS_VURDERT)))
end

START --> OPPRETTET_INN --> OPPRETTET
AVBRUTT --- OPPRETTET_INN
OPPRETTET --> AVSLUTTET_INN --> AVSLUTTET
AVSLUTTET --> KVALITETSSIKRET_INN --> KVALITETSSIKRET
AVSLUTTET --> SENDT_TILBAKE_FRA_KVALITETSSIKRER_INN --> SENDT_TILBAKE_FRA_KVALITETSSIKRER

AVSLUTTET --> TOTRINNS_VURDERT_INN
KVALITETSSIKRET --> TOTRINNS_VURDERT_INN
AVSLUTTET --> SENDT_TILBAKE_FRA_BESLUTTER_INN
KVALITETSSIKRET --> SENDT_TILBAKE_FRA_BESLUTTER_INN

TOTRINNS_VURDERT_INN --> TOTRINNS_VURDERT
SENDT_TILBAKE_FRA_BESLUTTER_INN --> SENDT_TILBAKE_FRA_BESLUTTER

SENDT_TILBAKE_FRA_KVALITETSSIKRER --> AVSLUTTET_INN
SENDT_TILBAKE_FRA_BESLUTTER --> AVSLUTTET_INN
```
 */

public enum class Status {
     /**  Hvis Definisjon.type er
      * - MANUELT_PÅKREVD:  behandlingsflyt venter på en løsning
      * - MANUELT_FRIVILLIG:  behandlingsflyt kan godta en løsning
      * - OVERSTYR:  behandlingsflyt kan godta en løsning
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
