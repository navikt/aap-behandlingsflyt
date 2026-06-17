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
    UFØRE_SØKNAD,
    AVBRYT_REVURDERING,
    DAGPENGER,
    TILTAKSPENGER,
    VEDTAKSLENGDE,
    GRUNNBELØP,

    @Deprecated("Ikke i bruk, men finnes i databasen.")
    RETTIGHETSPERIODE,
    @Deprecated("Ikke i bruk, men finnes i databasen.")
    TRUKKET_SØKNAD,
    @Deprecated("Ikke i bruk, men finnes i databasen.")
    TRUKKET_KLAGE,
}