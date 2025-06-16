package no.nav.aap.behandlingsflyt.behandling.samordning

/**
 * @param type Om denne ytelsen kan behandles automatisk, eller vil gi et avklaringsbehov.
 */
enum class Ytelse(val type: AvklaringsType) {
    SYKEPENGER(AvklaringsType.MANUELL),
    FORELDREPENGER(AvklaringsType.MANUELL),
    PLEIEPENGER(AvklaringsType.MANUELL),
    SVANGERSKAPSPENGER(AvklaringsType.MANUELL),
    OMSORGSPENGER(AvklaringsType.MANUELL),
    OPPLÆRINGSPENGER(AvklaringsType.MANUELL),
    UKJENT_SLUTTDATO_PÅ_YTELSE(AvklaringsType.MANUELL),
}

enum class AvklaringsType {
    MANUELL,
    AUTOMATISK,
}