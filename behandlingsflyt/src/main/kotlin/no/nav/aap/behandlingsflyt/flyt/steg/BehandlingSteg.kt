package no.nav.aap.behandlingsflyt.flyt.steg

import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder

interface BehandlingSteg {
    /** Adferden til steget.
    *
    * Et steg skal i utgangspunktet bare lese og skrive til databasen, ikke gjøre kall med
    * gateways. For å hente inn opplysninger fra et register, må du lage et [Informasjonskrav][no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav] som
    * du knytter til steget (se [BehandlingFlytBuilder.medSteg][no.nav.aap.behandlingsflyt.flyt.BehandlingFlytBuilder.medSteg]).
    *
    * For å etterspørre opplysninger fra et menneske, må det løftes et [avklaringsbehov][no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehov],
    * se [AvklaringsbehovService][no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovService].
    *
    * ### OBS: Transaksjonell
    * Hvis du trenger et remote procedure call, spesielt et som ikke er idempotent, så kan kallet gjøres indirekte, ved å skjedulere
    * en jobb som gjør kallet. Dette gjør vi for å forhindre en situasjon hvor et steg regner ut et resultat og sender verdien
    * videre til en ekstern tjeneste, og etter det, så feiler transaksjonen: da kommer vi i en tilstand hvor vi ikke har noe spor
    * i behandlingsflyt om hva som skjedde.
    *
    * Når kallet gjøres via en jobb, vil dette føre til at kallet kun utføres etter at transaksjonen bestilte jobben har comittet. Et eksempel på dette er
    * [VarsleVedtakJobbUtfører][no.nav.aap.behandlingsflyt.prosessering.VarsleVedtakJobbUtfører].
    *
    * ### OBS: Kjøres flere ganger
    *  Denne metoden vil ofte kjøres mange ganger i én behandling, og dette er ikke noe som steget kan styre selv.
    *  Eksempler på tilfeller når [utfør] blir kalt flere ganger:
    * - Steget løfter et avklaringsbehov (første kjøring). Avklaringsbehovet løses, så kjører steget på nytt (andre kjøring).
    * - Behandlingsflyten dras tilbake til et tidligere steg. Da vil den kjøre gjennom dette steget nok en gang.
    *
    * I en vanlig ytelsesbehandling (førstegangsbehandling eller revurdering), må du regne med at steget kjøres et betydelig antall
    * ganger.
    *
    * En teknikk for å håndtere at steget kjøres flere ganger, er å implementere det som en avstemming:
    * 1. Les det som er lagret
    * 2. Regn ut det som ønskes
    * 3. Oppdater om det som ønskes er annerledes en det som er lagret
    *
    * ### OBS: Må rydde opp etter seg selv
    * Tilstanden til behandlingen kan ha endret seg mellom to kjøringer av steget, så steget må rydde
    * opp etter seg selv. Dette håndteres av avstemming-metodikken nevnt over.
    *
    * Relatert til dette, er at data i databasen burde eies av ett steg — altså at ett steg har ansvar for å skrive
    * et bestemt sted. Hvis man mot formodning har to forskjellige steg som skriver til det samme steget, så
    * må begge stegene kunne kjøres flere ganger og begge stegene må kunne rydde opp etter seg, uten
    * at de går i bena på hverandre.
    *
    * ### OBS: Utfordringer med å lese opplysninger fra senere steg
    * Ikke anta at opplysninger er riktig hvis de eies av et steg som kommer senere i [flyten][no.nav.aap.behandlingsflyt.flyt.BehandlingFlyt].
    * Det kan være at det senere steget allerede har kjørt steget, men at behandlingen er dradd tilbake,
    * grunnlaget er endret, men det senere steget har ikke oppdatert verdien enda.
    */
    fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat
}
