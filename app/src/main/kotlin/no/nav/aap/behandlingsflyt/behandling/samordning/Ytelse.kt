package no.nav.aap.behandlingsflyt.behandling.samordning

enum class Ytelse(val type: AvklaringsType) {
    SYKEPENGER(AvklaringsType.MANUELL),
    FORELDREPENGER(AvklaringsType.AUTOMATISK),
    PLEIEPENGER_BARN(AvklaringsType.AUTOMATISK),
    PLEIEPENGER_NÆR_FAMILIE(AvklaringsType.AUTOMATISK),
    SVANGERSKAPSPENGER(AvklaringsType.AUTOMATISK),
}

enum class AvklaringsType {
    MANUELL,
    AUTOMATISK
}