package no.nav.aap.behandlingsflyt.faktagrunnlag

enum class InformasjonskravNavn {
    MELDEKORT,
    BARN,
    YRKESSKADE,
    AKTIVITETSPLIKT,
    AKTIVITETSPLIKT_11_9,
    FORUTGÅENDE_MEDLEMSKAP,
    INNTEKT,
    LOVVALG,
    SAMORDNING_YTELSE,
    SAMORDNING_TJENESTEPENSJON,
    LEGEERKLÆRING,
    SØKNAD,
    INSTITUSJONSOPPHOLD,
    PERSONOPPLYSNING_FORUTGÅENDE,
    PERSONOPPLYSNING,
    UFØRE,
    AVBRYT_REVURDERING,
    DAGPENGER,
    VEDTAKSLENGDE,

    @Deprecated("Ikke i bruk, men finnes i databasen.")
    RETTIGHETSPERIODE,
    @Deprecated("Ikke i bruk, men finnes i databasen.")
    TRUKKET_SØKNAD,
    @Deprecated("Ikke i bruk, men finnes i databasen.")
    TRUKKET_KLAGE,
}