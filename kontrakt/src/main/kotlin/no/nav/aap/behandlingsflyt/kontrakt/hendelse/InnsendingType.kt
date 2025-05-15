package no.nav.aap.behandlingsflyt.kontrakt.hendelse

public enum class InnsendingType {
    SØKNAD,
    /**
     * Dette er egentlig en dokument-type, ikke en egen innsending, da denne
     * typen opprettes i Kelvin.
     */
    AKTIVITETSKORT,
    MELDEKORT,
    LEGEERKLÆRING,
    LEGEERKLÆRING_AVVIST,
    DIALOGMELDING,
    KLAGE,
    ANNET_RELEVANT_DOKUMENT,
    MANUELL_REVURDERING,
    NY_ÅRSAK_TIL_BEHANDLING,
}