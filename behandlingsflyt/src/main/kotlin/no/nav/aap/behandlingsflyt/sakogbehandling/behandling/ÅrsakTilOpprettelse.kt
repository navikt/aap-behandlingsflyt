package no.nav.aap.behandlingsflyt.sakogbehandling.behandling

enum class ÅrsakTilOpprettelse {
    SØKNAD,
    MANUELL_OPPRETTELSE,
    HELSEOPPLYSNINGER,
    ANNET_RELEVANT_DOKUMENT,
    OMGJØRING_ETTER_KLAGE,
    OMGJØRING_ETTER_SVAR_FRA_KLAGEINSTANS,
    ENDRING_I_REGISTERDATA,

    // Meldeperioder og aktivitet
    FASTSATT_PERIODE_PASSERT,
    FRITAK_MELDEPLIKT,
    MELDEKORT,
    AKTIVITETSMELDING,

    // Oppfølgingsbehandling
    OPPFØLGINGSOPPGAVE,
    OPPFØLGINGSOPPGAVE_SAMORDNING_GRADERING,

    // Klage-behandling
    SVAR_FRA_KLAGEINSTANS,
    KLAGE,
    AKTIVITETSPLIKT,

    // Registeroppdatering
    OPPDATERING_FRA_PDL
}