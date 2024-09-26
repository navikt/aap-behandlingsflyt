package no.nav.aap.behandlingsflyt.behandling.samordning

enum class Ytelse(val type: AvklaringsType) {
    SYKEPENGER(AvklaringsType.MANUELL),
    FORELDREPENGER(AvklaringsType.MANUELL),
    PLEIEPENGER_BARN(AvklaringsType.MANUELL),
    PLEIEPENGER_NÆR_FAMILIE(AvklaringsType.MANUELL),
    SVANGERSKAPSPENGER(AvklaringsType.MANUELL),
    OMSORGSPENGER(AvklaringsType.MANUELL),
    OPPLÆRINGSPENGER(AvklaringsType.MANUELL)
}

enum class AvklaringsType {
    MANUELL,
    AUTOMATISK,
}