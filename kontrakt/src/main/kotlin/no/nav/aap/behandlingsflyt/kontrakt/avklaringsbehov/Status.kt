package no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov

public enum class Status {
    OPPRETTET,
    AVSLUTTET,
    TOTRINNS_VURDERT,
    SENDT_TILBAKE_FRA_BESLUTTER,
    KVALITETSSIKRET,
    SENDT_TILBAKE_FRA_KVALITETSSIKRER,
    AVBRUTT;

    public fun erÅpent(): Boolean {
        return this in setOf(
            OPPRETTET,
            SENDT_TILBAKE_FRA_BESLUTTER,
            SENDT_TILBAKE_FRA_KVALITETSSIKRER
        )
    }

    public fun erAvsluttet(): Boolean {
        return this in setOf(
            AVSLUTTET,
            TOTRINNS_VURDERT,
            KVALITETSSIKRET,
            AVBRUTT
        )
    }
}
