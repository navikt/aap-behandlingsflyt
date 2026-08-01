package no.nav.aap.behandlingsflyt.avklaringsbehov.flate

enum class Aksjon {
    SENDT_TIL_BESLUTTER,
    SENDT_TIL_KVALITETSSIKRER,
    RETURNERT_FRA_KVALITETSSIKRER,
    KVALITETSSIKRET,
    RETURNERT_FRA_BESLUTTER,
    FATTET_VEDTAK
}