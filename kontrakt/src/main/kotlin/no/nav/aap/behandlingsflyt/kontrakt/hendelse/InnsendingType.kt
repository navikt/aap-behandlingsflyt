package no.nav.aap.behandlingsflyt.kontrakt.hendelse

public enum class InnsendingType {
    SØKNAD,
    /**
     * Dette er egentlig en dokument-type, ikke en egen innsending, da denny
     * typen opprettes i Kelvin.
     */
    AKTIVITETSKORT,
    PLIKTKORT,
    LEGEERKLÆRING, // fra lege
    LEGEERKLÆRING_AVVIST,
    DIALOGMELDING // også fra lege
}