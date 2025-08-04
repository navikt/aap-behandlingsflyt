package no.nav.aap.behandlingsflyt.sakogbehandling.behandling

enum class ÅrsakTilOpprettelse {
    SØKNAD,
    OMGJØRING_ETTER_KLAGE,
    OMGJØRING_ETTER_SVAR_FRA_KLAGEINSTANS,

    MANUELL_OPPRETTELSE,
    HELSEOPPLYSNINGER,
    MELDEKORT,
    AKTIVITETSMELDING,
    ANNET_RELEVANT_DOKUMENT,

    // Oppfølgingsbehandling
    OPPFØLGINGSOPPGAVE,

    // Klage-behandling
    SVAR_FRA_KLAGEINSTANS,
    KLAGE,

    FASTSATT_PERIODE_PASSERT,
}