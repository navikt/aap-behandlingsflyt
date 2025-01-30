package no.nav.aap.behandlingsflyt.kontrakt.hendelse

public enum class InnsendingType {
    SØKNAD,
    /**
     * Dette er egentlig en dokument-type, ikke en egen innsending, da denne
     * typen opprettes i Kelvin.
     */
    AKTIVITETSKORT,
    PLIKTKORT,
    LEGEERKLÆRING,
    LEGEERKLÆRING_AVVIST,
    DIALOGMELDING,
    ANNET_RELEVANT_DOKUMENT
}