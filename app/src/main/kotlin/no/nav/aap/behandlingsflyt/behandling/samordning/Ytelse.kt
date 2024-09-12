package no.nav.aap.behandlingsflyt.behandling.samordning

enum class Ytelse {
    SYKEPENGER,
    FORELDREPENGER,
    PLEIEPENGER_BARN, // TODO: Mapping mellom disse? PLEIEPENGER_SYKT_BARN
    PLEIEPENGER_NÆR_FAMILIE, // TODO: Mapping mellom disse? PLEIEPENGER_NÆRSTÅENDE
    SVANGERSKAPSPENGER,
    OMSORGSPENGER,
    OPPLÆRINGSPENGER,
    ENGANGSTØNAD,
    FRISINN
}