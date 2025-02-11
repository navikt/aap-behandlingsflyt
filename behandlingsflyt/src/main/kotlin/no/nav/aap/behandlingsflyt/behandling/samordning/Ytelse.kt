package no.nav.aap.behandlingsflyt.behandling.samordning

/**
 * @param type Om denne ytelsen kan behandles automatisk, eller vil gi et avklaringsbehov.
 */
enum class Ytelse(val type: AvklaringsType) {
    SYKEPENGER(AvklaringsType.MANUELL),
    FORELDREPENGER(AvklaringsType.AUTOMATISK),
    PLEIEPENGER_BARN(AvklaringsType.MANUELL),
    PLEIEPENGER_NÆR_FAMILIE(AvklaringsType.MANUELL),
    SVANGERSKAPSPENGER(AvklaringsType.MANUELL),
    OMSORGSPENGER(AvklaringsType.AUTOMATISK),
    OPPLÆRINGSPENGER(AvklaringsType.AUTOMATISK)
}

enum class AvklaringsType {
    MANUELL,
    AUTOMATISK,
}