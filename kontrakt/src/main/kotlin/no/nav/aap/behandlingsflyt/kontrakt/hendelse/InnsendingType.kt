package no.nav.aap.behandlingsflyt.kontrakt.hendelse

public enum class InnsendingType {
    SØKNAD,
    AKTIVITETSKORT, // <-- alltid digital, egentlig ikke dokument
    PLIKTKORT,
    LEGEERKLÆRING, // fra lege
    LEGEERKLÆRING_AVVIST,
    DIALOGMELDING // også fra lege
}

// tilleggsopplysninger