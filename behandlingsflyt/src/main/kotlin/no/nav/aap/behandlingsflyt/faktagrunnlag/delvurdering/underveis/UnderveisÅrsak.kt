package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis

enum class UnderveisÅrsak(val konsekvens: Konsekvens) {
    IKKE_GRUNNLEGGENDE_RETT(Konsekvens.KONSEKVENS_FØLGER_AVSLAGSÅRSAK_TIL_RETTIGHETSTYPEN),
    MELDEPLIKT_FRIST_IKKE_PASSERT(Konsekvens.REDUKSJON),
    VARIGHETSKVOTE_BRUKT_OPP(Konsekvens.OPPHØR),
    @Deprecated("Vilkårssjekk er flyttet til Vilkårtype.AKTIVITETSPLIKT. Finnes ikke i prod, men i dev, så vi kan vurdere å slette.")
    BRUDD_PÅ_AKTIVITETSPLIKT_11_7_STANS(Konsekvens.STANS),
    @Deprecated("Vilkårssjekk er flyttet til Vilkårtype.AKTIVITETSPLIKT. Finnes ikke i prod, men i dev, så vi kan vurdere å slette.")
    BRUDD_PÅ_AKTIVITETSPLIKT_11_7_OPPHØR(Konsekvens.OPPHØR),
    @Deprecated("Vilkårssjekk er flyttet til Vilkårtype.OPPHOLDSKRAV.")
    BRUDD_PÅ_OPPHOLDSKRAV_11_3_STANS(Konsekvens.STANS),
    @Deprecated("Vilkårssjekk er flyttet til Vilkårtype.OPPHOLDSKRAV.")
    BRUDD_PÅ_OPPHOLDSKRAV_11_3_OPPHØR(Konsekvens.OPPHØR),

    @Deprecated("Sjekk om verdien finnes i prod-databasen før den evnt. slettes. Og dev?")
    SONER_STRAFF(Konsekvens.STANS),

    @Deprecated("""
        Ble brukt da ikke overholdt meldeplikt førte til stans, og ikke til reduksjon.
        Denne enum-verdien er lagret ned i databasen og deserialiseres derfra.
        Kan derfor ikke slettes så lenge det skal være mulig å se gamle behandlinger
        basert på verdiene i databasen.
        .""")
    IKKE_OVERHOLDT_MELDEPLIKT_SANKSJON(Konsekvens.REDUKSJON),

    @Deprecated("""
        Ble brukt da ikke overholdt meldeplikt førte til stans, og ikke til reduksjon.
        Denne enum-verdien er lagret ned i databasen og deserialiseres derfra.
        Kan derfor ikke slettes så lenge det skal være mulig å se gamle behandlinger
        basert på verdiene i databasen.
        .""")
    ARBEIDER_MER_ENN_GRENSEVERDI(Konsekvens.REDUKSJON),
}

enum class Konsekvens {
    KONSEKVENS_FØLGER_AVSLAGSÅRSAK_TIL_RETTIGHETSTYPEN,
    STANS,
    OPPHØR,
    REDUKSJON,
}