package no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov

enum class Status {
    OPPRETTET,
    AVSLUTTET,
    TOTRINNS_VURDERT,
    SENDT_TILBAKE_FRA_BESLUTTER,
    KVALITETSSIKRET,
    SENDT_TILBAKE_FRA_KVALITETSSIKRER,
    AVBRUTT
}
