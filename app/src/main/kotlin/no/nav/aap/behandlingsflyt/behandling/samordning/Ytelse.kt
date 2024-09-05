package no.nav.aap.behandlingsflyt.behandling.samordning

enum class Ytelse {
    SYKEPENGER,
    FORELDREPENGER,
    PLEIEPENGER_BARN, // TODO: er dette det samme? PLEIEPENGER_SYKT_BARN
    PLEIEPENGER_NÆR_FAMILIE, // TODO: er dette det samme? PLEIEPENGER_NÆRSTÅENDE
    SVANGERSKAPSPENGER,
    OMSORGSPENGER
}